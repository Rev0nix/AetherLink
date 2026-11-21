package com.aetherlink.android.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCardBg

@Composable
fun AetherCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = AetherCardBg,
    borderColor: Color = AetherBorder,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
