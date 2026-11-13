package org.example.project.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    COMPACT,   // < 600dp (Mobile phones)
    MEDIUM,    // 600dp - 840dp (Tablets, foldables)
    EXPANDED   // >= 840dp (Desktop, Web dashboard)
}

fun calculateWindowSizeClass(width: Dp): WindowSizeClass {
    return when {
        width < 600.dp -> WindowSizeClass.COMPACT
        width < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}
