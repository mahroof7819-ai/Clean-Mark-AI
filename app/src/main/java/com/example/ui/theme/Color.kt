package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Unique "Midnight Cosmic Sapphire & Radiant Aurora" Palette
// Replaces generic pitch-black with rich luminous cosmic tones
val DarkBg = Color(0xFF0A0E27)               // Deep Cosmic Midnight Sapphire
val DarkBgTop = Color(0xFF111842)            // Luminous upper indigo twilight
val DarkBgBottom = Color(0xFF060918)         // Deep cosmic sapphire abyss
val DarkSurface = Color(0xFF121B45)          // Deep royal navy surface
val DarkSurfaceElevated = Color(0xFF1B275E)  // Elevated cobalt sapphire
val DarkSurfaceCard = Color(0xFF162152)      // Rich frosted sapphire card
val DarkBorder = Color(0xFF2C3D7D)           // Luminous navy-indigo border
val DarkBorderGlow = Color(0xFF445CB8)       // Subtle accent rim border

// Ambient vertical gradient for rich background depth
val AmbientBackgroundBrush = Brush.verticalGradient(
  listOf(DarkBgTop, DarkBg, DarkBgBottom)
)

// Accent Colors: High-energy electric neon
val AccentCyan = Color(0xFF00F5D4)           // Electric Mint / Cyber Cyan
val AccentCyanDark = Color(0xFF00B4D8)
val AccentCyanContainer = Color(0xFF0D3D52)
val OnAccentCyan = Color(0xFF02232E)

val AccentPurple = Color(0xFFC084FC)         // Radiant Electric Orchid / Violet
val AccentPurpleDark = Color(0xFFA855F7)
val AccentPurpleContainer = Color(0xFF4C1D95)

// Unique highlight accents
val AccentCoral = Color(0xFFFF5376)          // Glowing Coral Rose
val AccentAmber = Color(0xFFFFB703)          // Radiant Amber Glow

// Crisp high-contrast readable typography with subtle sapphire tint
val TextPrimary = Color(0xFFF8FAFC)          // Diamond white
val TextSecondary = Color(0xFFA5B4FC)        // Soft luminous periwinkle
val TextMuted = Color(0xFF6E7FA8)            // Cosmic slate periwinkle

val SuccessGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFFF3366)

