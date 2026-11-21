package com.aetherlink.android.feature.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherlink.android.R
import com.aetherlink.android.core.designsystem.AetherBg
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherGradientPrimary
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherTextPrimary
import com.aetherlink.android.core.designsystem.AetherTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2200)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Outer Ring
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AetherPurple.copy(alpha = glowAlpha),
                                AetherCyan.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Central Logo Container with Modern AetherLink Icon
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.5.dp, AetherCyan.copy(alpha = 0.8f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_aetherlink_logo),
                        contentDescription = "AetherLink Logo",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "AetherLink",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AetherTextPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "One Connection. Every Device.",
                fontSize = 14.sp,
                color = AetherTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
