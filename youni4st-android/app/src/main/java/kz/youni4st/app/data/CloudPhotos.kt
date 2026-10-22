package kz.youni4st.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

/** Small account photographs fit in Firestore; no paid Storage bucket is required.
 * Local originals remain intact. Only the resized JPEG copy is uploaded.
 */
class CloudPhotos(private val context:Context) {
    private val cache=java.util.concurrent.ConcurrentHashMap<String,String>()
    fun upload(section:JSONObject):JSONObject=JSONObject(section.toString()).apply {
        val uri=optString("photo")
        put("photo",if(uri.isBlank()) "" else cache[uri] ?: encode(uri).also {if(cache.size>=4)cache.clear();cache[uri]=it})
    }
    fun download(section:JSONObject,uid:String):JSONObject=JSONObject(section.toString()).apply {
        val data=optString("photo")
        put("photo",if(data.isBlank()) "" else restore(data,uid))
    }
    private fun encode(value:String):String {
        val uri=Uri.parse(value)
        if(uri.scheme !in listOf("file","content"))throw RegistryException("CLOUD_PHOTO_UNREADABLE")
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds);Unit} ?: throw RegistryException("CLOUD_PHOTO_UNREADABLE")
        if(bounds.outWidth<=0 || bounds.outHeight<=0)throw RegistryException("CLOUD_PHOTO_UNREADABLE")
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1024)sample*=2
        val bitmap=context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})} ?: throw RegistryException("CLOUD_PHOTO_UNREADABLE")
        val orientation=runCatching {context.contentResolver.openInputStream(uri)?.use {android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1)} ?: 1}.getOrDefault(1)
        val matrix=android.graphics.Matrix().apply {
            when(orientation){2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f);5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f);7->{setRotate(-90f);postScale(-1f,1f)};8->setRotate(-90f)}
        }
        val oriented=if(matrix.isIdentity)bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
        val scale=minOf(1f,512f/maxOf(oriented.width,oriented.height))
        val resized=Bitmap.createScaledBitmap(oriented,(oriented.width*scale).toInt().coerceAtLeast(1),(oriented.height*scale).toInt().coerceAtLeast(1),true)
        return try {
            val bytes=ByteArrayOutputStream().use {out->resized.compress(Bitmap.CompressFormat.JPEG,78,out);out.toByteArray()}
            if(bytes.size>130_000)throw RegistryException("CLOUD_PHOTO_TOO_LARGE")
            "jpeg:"+Base64.encodeToString(bytes,Base64.NO_WRAP)
        } finally {if(resized!==oriented)resized.recycle();if(oriented!==bitmap)oriented.recycle();bitmap.recycle()}
    }
    private fun restore(value:String,uid:String):String {
        if(!value.startsWith("jpeg:") || value.length>180_000)throw RegistryException("CLOUD_PHOTO_UNREADABLE")
        val bytes=Base64.decode(value.removePrefix("jpeg:"),Base64.NO_WRAP)
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        if(bounds.outWidth !in 1..512 || bounds.outHeight !in 1..512)throw RegistryException("CLOUD_PHOTO_UNREADABLE")
        val hash=MessageDigest.getInstance("SHA-256").digest((uid+value).toByteArray()).joinToString("") {"%02x".format(it)}
        val directory=File(context.filesDir,"cloud-photos").apply {mkdirs()}
        val target=File(directory,"$hash.jpg")
        if(!target.exists()) {val temp=File.createTempFile("photo-",".tmp",directory);try {temp.writeBytes(bytes);if(!temp.renameTo(target))throw RegistryException("CLOUD_PHOTO_UNREADABLE")} finally {temp.delete()}}
        return Uri.fromFile(target).toString()
    }
}
