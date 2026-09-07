package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CleanMark AI", appName)
  }

  @Test
  fun `test offline inpainting removes watermark region`() {
    val width = 100
    val height = 100
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Draw blue background
    val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.BLUE }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Draw bright red watermark square in the center (40..60)
    val wmPaint = android.graphics.Paint().apply { color = android.graphics.Color.RED }
    canvas.drawRect(40f, 40f, 60f, 60f, wmPaint)

    // Mark the watermark box
    val boxes = listOf(
      com.example.core.inpainting.InpaintingAlgorithm.WatermarkBox(
        id = 1L,
        left = 0.38f,
        top = 0.38f,
        right = 0.62f,
        bottom = 0.62f
      )
    )

    val mask = com.example.core.inpainting.InpaintingAlgorithm.createMaskBitmap(width, height, boxes)
    val cleaned = com.example.core.inpainting.InpaintingAlgorithm.inpaint(bitmap, mask)

    // Verify center pixel is no longer red, and has been inpainted with surrounding blue
    val centerPixel = cleaned.getPixel(50, 50)
    val redComponent = (centerPixel ushr 16) and 0xFF
    val blueComponent = centerPixel and 0xFF

    // Blue should dominate and red should be significantly reduced
    org.junit.Assert.assertTrue("Red should be reduced after inpainting", redComponent < 100)
    org.junit.Assert.assertTrue("Blue should be restored from surroundings", blueComponent > 180)
  }
}
