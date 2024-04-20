package com.hpn.hmessager.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hpn.hmessager.data.model.Preference

private val DarkColorScheme = darkColorScheme(
    background = Grey33,
    primary = Blue,
    onPrimary = White,
    secondary = DarkBlue,
    onSecondary = GreyBB,
    tertiary = Green,
    primaryContainer = Grey4D,
    onPrimaryContainer = Grey88,
    error = Red
)

private val DeepDarkColorScheme = darkColorScheme(
    background = Grey1A,
    primary = DarkBlue,
    onPrimary = White,
    secondary = DarkBlue2,
    onSecondary = Grey99,
    tertiary = GreenDD,
    primaryContainer = Grey33,
    onPrimaryContainer = Grey66,
    error = RedDD
)

private val LightColorScheme = lightColorScheme(
    background = White,
    primary = BlueLight,
    onPrimary = Black,
    secondary = DarkBlue,
    onSecondary = GreyBB,
    tertiary = Green,
    primaryContainer = GreyF1,
    onPrimaryContainer = Grey88,
    error = Red
)

val Themes = listOf(DarkColorScheme, DeepDarkColorScheme, LightColorScheme)
val ThemesName = listOf("Dark", "Deep Dark", "Light")

@Composable
fun HMessagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pref : Preference? = null,
    content: @Composable () -> Unit,
) {
    val themeId = pref?.themeId ?: 0
    val colorScheme = Themes[themeId]

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = colorScheme.primaryContainer.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}