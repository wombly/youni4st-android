// Local compatibility adapter for the observed AAPT2 ZIP finalization crash.
// It never accepts compiler errors or truncated payloads. Every local ZIP entry
// must be complete and pass its original CRC32 before a directory is appended.
using System;
using System.IO;
using System.IO.Compression;
using System.Text;
using System.Linq;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;

class AaptBridge {
    [DllImport("kernel32.dll")] static extern uint SetErrorMode(uint mode);
    class Record {
        public uint Offset, Crc, Packed, Size;
        public ushort Version, Flags, Method, Time, Date;
        public byte[] Name;
    }
    static string Quote(string value) {
        var b = new StringBuilder("\""); int slashes = 0;
        foreach (char c in value) {
            if (c == '\\') { slashes++; continue; }
            if (c == '"') { b.Append('\\', slashes * 2 + 1); b.Append(c); slashes = 0; continue; }
            b.Append('\\', slashes); slashes = 0; b.Append(c);
        }
        b.Append('\\', slashes * 2); b.Append('"'); return b.ToString();
    }
    static uint Crc(byte[] bytes) {
        uint crc = 0xffffffff;
        foreach (byte b in bytes) {
            crc ^= b;
            for (int n = 0; n < 8; n++) crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xedb88320 : crc >> 1;
        }
        return crc ^ 0xffffffff;
    }
    static bool Repair(string file, bool symbolsRequired) {
        byte[] bytes = File.ReadAllBytes(file);
        var records = new List<Record>();
        long localEnd = 0;
        using (var input = new MemoryStream(bytes)) using (var r = new BinaryReader(input)) {
            while (input.Position < input.Length) {
                var e = new Record(); e.Offset = checked((uint)input.Position);
                if (input.Length - input.Position < 4) return false;
                uint signature = r.ReadUInt32();
                // A crash can leave a partial central directory after all payloads.
                if (signature == 0x02014b50 && records.Count > 0) break;
                if (signature != 0x04034b50 || input.Length - input.Position < 26) return false;
                e.Version = r.ReadUInt16(); e.Flags = r.ReadUInt16(); e.Method = r.ReadUInt16();
                e.Time = r.ReadUInt16(); e.Date = r.ReadUInt16(); e.Crc = r.ReadUInt32();
                e.Packed = r.ReadUInt32(); e.Size = r.ReadUInt32();
                ushort nameLength = r.ReadUInt16(), extraLength = r.ReadUInt16();
                if ((e.Flags & 9) != 0 || e.Packed > int.MaxValue || e.Size > 100000000) return false;
                if (input.Position + nameLength + extraLength + e.Packed > input.Length) return false;
                e.Name = r.ReadBytes(nameLength); r.ReadBytes(extraLength);
                byte[] packed = r.ReadBytes((int)e.Packed); byte[] content;
                if (e.Method == 0) content = packed;
                else if (e.Method == 8) {
                    using (var compressed = new MemoryStream(packed))
                    using (var deflate = new DeflateStream(compressed, CompressionMode.Decompress))
                    using (var output = new MemoryStream()) { deflate.CopyTo(output); content = output.ToArray(); }
                } else return false;
                if (content.Length != e.Size || Crc(content) != e.Crc) return false;
                string name = Encoding.UTF8.GetString(e.Name);
                if (name == "AndroidManifest.xml" && (content.Length < 8 || content[0] != 3 || content[1] != 0)) return false;
                if (name == "resources.arsc" && (content.Length < 12 || content[0] != 2 || content[1] != 0)) return false;
                records.Add(e);
                localEnd = input.Position;
            }
        }
        if (records.Count == 0 || records.Count >= 65535) return false;
        if (!records.Any(e => Encoding.UTF8.GetString(e.Name) == "AndroidManifest.xml")) return false;
        if (symbolsRequired && !records.Any(e => Encoding.UTF8.GetString(e.Name) == "resources.arsc")) return false;
        using (var stream = new FileStream(file, FileMode.Open, FileAccess.Write))
        using (var w = new BinaryWriter(stream)) {
            stream.SetLength(localEnd); stream.Position = localEnd;
            uint start = checked((uint)stream.Position);
            foreach (var e in records) {
                w.Write((uint)0x02014b50); w.Write((ushort)20); w.Write(e.Version); w.Write(e.Flags);
                w.Write(e.Method); w.Write(e.Time); w.Write(e.Date); w.Write(e.Crc); w.Write(e.Packed); w.Write(e.Size);
                w.Write((ushort)e.Name.Length); w.Write((ushort)0); w.Write((ushort)0); w.Write((ushort)0);
                w.Write((ushort)0); w.Write((uint)0); w.Write(e.Offset); w.Write(e.Name);
            }
            uint size = checked((uint)stream.Position - start);
            w.Write((uint)0x06054b50); w.Write((ushort)0); w.Write((ushort)0);
            w.Write((ushort)records.Count); w.Write((ushort)records.Count); w.Write(size); w.Write(start); w.Write((ushort)0);
        }
        using (var zip = ZipFile.OpenRead(file)) {
            if (zip.Entries.Count != records.Count) return false;
            foreach (var entry in zip.Entries) using (var s = entry.Open()) using (var dest = new MemoryStream()) { s.CopyTo(dest); }
        }
        string log = Environment.GetEnvironmentVariable("YOUNI_AAPT_LOG");
        if (!string.IsNullOrEmpty(log)) File.AppendAllText(log, "Validated ZIP repair: " + file + "; entries=" + records.Count + "; all original CRC32 values matched\n");
        return true;
    }
    static int Run(string[] args) {
        string diagnosticLog = Environment.GetEnvironmentVariable("YOUNI_AAPT_LOG");
        if (!string.IsNullOrEmpty(diagnosticLog)) File.AppendAllText(diagnosticLog, "Command: " + string.Join(" | ", args) + "\n");
        string exe = Environment.GetEnvironmentVariable("YOUNI_REAL_AAPT2");
        if (string.IsNullOrEmpty(exe)) throw new Exception("YOUNI_REAL_AAPT2 is required");
        string commandLine = string.Join(" ", args.Select(Quote).ToArray());
        bool viaInput = commandLine.Length > 28000;
        var info = new ProcessStartInfo(exe, viaInput ? "daemon" : commandLine);
        info.UseShellExecute = false; info.CreateNoWindow = true;
        info.RedirectStandardOutput = true; info.RedirectStandardError = true;
        info.RedirectStandardInput = viaInput;
        using (var p = Process.Start(info)) {
            var stdout = p.StandardOutput.ReadToEndAsync(); var stderr = p.StandardError.ReadToEndAsync();
            if (viaInput) {
                using (var input = new StreamWriter(p.StandardInput.BaseStream, new UTF8Encoding(false))) {
                    input.NewLine = "\n";
                    foreach (string arg in args) input.WriteLine(arg);
                    input.WriteLine(); input.WriteLine("quit"); input.WriteLine();
                }
            }
            p.WaitForExit(); string output = stdout.Result, error = stderr.Result;
            if (viaInput) {
                bool failed = error.Split('\n').Any(line => line.Trim() == "Error");
                output = string.Join("\n", output.Split('\n').Where(line => line.Trim() != "Ready" && line.Trim() != "Exiting daemon"));
                error = string.Join("\n", error.Split('\n').Where(line => line.Trim() != "Done" && line.Trim() != "Error"));
                if (failed && p.ExitCode == 0) { Console.Error.Write(error); return 1; }
            }
            if (!string.IsNullOrEmpty(diagnosticLog)) File.AppendAllText(diagnosticLog, "Exit: " + p.ExitCode + "; stderr: " + error + "\n");
            if (output.Length > 0) Console.Out.Write(output);
            if (error.Length > 0) Console.Error.Write(error);
            if (p.ExitCode == 0) return 0;
            // Only the specifically observed native access violation is recoverable.
            if (p.ExitCode != unchecked((int)0xc0000005) || args.Length == 0 || (args[0] != "link" && args[0] != "l") || error.Contains("error:")) return p.ExitCode;
            int outputIndex = Array.IndexOf(args, "-o");
            if (outputIndex < 0 || outputIndex + 1 >= args.Length) return p.ExitCode;
            int symbols = Array.IndexOf(args, "--output-text-symbols");
            if (symbols >= 0 && (symbols + 1 >= args.Length || !File.Exists(args[symbols + 1]) || new FileInfo(args[symbols + 1]).Length == 0)) return p.ExitCode;
            return Repair(args[outputIndex + 1], symbols >= 0) ? 0 : p.ExitCode;
        }
    }
    static int Main(string[] args) {
        Console.InputEncoding = new UTF8Encoding(false);
        SetErrorMode(0x0001 | 0x0002 | 0x8000);
        try {
            if (args.Length >= 1 && (args[0] == "daemon" || args[0] == "m")) {
                Console.WriteLine("Ready");
                Console.Out.Flush();
                while (true) {
                    var lines = new List<string>(); string line;
                    while ((line = Console.ReadLine()) != null && line.Length > 0) lines.Add(line);
                    if (line == null || (lines.Count > 0 && lines[0] == "quit")) break;
                    if (lines.Count == 0) continue;
                    try { if (Run(lines.ToArray()) != 0) Console.Error.WriteLine("Error"); }
                    catch (Exception e) { Console.Error.WriteLine(e.Message); Console.Error.WriteLine("Error"); }
                    Console.Error.WriteLine("Done"); Console.Error.Flush();
                }
                Console.WriteLine("Exiting daemon"); return 0;
            }
            return Run(args);
        } catch (Exception e) { Console.Error.WriteLine(e); return 1; }
    }
}
