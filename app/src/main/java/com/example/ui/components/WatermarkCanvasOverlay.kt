package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.inpainting.InpaintingAlgorithm
import kotlin.math.max
import kotlin.math.min

enum class SelectionMode {
  RECTANGLE,
  BRUSH
}

@Composable
fun WatermarkCanvasOverlay(
  modifier: Modifier = Modifier,
  mode: SelectionMode,
  brushSizeRatio: Float = 0.05f,
  boxes: List<InpaintingAlgorithm.WatermarkBox>,
  strokes: List<InpaintingAlgorithm.WatermarkStroke>,
  onAddBox: (InpaintingAlgorithm.WatermarkBox) -> Unit,
  onAddStroke: (InpaintingAlgorithm.WatermarkStroke) -> Unit,
  enabled: Boolean = true
) {
  var currentDragStart by remember { mutableStateOf<Offset?>(null) }
  var currentDragEnd by remember { mutableStateOf<Offset?>(null) }
  val currentStrokePoints = remember { mutableListOf<Pair<Float, Float>>() }
  var strokeUpdateTrigger by remember { mutableStateOf(0) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .pointerInput(mode, enabled) {
        if (!enabled) return@pointerInput

        detectDragGestures(
          onDragStart = { offset ->
            val relX = (offset.x / size.width).coerceIn(0f, 1f)
            val relY = (offset.y / size.height).coerceIn(0f, 1f)

            if (mode == SelectionMode.RECTANGLE) {
              currentDragStart = offset
              currentDragEnd = offset
            } else {
              currentStrokePoints.clear()
              currentStrokePoints.add(Pair(relX, relY))
              strokeUpdateTrigger++
            }
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val newOffset = (currentDragEnd ?: change.position) + dragAmount
            val relX = (change.position.x / size.width).coerceIn(0f, 1f)
            val relY = (change.position.y / size.height).coerceIn(0f, 1f)

            if (mode == SelectionMode.RECTANGLE) {
              currentDragEnd = newOffset
            } else {
              currentStrokePoints.add(Pair(relX, relY))
              strokeUpdateTrigger++
            }
          },
          onDragEnd = {
            if (mode == SelectionMode.RECTANGLE) {
              val start = currentDragStart
              val end = currentDragEnd
              if (start != null && end != null) {
                val l = min(start.x, end.x) / size.width
                val t = min(start.y, end.y) / size.height
                val r = max(start.x, end.x) / size.width
                val b = max(start.y, end.y) / size.height

                // Only add if box has meaningful size (> 1% of dimension)
                if (r - l > 0.015f && b - t > 0.015f) {
                  onAddBox(
                    InpaintingAlgorithm.WatermarkBox(
                      id = System.currentTimeMillis(),
                      left = l.coerceIn(0f, 1f),
                      top = t.coerceIn(0f, 1f),
                      right = r.coerceIn(0f, 1f),
                      bottom = b.coerceIn(0f, 1f)
                    )
                  )
                }
              }
              currentDragStart = null
              currentDragEnd = null
            } else {
              if (currentStrokePoints.size >= 2) {
                onAddStroke(
                  InpaintingAlgorithm.WatermarkStroke(
                    id = System.currentTimeMillis(),
                    points = currentStrokePoints.toList(),
                    strokeWidthRatio = brushSizeRatio
                  )
                )
              }
              currentStrokePoints.clear()
              strokeUpdateTrigger++
            }
          },
          onDragCancel = {
            currentDragStart = null
            currentDragEnd = null
            currentStrokePoints.clear()
            strokeUpdateTrigger++
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 0 || h <= 0) return@Canvas

      // 1. Draw saved strokes
      for (stroke in strokes) {
        if (stroke.points.size < 2) continue
        val path = Path()
        val p0 = stroke.points[0]
        path.moveTo(p0.first * w, p0.second * h)
        for (i in 1 until stroke.points.size) {
          val p = stroke.points[i]
          path.lineTo(p.first * w, p.second * h)
        }
        val strokeWidth = max(8f, stroke.strokeWidthRatio * min(w, h))
        drawPath(
          path = path,
          color = Color(0x77F43F5E),
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }

      // 2. Draw currently drawing stroke
      if (currentStrokePoints.size >= 2) {
        val path = Path()
        val p0 = currentStrokePoints[0]
        path.moveTo(p0.first * w, p0.second * h)
        for (i in 1 until currentStrokePoints.size) {
          val p = currentStrokePoints[i]
          path.lineTo(p.first * w, p.second * h)
        }
        val strokeWidth = max(8f, brushSizeRatio * min(w, h))
        drawPath(
          path = path,
          color = Color(0x99F43F5E),
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }

      // 3. Draw saved watermark boxes
      boxes.forEachIndexed { index, box ->
        val l = box.left * w
        val t = box.top * h
        val r = box.right * w
        val b = box.bottom * h
        val rectWidth = max(0f, r - l)
        val rectHeight = max(0f, b - t)

        // Semi-transparent highlight
        drawRect(
          color = Color(0x4400E5FF),
          topLeft = Offset(l, t),
          size = Size(rectWidth, rectHeight)
        )

        // Crisp border
        drawRect(
          color = Color(0xFF00E5FF),
          topLeft = Offset(l, t),
          size = Size(rectWidth, rectHeight),
          style = Stroke(width = 2.5.dp.toPx())
        )

        // Corner accents
        val cornerSize = min(rectWidth * 0.25f, 16.dp.toPx())
        drawRect(
          color = Color.White,
          topLeft = Offset(l, t),
          size = Size(cornerSize, 3.dp.toPx())
        )
        drawRect(
          color = Color.White,
          topLeft = Offset(l, t),
          size = Size(3.dp.toPx(), cornerSize)
        )
      }

      // 4. Draw currently dragging rectangle
      val start = currentDragStart
      val end = currentDragEnd
      if (start != null && end != null) {
        val l = min(start.x, end.x).coerceIn(0f, w)
        val t = min(start.y, end.y).coerceIn(0f, h)
        val r = max(start.x, end.x).coerceIn(0f, w)
        val b = max(start.y, end.y).coerceIn(0f, h)
        val rectWidth = max(0f, r - l)
        val rectHeight = max(0f, b - t)

        drawRect(
          color = Color(0x5500E5FF),
          topLeft = Offset(l, t),
          size = Size(rectWidth, rectHeight)
        )
        drawRect(
          color = Color(0xFF00E5FF),
          topLeft = Offset(l, t),
          size = Size(rectWidth, rectHeight),
          style = Stroke(width = 2.5.dp.toPx())
        )
      }
    }
  }
}
