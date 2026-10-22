# Account synchronization

The app uses Firebase Authentication ID tokens over HTTPS with the Cloud Firestore REST API. No service-account key is included in Android. Existing username reservation rules are preserved.

`user_data/{firebaseUid}` contains schema version 1 and independent sections: profile, resume, favorites, drafts, chat, settings and receipts. Profile and resume photos are bounded resized copies encoded within their respective sections. Firestore rules require authenticated ownership for every read and write, disallow collection listing, and validate section types and photo bounds.

Each signed-in account has a separate local preferences namespace. The old profile cache and local device data migrate to the original account once. Later accounts do not inherit the preceding account's cache. Local writes and pending revision markers are saved in the same SharedPreferences edit. A server acknowledgement clears only the exact revision it uploaded.

At sign-in, foreground return, network restoration or manual refresh, the app reads the account document and restores sections that have no pending local edits. Automatic local saves patch touched fields without downloading photos again. Photos are decoded and resized off the UI thread; downloaded photos become private local files with content-derived names.

Favorites and drafts are maps of independently patched items keyed by `i` + SHA-256(entry ID), using tombstones for deletions. Two devices can modify different items without replacing the whole collection. The server is authoritative except for explicit pending local edits. Concurrent edits to the same profile, resume or chat section use the last accepted section write; this is not a document collaboration editor.

No continuous polling is used. Connectivity retries continue while the application process is alive; persistent pending edits resume on the next app launch after the process is closed. Status in Profile distinguishes locally saved changes, pending changes and successfully synced data. Guest data stays local.

## Tests

From this folder, with Node.js 22+ and Java 21+:

```sh
npm install
npm test
```

The runner uses the demo project and local emulator. Tests cover atomic nickname registration plus account isolation, exact REST field masks, simultaneous favorites, section preservation and invalid photo paths. These tests never call production Firebase or send SMS.

Publish the complete `firestore.rules` to your own default Firestore database separately. The test run does not deploy anything to the production project. For account deletion later, implement trusted cleanup of the account document and username records together with Firebase Auth deletion.
