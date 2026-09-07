package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AmbientBackgroundBrush
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderGlow
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MainScreen(
  onNavigateToImage: () -> Unit,
  onNavigateToVideo: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AmbientBackgroundBrush)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Subtle ambient atmospheric glow orbs for a unique aesthetic
    Box(
      modifier = Modifier
        .size(320.dp)
        .align(Alignment.TopEnd)
        .background(
          Brush.radialGradient(
            colors = listOf(AccentCyan.copy(alpha = 0.12f), Color.Transparent),
            radius = 360f
          )
        )
    )

    Box(
      modifier = Modifier
        .size(340.dp)
        .align(Alignment.BottomStart)
        .background(
          Brush.radialGradient(
            colors = listOf(AccentPurple.copy(alpha = 0.14f), Color.Transparent),
            radius = 380f
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header Section
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
      ) {
        // App Icon Badge with vibrant gradient & glowing outline
        Box(
          modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.linearGradient(
                listOf(AccentCyan, AccentPurple)
              )
            )
            .border(
              1.5.dp,
              Color.White.copy(alpha = 0.35f),
              RoundedCornerShape(18.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoFixHigh,
            contentDescription = "CleanMark AI",
            tint = Color(0xFF07091B),
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "CleanMark AI",
          color = TextPrimary,
          fontSize = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Offline Watermark Remover",
          color = TextSecondary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // The Two Large Requested Buttons: 1. IMAGE, 2. VIDEO
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        LargeFeatureButton(
          title = "IMAGE",
          subtitle = "Remove watermarks from photos",
          icon = Icons.Rounded.Image,
          accentColor = AccentCyan,
          testTag = "image_feature_button",
          onClick = onNavigateToImage
        )

        LargeFeatureButton(
          title = "VIDEO",
          subtitle = "MP4, MKV, MOV, AVI, 4K & all videos",
          icon = Icons.Rounded.Movie,
          accentColor = AccentPurple,
          testTag = "video_feature_button",
          onClick = onNavigateToVideo
        )
      }

      // Bottom Offline Guarantee Badge
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(DarkSurfaceElevated.copy(alpha = 0.75f))
          .border(1.dp, DarkBorderGlow.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
          .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Rounded.Shield,
          contentDescription = "Offline Secure",
          tint = AccentCyan,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "100% Offline • No Cloud • Zero Data Uploaded",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun LargeFeatureButton(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(142.dp)
      .clip(RoundedCornerShape(24.dp))
      .border(
        1.5.dp,
        Brush.linearGradient(
          listOf(accentColor.copy(alpha = 0.55f), DarkBorder)
        ),
        RoundedCornerShape(24.dp)
      )
      .clickable(onClick = onClick)
      .testTag(testTag),
    color = DarkSurfaceCard,
    tonalElevation = 8.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.18f), Color.Transparent),
            radius = 420f
          )
        )
        .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = title,
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
          )
        }

        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.18f))
            .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(36.dp)
          )
        }
      }
    }
  }
}
