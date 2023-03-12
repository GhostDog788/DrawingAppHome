package il.ghostdog.drawingapp

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SerializationHelper {
    companion object {
        fun compressBitmap(bitmap: Bitmap): ByteArray {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            return outputStream.toByteArray()
        }

        fun gzip(content: String): String {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).bufferedWriter(StandardCharsets.UTF_8).use { it.write(content) }
            val b = bos.toByteArray()
            return Base64.encodeToString(b, Base64.DEFAULT)
        }

        fun unGzip(content: String): String =
            GZIPInputStream(Base64.decode(content, Base64.DEFAULT).inputStream()).bufferedReader(
                StandardCharsets.UTF_8
            ).use { it.readText() }
    }
}