package com.hybridtts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hybridtts.ui.model.NavigationItem
import com.hybridtts.ui.theme.AccentCyan
import com.hybridtts.ui.theme.AccentEmerald
import com.hybridtts.ui.theme.BorderSubtle
import com.hybridtts.ui.theme.DarkCard
import com.hybridtts.ui.theme.DarkSurface
import com.hybridtts.ui.theme.PureBlack
import com.hybridtts.ui.theme.TextPrimary
import com.hybridtts.ui.theme.TextSecondary

@Composable
fun MainScreen() {
    var currentTabIndex by remember { mutableIntStateOf(0) }
    val navItems = NavigationItem.items

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PureBlack,
        topBar = {
            Surface(
                color = PureBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand & Hardware Subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AccentEmerald, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "HYBRID-TTS // AGENT",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "HELIO G85 READY • 60 FPS",
                                    color = AccentEmerald,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // High-Tech Sleek Engine Badge (Never Wraps)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkCard,
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ENGINE 2: HYBRID",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimalist Separation Rule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderSubtle)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 0.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = currentTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureBlack,
                            selectedTextColor = AccentEmerald,
                            indicatorColor = AccentEmerald,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureBlack)
        ) {
            when (currentTabIndex) {
                0 -> StudioScreen()
                1 -> VoicesScreen()
                2 -> CommsScreen()
                3 -> SettingsScreen()
            }
        }
    }
}
