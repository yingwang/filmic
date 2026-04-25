package com.yingwang.filmic.io

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.yingwang.filmic.lut.Style

/**
 * Centralised gallery write path. Three callers -- Preview save/share, Batch
 * export, and Camera capture -- all funnel through here so the directory,
 * filename, IS_PENDING flow, and quality stay consistent.
 *
 * Camera and edits both land under `DCIM/Filmic/` because Pixel's Photos app
 * indexes anything under DCIM into the main feed but skips `Pictures` (which
 * only shows up under Library, Folders). The user expected to see results in
 * 相册 (Photos), not 文件 (Files), so DCIM is the right home.
 */
object MediaSaver {

    private const val RELATIVE_PATH = "DCIM/Filmic"

    /**
     * Inserts a new image at `DCIM/Filmic/Filmic_<style>_<timestamp>.jpg` and
     * returns its content URI. On API 29+ the row starts as IS_PENDING so
     * MediaStore won't index a half-written file; clear with [finishPending].
     */
    fun insert(context: Context, style: Style): Uri? {
        val name = "Filmic_${style.id}_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun finishPending(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    /**
     * Decodes an image URI into a bitmap with EXIF orientation already baked
     * into the pixels. Caller never needs to handle ORIENTATION_* again -- the
     * returned bitmap is upright in display order.
     */
    fun decodeOriented(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        return applyExifOrientation(raw, orientation)
    }

    fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    /**
     * Encode [bitmap] as JPEG and write it to the gallery via [insert]/[finishPending].
     * Returns true if everything succeeded.
     *
     * The bitmap is expected to already be in display orientation (no EXIF
     * orientation tag will be written; we set ORIENTATION_NORMAL).
     */
    fun saveBitmap(context: Context, bitmap: Bitmap, style: Style, jpegQuality: Int): Boolean {
        val uri = insert(context, style) ?: return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            } ?: return false
            writeNormalOrientation(context, uri)
            finishPending(context, uri)
            true
        } catch (t: Throwable) {
            // Best-effort cleanup so the gallery doesn't keep an empty pending row.
            try { context.contentResolver.delete(uri, null, null) } catch (_: Throwable) {}
            false
        }
    }

    /** Returns the content URI of the most recent image saved under DCIM/Filmic, or null. */
    fun latestPhoto(context: Context): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection: String?
        val selectionArgs: Array<String>?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("$RELATIVE_PATH%")
        } else {
            selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("Filmic_%")
        }
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
        }
        return null
    }

    private fun writeNormalOrientation(context: Context, uri: Uri) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exif.setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString(),
                )
                exif.saveAttributes()
            }
        } catch (_: Throwable) {
            // EXIF is best-effort -- pixels are already in display orientation.
        }
    }
}
