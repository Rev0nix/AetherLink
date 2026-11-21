package com.aetherlink.android.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aetherlink.android.core.designsystem.AetherBgSecondary
import com.aetherlink.android.core.designsystem.AetherBorder
import com.aetherlink.android.core.designsystem.AetherCyan
import com.aetherlink.android.core.designsystem.AetherPurple
import com.aetherlink.android.core.designsystem.AetherTextMuted

@Composable
fun AetherBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only display on main 5 tabs
    val isTabRoute = Screen.bottomBarTabs.any { it.route == currentRoute }
    if (!isTabRoute) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(AetherBgSecondary.copy(alpha = 0.95f))
                .border(1.dp, AetherBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomBarTabs.forEach { tab ->
                val selected = currentRoute == tab.route

                val iconTint by animateColorAsState(
                    targetValue = if (selected) AetherCyan else AetherTextMuted,
                    label = "iconTint"
                )

                val pillBg by animateColorAsState(
                    targetValue = if (selected) AetherPurple.copy(alpha = 0.2f) else Color.Transparent,
                    label = "pillBg"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = tab.title,
                        color = iconTint,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(AetherCyan)
                        )
                    }
                }
            }
        }
    }
}
