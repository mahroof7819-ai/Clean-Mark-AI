package com.example.core.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.core.inpainting.InpaintingAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Offline Video Processor for CleanMark AI.
 *
 * Supports wide array of video formats:
 * MP4, MKV, MOV, AVI, WEBM, 3GP, M4V, MPEG/MPG, TS,
 * including videos from AI video generators (Runway, Sora, Kling, Luma, Pika, Stable Video),
 * HD, Full HD, 2K, 4K and 3D/4D stereoscopic/multi-view video files decoded by the Android device.
 *
 * Catches format decoding limitations gracefully with friendly descriptive errors.
 */
object VideoProcessor {

  data class VideoInfo(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val rotation: Int,
    val mimeType: String?,
    val formatName: String,
    val previewFrame: Bitmap?
  )

  /**
   * Identifies user-friendly format name from file extension or mime type.
   */
  fun detectFormatName(context: Context, uri: Uri, mimeType: String?): String {
    val uriString = uri.toString().lowercase()
    return when {
      uriString.contains(".mkv") || mimeType?.contains("matroska") == true -> "MKV"
      uriString.contains(".webm") || mimeType?.contains("webm") == true -> "WEBM"
      uriString.contains(".mov") || mimeType?.contains("quicktime") == true -> "MOV"
      uriString.contains(".avi") || mimeType?.contains("avi") == true || mimeType?.contains("msvideo") == true -> "AVI"
      uriString.contains(".3gp") || mimeType?.contains("3gpp") == true -> "3GP"
      uriString.contains(".m4v") -> "M4V"
      uriString.contains(".ts") || mimeType?.contains("mp2t") == true -> "TS"
      uriString.contains(".mpg") || uriString.contains(".mpeg") || mimeType?.contains("mpeg") == true -> "MPEG"
      uriString.contains(".mp4") || mimeType?.contains("mp4") == true -> "MP4"
      else -> {
        val extension = uriString.substringAfterLast('.', "")
        if (extension.isNotBlank() && extension.length <= 4) extension.uppercase() else "Video"
      }
    }
  }

  /**
   * Retrieves video metadata and extracts the first frame for watermark area selection.
   * Throws an UnsupportedOperationException if the video cannot be parsed or decoded on this device.
   */
  suspend fun extractVideoInfo(context: Context, videoUri: Uri): VideoInfo = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
      try {
        retriever.setDataSource(context, videoUri)
      } catch (e: Exception) {
        throw UnsupportedOperationException("This video format is not supported on this device.")
      }

      val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
      val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

      val durationMs = durationStr?.toLongOrNull() ?: 5000L
      val rotation = rotationStr?.toIntOrNull() ?: 0

      // Extract high quality frame for preview and watermark marking
      val rawFrame = try {
        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          ?: retriever.getFrameAtTime(100_000, MediaMetadataRetriever.OPTION_CLOSEST)
          ?: retriever.frameAtTime
      } catch (e: Exception) {
        null
      }

      if (rawFrame == null && (widthStr == null || heightStr == null)) {
        throw UnsupportedOperationException("This video format is not supported on this device.")
      }

      val width = rawFrame?.width ?: (widthStr?.toIntOrNull() ?: 1280)
      val height = rawFrame?.height ?: (heightStr?.toIntOrNull() ?: 720)
      val formatName = detectFormatName(context, videoUri, mimeType)

