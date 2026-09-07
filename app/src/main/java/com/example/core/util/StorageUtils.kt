package com.example.core.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object StorageUtils {

  /**
   * Generates a clean, professional sequential-style filename:
   * e.g. "watermark_removed_01.jpg" or "watermark_removed_142345.jpg"
   */
  fun generateFormattedFilename(extension: String, isVideo: Boolean = false): String {
    val cleanExt = extension.trimStart('.').lowercase(Locale.ROOT)
    val timeSuffix = System.currentTimeMillis() % 1000000L
    val paddedNumber = String.format(Locale.US, "%02d", (timeSuffix % 100).toInt())
    return "watermark_removed_$paddedNumber.$cleanExt"
  }

  /**
   * Saves a Bitmap to the phone's gallery (Pictures/CleanMark) with the highest possible quality.
   * Format is PNG (lossless) or JPEG at 100% quality depending on transparency.
   * Works completely offline and uses scoped storage (no permissions needed).
   */
  fun saveImageToGallery(context: Context, bitmap: Bitmap): Uri? {
    val isPng = bitmap.hasAlpha()
    val extension = if (isPng) "png" else "jpg"
    val mimeType = if (isPng) "image/png" else "image/jpeg"
    val filename = generateFormattedFilename(extension, isVideo = false)

    val contentValues = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, filename)
      put(MediaStore.Images.Media.MIME_TYPE, mimeType)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CleanMark")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

    try {
      resolver.openOutputStream(uri)?.use { out ->
        if (isPng) {
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } else {
          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
      }
      return uri
    } catch (e: Exception) {
      e.printStackTrace()
      try {
        resolver.delete(uri, null, null)
      } catch (_: Exception) {}
      return null
    }
  }

  /**
   * Saves a video file to the phone's gallery (Movies/CleanMark).
   * Works completely offline and uses scoped storage.
   */
  fun saveVideoToGallery(context: Context, videoFile: File): Uri? {
    val filename = generateFormattedFilename("mp4", isVideo = true)
    val contentValues = ContentValues().apply {
      put(MediaStore.Video.Media.DISPLAY_NAME, filename)
      put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CleanMark")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null

    try {
      resolver.openOutputStream(uri)?.use { out ->
        FileInputStream(videoFile).use { inStream ->
          inStream.copyTo(out)
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
      }
      return uri
    } catch (e: Exception) {
      e.printStackTrace()
      try {
        resolver.delete(uri, null, null)
      } catch (_: Exception) {}
      return null
    }
  }

  /**
   * Shares a Bitmap via Android system share sheet using FileProvider.
   */
  fun shareImage(context: Context, bitmap: Bitmap) {
    try {
      val isPng = bitmap.hasAlpha()
      val ext = if (isPng) "png" else "jpg"
      val mimeType = if (isPng) "image/png" else "image/jpeg"
      val filename = generateFormattedFilename(ext, isVideo = false)

      val shareCacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
      val tempFile = File(shareCacheDir, filename)
      FileOutputStream(tempFile).use { out ->
        if (isPng) {
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } else {
          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
      }

      val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share Cleaned Image"))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Shares a processed video file via Android system share sheet using FileProvider.
   */
  fun shareVideo(context: Context, videoFile: File) {
    try {
      val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        videoFile
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share Cleaned Video"))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
