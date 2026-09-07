package com.example.ui.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.inpainting.InpaintingAlgorithm
import com.example.core.util.StorageUtils
import com.example.ui.components.SelectionMode
import com.example.ui.components.WatermarkCanvasOverlay
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AmbientBackgroundBrush
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OnAccentCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ImageScreen(
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var showCleanedPreview by remember { mutableStateOf(true) }

  val watermarkBoxes = remember { mutableStateListOf<InpaintingAlgorithm.WatermarkBox>() }
  val watermarkStrokes = remember { mutableStateListOf<InpaintingAlgorithm.WatermarkStroke>() }

  var selectionMode by remember { mutableStateOf(SelectionMode.RECTANGLE) }
  var isProcessing by remember { mutableStateOf(false) }
  var saveSuccessMsg by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      coroutineScope.launch {
        val loaded = loadBitmapFromUri(context, it)
        if (loaded != null) {
          originalBitmap = loaded
          processedBitmap = null
          watermarkBoxes.clear()
          watermarkStrokes.clear()
          saveSuccessMsg = null
          showCleanedPreview = true
        } else {
          Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
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
              .testTag("image_back_button")
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
              text = "Image Remover",
              color = TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Offline Inpainting Engine",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        if (originalBitmap != null) {
          IconButton(
            onClick = {
              photoPickerLauncher.launch("image/*")
            },
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .testTag("pick_another_image_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = "Pick Another",
              tint = AccentCyan
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Content Area: Either Picker or Image Editor
      val currentOriginal = originalBitmap
      if (currentOriginal == null) {
        // Empty State: Prominent Image Picker
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, DarkBorder, RoundedCornerShape(24.dp))
            .clickable {
              photoPickerLauncher.launch("image/*")
            }
            .testTag("select_image_button"),
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
                .background(AccentCyan.copy(alpha = 0.12f))
                .border(1.5.dp, AccentCyan.copy(alpha = 0.4f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = "Select Image",
                tint = AccentCyan,
                modifier = Modifier.size(40.dp)
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
              text = "Select Image from Gallery",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "JPG, JPEG, PNG, WEBP, BMP, HEIC supported\n100% offline watermark removal",
              color = TextMuted,
              fontSize = 13.sp,
              textAlign = TextAlign.Center
            )
          }
        }
      } else {
        // Image Display with Watermark Overlay
        val activeBitmap = if (processedBitmap != null && showCleanedPreview) {
          processedBitmap!!
        } else {
          currentOriginal
        }

        val totalMarks = watermarkBoxes.size + watermarkStrokes.size
        val aspectRatio = currentOriginal.width.toFloat() / currentOriginal.height.toFloat()

        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, DarkBorder, RoundedCornerShape(20.dp))
            .background(DarkSurfaceCard),
          contentAlignment = Alignment.Center
        ) {
          val containerWidth = maxWidth
          val targetHeight = (containerWidth.value / aspectRatio).dp.coerceIn(240.dp, 460.dp)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(targetHeight)
          ) {
            Image(
              bitmap = activeBitmap.asImageBitmap(),
              contentDescription = "Watermark image preview",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Fit
            )

            // Show interactive watermark selection overlay only when viewing original
            if (processedBitmap == null || !showCleanedPreview) {
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

            // Before / After Indicator Badge
            if (processedBitmap != null) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopStart)
                  .padding(12.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(DarkBg.copy(alpha = 0.85f))
                  .border(1.dp, if (showCleanedPreview) AccentCyan else DarkBorder, RoundedCornerShape(12.dp))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (showCleanedPreview) "CLEANED" else "ORIGINAL",
                  color = if (showCleanedPreview) AccentCyan else TextSecondary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Loading overlay
            if (isProcessing) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(DarkBg.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  CircularProgressIndicator(color = AccentCyan)
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = "Removing watermarks offline...",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Before / After Toggle Buttons (if processed)
        if (processedBitmap != null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated)
              .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (showCleanedPreview) AccentCyan else DarkSurfaceElevated)
                .clickable { showCleanedPreview = true }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "After (Cleaned)",
                color = if (showCleanedPreview) DarkBg else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (!showCleanedPreview) AccentCyan else DarkSurfaceElevated)
                .clickable { showCleanedPreview = false }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Before (Original)",
                color = if (!showCleanedPreview) DarkBg else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
        }

        // Watermark Selection Toolbar (shown when editing marks)
        if (processedBitmap == null || !showCleanedPreview) {
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
                  selectedContainerColor = AccentCyan,
                  selectedLabelColor = OnAccentCyan,
                  selectedLeadingIconColor = OnAccentCyan,
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
                  selectedContainerColor = AccentCyan,
                  selectedLabelColor = OnAccentCyan,
                  selectedLeadingIconColor = OnAccentCyan,
                  containerColor = DarkSurface,
                  labelColor = TextPrimary,
                  iconColor = TextSecondary
                )
              )
            }

            // Quick Add Box Button
            IconButton(
              onClick = {
                // Add a default centered box
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
                tint = AccentCyan
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Marks Status & Undo/Clear bar
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
                "Drag on watermark to mark it"
              } else {
                "$totalMarks watermark area${if (totalMarks > 1) "s" else ""} marked"
              },
              color = if (totalMarks > 0) AccentCyan else TextMuted,
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

          Spacer(modifier = Modifier.height(16.dp))
        }

        // Success message banner if saved
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

        // Primary Action Buttons
        if (processedBitmap == null) {
          Button(
            onClick = {
              if (totalMarks == 0) {
                Toast.makeText(context, "Please mark at least one watermark area", Toast.LENGTH_SHORT).show()
                return@Button
              }
              isProcessing = true
              coroutineScope.launch {
                val cleaned = withContext(Dispatchers.Default) {
                  val mask = InpaintingAlgorithm.createMaskBitmap(
                    currentOriginal.width,
                    currentOriginal.height,
                    watermarkBoxes,
                    watermarkStrokes
                  )
                  InpaintingAlgorithm.inpaint(currentOriginal, mask)
                }
                processedBitmap = cleaned
                showCleanedPreview = true
                isProcessing = false
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("remove_watermark_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = AccentCyan,
              contentColor = OnAccentCyan
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = !isProcessing && totalMarks > 0
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoFixHigh,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Remove Watermarks",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          // Download / Save, Share, and Adjust Buttons
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Prominent Download / Save Button
            Button(
              onClick = {
                val savedUri = StorageUtils.saveImageToGallery(context, processedBitmap!!)
                if (savedUri != null) {
                  saveSuccessMsg = "Saved successfully to Gallery (Pictures/CleanMark)"
                  Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("save_image_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = OnAccentCyan
              ),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Download / Save Image",
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Download / Save Image",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Share Button
            OutlinedButton(
              onClick = {
                StorageUtils.shareImage(context, processedBitmap!!)
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("share_image_button"),
              shape = RoundedCornerShape(16.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(AccentCyan.copy(alpha = 0.6f))
              )
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = "Share Image",
                tint = AccentCyan,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Share Processed Image",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Adjust Watermark Areas Button
            OutlinedButton(
              onClick = {
                processedBitmap = null
                saveSuccessMsg = null
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("edit_marks_button"),
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

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
  try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
      val original = BitmapFactory.decodeStream(stream) ?: return@use null
      // Downscale if image is gigantic (> 2400px) to keep memory footprint light and inpainting instantaneous
      val maxDim = max(original.width, original.height)
      if (maxDim > 2400) {
        val scale = 2400f / maxDim
        val w = (original.width * scale).toInt()
        val h = (original.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
        if (scaled != original) {
          original.recycle()
        }
        scaled
      } else {
        original
      }
    }
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}