      VideoInfo(
        width = width,
        height = height,
        durationMs = durationMs,
        rotation = rotation,
        mimeType = mimeType,
        formatName = formatName,
        previewFrame = rawFrame
      )
    } catch (e: UnsupportedOperationException) {
      throw e
    } catch (e: Exception) {
      throw UnsupportedOperationException("This video format is not supported on this device.")
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }
  }

  /**
   * Processes the video offline frame-by-frame, removing watermark areas, and outputs a video file.
   * Maintains original resolution up to 4K (3840x2160) where device encoders support it.
   */
  suspend fun processVideo(
    context: Context,
    videoUri: Uri,
    boxes: List<InpaintingAlgorithm.WatermarkBox>,
    strokes: List<InpaintingAlgorithm.WatermarkStroke> = emptyList(),
    onProgress: (currentFrame: Int, totalFrames: Int, percentage: Float) -> Unit,
    isCancelled: () -> Boolean
  ): File = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, videoUri)
    } catch (e: Exception) {
      throw UnsupportedOperationException("This video format is not supported on this device.")
    }

    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val durationMs = durationStr?.toLongOrNull() ?: 3000L

    val firstFrame = try {
      retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: retriever.getFrameAtTime(100_000, MediaMetadataRetriever.OPTION_CLOSEST)
        ?: retriever.frameAtTime
    } catch (e: Exception) {
      null
    } ?: throw UnsupportedOperationException("This video format is not supported on this device.")

    // Keep highest possible original resolution while ensuring even dimensions required by H.264
    var targetWidth = (firstFrame.width / 2) * 2
    var targetHeight = (firstFrame.height / 2) * 2

    // Check device encoder capability up to 4K (3840x2160)
    val mimeType = "video/avc"
    val maxEncoderDim = getMaxSupportedDimension(mimeType)
    if (targetWidth > maxEncoderDim || targetHeight > maxEncoderDim) {
      val scale = min(maxEncoderDim.toFloat() / targetWidth, maxEncoderDim.toFloat() / targetHeight)
      targetWidth = ((targetWidth * scale).toInt() / 2) * 2
      targetHeight = ((targetHeight * scale).toInt() / 2) * 2
    }

    val fps = 24
    val frameIntervalUs = 1_000_000L / fps
    val totalFrames = max(1, ((durationMs * 1000L) / frameIntervalUs).toInt())

    // Precompute binary mask and bounding box for high-speed inpainting
    val maskBitmap = InpaintingAlgorithm.createMaskBitmap(targetWidth, targetHeight, boxes, strokes)
    val maskPixels = IntArray(targetWidth * targetHeight)
    maskBitmap.getPixels(maskPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

    val isHole = BooleanArray(targetWidth * targetHeight)
    var minX = targetWidth
    var maxX = 0
    var minY = targetHeight
    var maxY = 0
    var holeCount = 0

    for (y in 0 until targetHeight) {
      val row = y * targetWidth
      for (x in 0 until targetWidth) {
        val idx = row + x
        val alpha = (maskPixels[idx] ushr 24) and 0xFF
        val red = (maskPixels[idx] ushr 16) and 0xFF
        if (alpha > 40 && red > 40) {
          isHole[idx] = true
          holeCount++
          if (x < minX) minX = x
          if (x > maxX) maxX = x
          if (y < minY) minY = y
          if (y > maxY) maxY = y
        }
      }
    }

    val margin = 16
    val startX = max(0, minX - margin)
    val endX = min(targetWidth - 1, maxX + margin)
    val startY = max(0, minY - margin)
    val endY = min(targetHeight - 1, maxY + margin)

    // Preallocate buffers for the frame inpainting loop
    val argbPixels = IntArray(targetWidth * targetHeight)
    val rChan = FloatArray(targetWidth * targetHeight)
    val gChan = FloatArray(targetWidth * targetHeight)
    val bChan = FloatArray(targetWidth * targetHeight)
    val yuvBuffer = ByteArray(targetWidth * targetHeight * 3 / 2)

    val outputFile = File(context.cacheDir, "cleanmark_processed_${System.currentTimeMillis()}.mp4")
    if (outputFile.exists()) outputFile.delete()

    val colorFormat = selectColorFormat(mimeType)
    val bitrate = (targetWidth.toLong() * targetHeight * 4L).toInt().coerceIn(2_000_000, 25_000_000)

    val videoFormat = MediaFormat.createVideoFormat(mimeType, targetWidth, targetHeight).apply {
      setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
      setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
      setInteger(MediaFormat.KEY_FRAME_RATE, fps)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val encoder = try {
      MediaCodec.createEncoderByType(mimeType)
    } catch (e: Exception) {
      throw UnsupportedOperationException("This video format is not supported on this device.")
    }

    try {
      encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()
    } catch (e: Exception) {
      try { encoder.release() } catch (_: Exception) {}
      throw UnsupportedOperationException("This video format is not supported on this device.")
    }

    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var videoTrackIndex = -1
    var muxerStarted = false

    // Check for audio track in original video
    val audioExtractor = MediaExtractor()
    var audioTrackIndex = -1
    var audioSourceTrack = -1
    try {
      audioExtractor.setDataSource(context, videoUri, null)
      for (i in 0 until audioExtractor.trackCount) {
        val format = audioExtractor.getTrackFormat(i)
        val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
        if (trackMime.startsWith("audio/")) {
          audioSourceTrack = i
          break
        }
      }
    } catch (_: Exception) {}

    val bufferInfo = MediaCodec.BufferInfo()

    try {
      for (frameIndex in 0 until totalFrames) {
        if (isCancelled()) {
          throw InterruptedException("Video processing cancelled by user")
        }

        val timeUs = frameIndex * frameIntervalUs
        val currentFrameBitmap = try {
          retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (_: Exception) {
          null
        } ?: firstFrame

        val scaledBitmap = if (currentFrameBitmap.width != targetWidth || currentFrameBitmap.height != targetHeight) {
          Bitmap.createScaledBitmap(currentFrameBitmap, targetWidth, targetHeight, true)
        } else {
          currentFrameBitmap
        }

        scaledBitmap.getPixels(argbPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        if (scaledBitmap != currentFrameBitmap && scaledBitmap != firstFrame) {
          scaledBitmap.recycle()
        }
        if (currentFrameBitmap != firstFrame) {
          currentFrameBitmap.recycle()
        }

        // Apply offline inpainting if watermark areas exist
        if (holeCount > 0) {
          InpaintingAlgorithm.inpaintFramePixels(
            pixels = argbPixels,
            width = targetWidth,
            height = targetHeight,
            isHole = isHole,
            startX = startX,
            endX = endX,
            startY = startY,
            endY = endY,
            rChan = rChan,
            gChan = gChan,
            bChan = bChan
          )
        }

        // Convert to YUV420
        encodeYUV420(yuvBuffer, argbPixels, targetWidth, targetHeight, colorFormat)

        // Feed into MediaCodec
        feedFrameToEncoder(
          encoder = encoder,
          yuvBuffer = yuvBuffer,
          presentationTimeUs = timeUs,
          isEos = (frameIndex == totalFrames - 1)
        )

        // Drain encoder outputs
        videoTrackIndex = drainEncoder(
          encoder = encoder,
          bufferInfo = bufferInfo,
          muxer = muxer,
          muxerStarted = muxerStarted,
          videoTrackIndex = videoTrackIndex,
          audioExtractor = audioExtractor,
          audioSourceTrack = audioSourceTrack,
          onMuxerStarted = { muxerStarted = true; audioTrackIndex = it }
        )

        val progress = (frameIndex + 1).toFloat() / totalFrames
        onProgress(frameIndex + 1, totalFrames, progress)
      }

      // Final drain with end of stream
      drainEncoderEos(encoder, bufferInfo, muxer, videoTrackIndex)

      // Copy audio samples if present
      if (muxerStarted && audioTrackIndex != -1 && audioSourceTrack != -1) {
        copyAudioTrack(audioExtractor, audioSourceTrack, muxer, audioTrackIndex)
      }

    } finally {
      try {
        encoder.stop()
        encoder.release()
      } catch (_: Exception) {}

      try {
        if (muxerStarted) {
          muxer.stop()
          muxer.release()
        }
      } catch (_: Exception) {}

      try {
        audioExtractor.release()
      } catch (_: Exception) {}

      try {
        retriever.release()
      } catch (_: Exception) {}

      firstFrame.recycle()
      maskBitmap.recycle()
    }

    outputFile
  }

  private fun getMaxSupportedDimension(mimeType: String): Int {
    try {
      val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
      for (info in codecList.codecInfos) {
        if (!info.isEncoder) continue
        val types = info.supportedTypes
        for (type in types) {
          if (type.equals(mimeType, ignoreCase = true)) {
            val videoCaps = info.getCapabilitiesForType(type).videoCapabilities
            if (videoCaps != null) {
              val maxWidth = videoCaps.supportedWidths.upper
              return if (maxWidth >= 3840) 3840 else if (maxWidth >= 2560) 2560 else 1920
            }
          }
        }
      }
    } catch (_: Exception) {}
    return 1920
  }

  private fun selectColorFormat(mimeType: String): Int {
    val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    for (info in codecList.codecInfos) {
      if (!info.isEncoder) continue
      val types = info.supportedTypes
      for (type in types) {
        if (type.equals(mimeType, ignoreCase = true)) {
          val capabilities = info.getCapabilitiesForType(type)
          for (format in capabilities.colorFormats) {
            if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar ||
                format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            ) {
              return format
            }
          }
        }
      }
    }
    return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
  }

  private fun encodeYUV420(
    yuv: ByteArray,
    argb: IntArray,
    width: Int,
    height: Int,
    colorFormat: Int
  ) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize
    val isPlanar = (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)

    var uIndex = frameSize
    var vIndex = frameSize + frameSize / 4

    for (j in 0 until height) {
      val rowStart = j * width
      for (i in 0 until width) {
        val c = argb[rowStart + i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF

        val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
        yuv[yIndex++] = y.coerceIn(0, 255).toByte()

        if (j % 2 == 0 && i % 2 == 0) {
          val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
          val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
          if (isPlanar) {
            yuv[uIndex++] = u.coerceIn(0, 255).toByte()
            yuv[vIndex++] = v.coerceIn(0, 255).toByte()
          } else {
            // SemiPlanar (NV12: U then V)
            yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
            yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
          }
        }
      }
    }
  }

  private fun feedFrameToEncoder(
    encoder: MediaCodec,
    yuvBuffer: ByteArray,
    presentationTimeUs: Long,
    isEos: Boolean
  ) {
    val inputBufferIndex = encoder.dequeueInputBuffer(10_000L)
    if (inputBufferIndex >= 0) {
      val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
      inputBuffer?.clear()
      inputBuffer?.put(yuvBuffer)
      val flags = if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
      encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, presentationTimeUs, flags)
    }
  }

  private fun drainEncoder(
    encoder: MediaCodec,
    bufferInfo: MediaCodec.BufferInfo,
    muxer: MediaMuxer,
    muxerStarted: Boolean,
    videoTrackIndex: Int,
    audioExtractor: MediaExtractor,
    audioSourceTrack: Int,
    onMuxerStarted: (audioTrackIndex: Int) -> Unit
  ): Int {
    var track = videoTrackIndex
    var started = muxerStarted

    while (true) {
      val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 2500L)
      if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
        break
      } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        if (!started) {
          val newFormat = encoder.outputFormat
          track = muxer.addTrack(newFormat)
          var audioTrack = -1
          if (audioSourceTrack != -1) {
            try {
              val aFormat = audioExtractor.getTrackFormat(audioSourceTrack)
              audioTrack = muxer.addTrack(aFormat)
            } catch (_: Exception) {}
          }
          muxer.start()
          started = true
          onMuxerStarted(audioTrack)
        }
      } else if (encoderStatus >= 0) {
        val encodedData = encoder.getOutputBuffer(encoderStatus)
        if (encodedData != null && bufferInfo.size > 0 && started) {
          encodedData.position(bufferInfo.offset)
          encodedData.limit(bufferInfo.offset + bufferInfo.size)
          muxer.writeSampleData(track, encodedData, bufferInfo)
        }
        encoder.releaseOutputBuffer(encoderStatus, false)
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break
        }
      }
    }
    return track
  }

  private fun drainEncoderEos(
    encoder: MediaCodec,
    bufferInfo: MediaCodec.BufferInfo,
    muxer: MediaMuxer,
    videoTrackIndex: Int
  ) {
    while (true) {
      val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
      if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
        break
      } else if (encoderStatus >= 0) {
        val encodedData = encoder.getOutputBuffer(encoderStatus)
        if (encodedData != null && bufferInfo.size > 0) {
          encodedData.position(bufferInfo.offset)
          encodedData.limit(bufferInfo.offset + bufferInfo.size)
          muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
        }
        encoder.releaseOutputBuffer(encoderStatus, false)
        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
          break
        }
      }
    }
  }

  private fun copyAudioTrack(
    audioExtractor: MediaExtractor,
    audioSourceTrack: Int,
    muxer: MediaMuxer,
    audioTrackIndex: Int
  ) {
    try {
      audioExtractor.selectTrack(audioSourceTrack)
      val buffer = ByteBuffer.allocateDirect(256 * 1024)
      val audioBufferInfo = MediaCodec.BufferInfo()

      while (true) {
        val sampleSize = audioExtractor.readSampleData(buffer, 0)
        if (sampleSize < 0) break

        audioBufferInfo.offset = 0
        audioBufferInfo.size = sampleSize
        audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
        audioBufferInfo.flags = audioExtractor.sampleFlags

        muxer.writeSampleData(audioTrackIndex, buffer, audioBufferInfo)
        audioExtractor.advance()
      }
    } catch (_: Exception) {}
  }
}
