package com.prod.singles_date.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class MainTab {
    Home,
    Snap,
    City,
    Profile,
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    profilePhotoUrl: String,
    profileName: String,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        BottomBarItem(
            tab = MainTab.Home,
            selectedTab = selectedTab,
            icon = Icons.Filled.Home,
            label = "Home",
            onClick = { onTabSelected(MainTab.Home) },
        )
        BottomBarItem(
            tab = MainTab.City,
            selectedTab = selectedTab,
            icon = Icons.Filled.LocationOn,
            label = "City",
            onClick = { onTabSelected(MainTab.City) },
        )
        NavigationBarItem(
            selected = false,
            onClick = { onTabSelected(MainTab.Snap) },
            icon = {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, CircleShape, clip = false)
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Snap",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            },
            label = {
                Text(
                    text = "Snap",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.surface,
            ),
        )
        BottomBarItem(
            tab = MainTab.Profile,
            selectedTab = selectedTab,
            icon = Icons.Filled.Person,
            label = "Profile",
            onClick = { onTabSelected(MainTab.Profile) },
            profilePhotoUrl = profilePhotoUrl,
            profileName = profileName,
        )
    }
}

@Composable
private fun RowScope.BottomBarItem(
    tab: MainTab,
    selectedTab: MainTab,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    profilePhotoUrl: String = "",
    profileName: String = "",
) {
    val selected = selectedTab == tab
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            if (tab == MainTab.Profile && profilePhotoUrl.isNotBlank()) {
                UserAvatar(
                    name = profileName,
                    photoUrl = profilePhotoUrl,
                    size = 26.dp,
                    showAccentRing = selected,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ),
    )
}
