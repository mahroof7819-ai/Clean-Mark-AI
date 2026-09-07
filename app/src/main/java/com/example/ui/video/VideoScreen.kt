package com.example.ui.video

import android.graphics.Bitmap
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.inpainting.InpaintingAlgorithm
import com.example.core.util.StorageUtils
import com.example.core.video.VideoProcessor
import com.example.ui.components.SelectionMode
import com.example.ui.components.WatermarkCanvasOverlay
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AmbientBackgroundBrush
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun VideoScreen(
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
  var videoInfo by remember { mutableStateOf<VideoProcessor.VideoInfo?>(null) }
  var processedVideoFile by remember { mutableStateOf<File?>(null) }

  val watermarkBoxes = remember { mutableStateListOf<InpaintingAlgorithm.WatermarkBox>() }
  val watermarkStrokes = remember { mutableStateListOf<InpaintingAlgorithm.WatermarkStroke>() }

  var selectionMode by remember { mutableStateOf(SelectionMode.RECTANGLE) }
  var isProcessing by remember { mutableStateOf(false) }
  var isCancelled by remember { mutableStateOf(false) }

  var currentFrame by remember { mutableIntStateOf(0) }
  var totalFrames by remember { mutableIntStateOf(0) }
  var progressPercentage by remember { mutableFloatStateOf(0f) }
  var saveSuccessMsg by remember { mutableStateOf<String?>(null) }

  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      selectedVideoUri = it
      processedVideoFile = null
      watermarkBoxes.clear()
      watermarkStrokes.clear()
      saveSuccessMsg = null

      coroutineScope.launch {
        try {
          videoInfo = VideoProcessor.extractVideoInfo(context, it)
        } catch (e: UnsupportedOperationException) {
          selectedVideoUri = null
          videoInfo = null
          Toast.makeText(context, e.message ?: "This video format is not supported on this device.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
          selectedVideoUri = null
          videoInfo = null
          Toast.makeText(context, "This video format is not supported on this device.", Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AmbientBackgroundBrush)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      // Top Navigation Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(DarkSurface)
              .testTag("video_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Video Remover",
              color = TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Offline Frame-by-Frame Inpainting",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        if (selectedVideoUri != null) {
          IconButton(
            onClick = {
              videoPickerLauncher.launch("video/*")
            },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .testTag("pick_another_video_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = "Pick Another",
              tint = AccentPurple
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Video Selection Empty State
      val currentUri = selectedVideoUri
      val info = videoInfo
      if (currentUri == null || info == null || info.previewFrame == null) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, DarkBorder, RoundedCornerShape(24.dp))
            .clickable {
              videoPickerLauncher.launch("video/*")
            }
            .testTag("select_video_button"),
          color = DarkSurfaceCard
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.15f))
                .border(1.5.dp, AccentPurple.copy(alpha = 0.4f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = "Select Video",
                tint = AccentPurple,
                modifier = Modifier.size(40.dp)
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
              text = "Select Video from Gallery",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "MP4, MKV, MOV, AVI, WEBM, 3GP, M4V, TS\nHD, 2K, 4K & AI videos • 100% offline frame-by-frame",
              color = TextMuted,
              fontSize = 13.sp,
              textAlign = TextAlign.Center
            )
          }
        }
      } else {
        // Video Preview / Player Area
        val totalMarks = watermarkBoxes.size + watermarkStrokes.size
        val durationSec = (info.durationMs / 1000f)

        // Metadata chip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${info.formatName} • ${info.width}x${info.height} • ${String.format("%.1f", durationSec)}s",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )

          Text(
            text = "100% Offline",
            color = AccentPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, DarkBorder, RoundedCornerShape(20.dp))
            .background(DarkSurfaceCard),
          contentAlignment = Alignment.Center
        ) {
          val aspectRatio = (info.width.toFloat() / info.height.toFloat()).coerceIn(0.5f, 2.2f)
          val containerWidth = maxWidth
          val targetHeight = (containerWidth.value / aspectRatio).dp.coerceIn(240.dp, 460.dp)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(targetHeight)
          ) {
            val processed = processedVideoFile
            if (processed != null && processed.exists()) {
              // Video Player for the Cleaned Video
              AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                  VideoView(ctx).apply {
                    setVideoURI(Uri.fromFile(processed))
                    val mediaController = MediaController(ctx)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    setOnPreparedListener { mp ->
                      mp.isLooping = true
                      start()
                    }
                  }
                }
              )
            } else {
              // First Frame Preview for Watermark Marking
              Image(
                bitmap = info.previewFrame.asImageBitmap(),
                contentDescription = "Video watermark preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
              )

              // Interactive Watermark Overlay
              WatermarkCanvasOverlay(
                modifier = Modifier.fillMaxSize(),
                mode = selectionMode,
                boxes = watermarkBoxes,
                strokes = watermarkStrokes,
                onAddBox = { watermarkBoxes.add(it) },
                onAddStroke = { watermarkStrokes.add(it) },
                enabled = !isProcessing
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Watermark Controls when in marking mode
        if (processedVideoFile == null && !isProcessing) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = selectionMode == SelectionMode.RECTANGLE,
                onClick = { selectionMode = SelectionMode.RECTANGLE },
                label = { Text("Box Selection") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.CropFree,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AccentPurple,
                  selectedLabelColor = TextPrimary,
                  selectedLeadingIconColor = TextPrimary,
                  containerColor = DarkSurface,
                  labelColor = TextPrimary,
                  iconColor = TextSecondary
                )
              )

              FilterChip(
                selected = selectionMode == SelectionMode.BRUSH,
                onClick = { selectionMode = SelectionMode.BRUSH },
                label = { Text("Brush") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.Brush,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AccentPurple,
                  selectedLabelColor = TextPrimary,
                  selectedLeadingIconColor = TextPrimary,
                  containerColor = DarkSurface,
                  labelColor = TextPrimary,
                  iconColor = TextSecondary
                )
              )
            }

            // Quick Add Box Button
            IconButton(
              onClick = {
                watermarkBoxes.add(
                  InpaintingAlgorithm.WatermarkBox(
                    id = System.currentTimeMillis(),
                    left = 0.35f,
                    top = 0.35f,
                    right = 0.65f,
                    bottom = 0.65f
                  )
                )
              },
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
            ) {
              Icon(
                imageVector = Icons.Rounded.AddBox,
                contentDescription = "Add Box",
                tint = AccentPurple
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Watermark Counter and Undo / Clear
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurface)
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (totalMarks == 0) {
                "Mark watermark area(s) on video frame"
              } else {
                "$totalMarks watermark area${if (totalMarks > 1) "s" else ""} marked"
              },
              color = if (totalMarks > 0) AccentPurple else TextMuted,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              if (totalMarks > 0) {
                IconButton(
                  onClick = {
                    if (watermarkBoxes.isNotEmpty()) {
                      watermarkBoxes.removeLast()
                    } else if (watermarkStrokes.isNotEmpty()) {
                      watermarkStrokes.removeLast()
                    }
                  },
                  modifier = Modifier.size(34.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                IconButton(
                  onClick = {
                    watermarkBoxes.clear()
                    watermarkStrokes.clear()
                  },
                  modifier = Modifier.size(34.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear All",
                    tint = ErrorRed,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))
        }

        // Progress Bar during video frame processing
        if (isProcessing) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            color = DarkSurfaceCard
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Processing Video Offline...",
                  color = TextPrimary,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )

                Text(
                  text = "${(progressPercentage * 100).roundToInt()}%",
                  color = AccentPurple,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              LinearProgressIndicator(
                progress = { progressPercentage },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .testTag("video_progress_bar"),
                color = AccentPurple,
                trackColor = DarkSurfaceElevated
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Frame $currentFrame of $totalFrames",
                  color = TextSecondary,
                  fontSize = 12.sp
                )

                Text(
                  text = "Cancel",
                  color = ErrorRed,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.clickable { isCancelled = true }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))
        }

        // Success banner
        AnimatedVisibility(visible = saveSuccessMsg != null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(SuccessGreen.copy(alpha = 0.15f))
              .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = SuccessGreen,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = saveSuccessMsg ?: "",
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Action Buttons
        if (processedVideoFile == null && !isProcessing) {
          Button(
            onClick = {
              if (totalMarks == 0) {
                Toast.makeText(context, "Please mark at least one watermark area", Toast.LENGTH_SHORT).show()
                return@Button
              }
              isProcessing = true
              isCancelled = false
              currentFrame = 0
              totalFrames = 1
              progressPercentage = 0f

              coroutineScope.launch {
                try {
                  val resultFile = VideoProcessor.processVideo(
                    context = context,
                    videoUri = currentUri,
                    boxes = watermarkBoxes,
                    strokes = watermarkStrokes,
                    onProgress = { frame, total, pct ->
                      currentFrame = frame
                      totalFrames = total
                      progressPercentage = pct
                    },
                    isCancelled = { isCancelled }
                  )
                  processedVideoFile = resultFile
                  isProcessing = false
                } catch (e: Exception) {
                  isProcessing = false
                  if (!isCancelled) {
                    Toast.makeText(context, "Processing error: ${e.message}", Toast.LENGTH_LONG).show()
                  }
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("remove_video_watermark_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentPurple,
              contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = totalMarks > 0
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoFixHigh,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Remove Watermarks from Video",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        } else if (processedVideoFile != null) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Prominent Download / Save Button
            Button(
              onClick = {
                val savedUri = StorageUtils.saveVideoToGallery(context, processedVideoFile!!)
                if (savedUri != null) {
                  saveSuccessMsg = "Saved successfully to Gallery (Movies/CleanMark)"
                  Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_video_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple,
                contentColor = TextPrimary
              ),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Download / Save Video",
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Download / Save Video",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Share Button
            OutlinedButton(
              onClick = {
                StorageUtils.shareVideo(context, processedVideoFile!!)
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("share_video_button"),
              shape = RoundedCornerShape(16.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(AccentPurple.copy(alpha = 0.6f))
              )
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = "Share Video",
                tint = AccentPurple,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Share Processed Video",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Adjust Watermark Areas Button
            OutlinedButton(
              onClick = {
                processedVideoFile = null
                saveSuccessMsg = null
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("adjust_video_marks_button"),
              shape = RoundedCornerShape(16.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
              )
            ) {
              Text(
                text = "Adjust Watermark Areas",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }
}
