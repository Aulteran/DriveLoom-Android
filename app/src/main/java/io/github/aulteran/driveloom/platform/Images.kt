package io.github.aulteran.driveloom.platform

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Decoding and re-encoding of vehicle photos. Nothing here touches the network. */
object Images {
    /** The server's upload limit. */
    const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024
    private const val MAX_UPLOAD_EDGE = 2048
    private const val MAX_DISPLAY_EDGE = 1600

    /**
     * Reads the picked image, scales it to at most 2048 px on its long edge and
     * re-encodes it as JPEG. Re-encoding drops all metadata, including the
     * place the photo was taken. Returns null when the image cannot be read.
     */
    fun prepareUpload(resolver: ContentResolver, uri: Uri): ByteArray? {
        val decoded: Bitmap? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(resolver, uri) else decodeLegacy(resolver, uri)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
        val bitmap = decoded ?: return null
        var quality = 88
        while (true) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (out.size() <= MAX_UPLOAD_BYTES || quality <= 40) {
                bitmap.recycle()
                return out.toByteArray().takeIf { it.size <= MAX_UPLOAD_BYTES }
            }
            quality -= 12
        }
    }

    /** Decodes image bytes from the server for display, sampled down to a sensible size. */
    fun decodeForDisplay(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_DISPLAY_EDGE) }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    internal fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(resolver: ContentResolver, uri: Uri): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_UPLOAD_EDGE) {
                val scale = MAX_UPLOAD_EDGE.toFloat() / longest
                decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
            }
        }
    }

    private fun decodeLegacy(resolver: ContentResolver, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_UPLOAD_EDGE) }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val degrees = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val longest = maxOf(decoded.width, decoded.height)
        val scale = if (longest > MAX_UPLOAD_EDGE) MAX_UPLOAD_EDGE.toFloat() / longest else 1f
        if (degrees == 0f && scale == 1f) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees)
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }
}
