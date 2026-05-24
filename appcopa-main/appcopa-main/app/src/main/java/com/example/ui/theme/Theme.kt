package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LightMint,
    primaryContainer = MediumFieldGreen,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = GoldAccent,
    tertiary = SoftGreen,
    background = DarkSlateBg,
    surface = DarkSlateBg,
    surfaceContainer = SlateGrey,
    surfaceContainerHigh = ElevatedGrey,
    onBackground = LightAshText,
    onSurface = LightAshText,
    onSurfaceVariant = MutedAshText,
    error = ErrorRed
  )

private val LightColorScheme = DarkColorScheme // Standard forced dark mode to preserve stadium-night atmosphere!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  // Brand identity requires Dark theme by default, disable dynamic colors that override branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
