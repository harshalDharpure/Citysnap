package com.prod.singles_date.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.prod.singles_date.R
import com.prod.singles_date.data.LocalPreferences
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.Badge
import com.prod.singles_date.model.ThemeMode
import com.prod.singles_date.model.Thought
import com.prod.singles_date.model.User
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.prod.singles_date.ui.components.GoogleSignInButton
import com.prod.singles_date.ui.components.MainBottomBar
import com.prod.singles_date.ui.components.MainTab
import com.prod.singles_date.ui.components.UserAvatar
import com.prod.singles_date.ui.components.ThoughtImageCarousel
import com.prod.singles_date.ui.util.copyInviteLink
import com.prod.singles_date.ui.util.rememberProfilePhotoPickerState
import com.prod.singles_date.ui.util.shareInviteLink
import com.prod.singles_date.util.AppLinks
import com.prod.singles_date.viewmodel.AuthViewModel
import com.prod.singles_date.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onChangeCity: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenNotificationInbox: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onOpenSponsorLead: () -> Unit = {},
    onOpenGuidelines: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSnap: () -> Unit = {},
    onNavigateHome: () -> Unit = onBack,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val firebaseUser by authViewModel.firebaseUser.collectAsStateWithLifecycle()
    val firestoreProfile by authViewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { LocalPreferences(context) }

    val fu = firebaseUser
    if (fu == null) {
        LaunchedEffect(Unit) { onLoggedOut() }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Reconnecting…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LaunchedEffect(fu.uid) {
        profileViewModel.setUid(fu.uid)
    }
    LaunchedEffect(fu.uid, firestoreProfile) {
        if (firestoreProfile == null) {
            authViewModel.ensureUserProfile()
        }
    }

    val user = remember(fu, firestoreProfile, prefs) {
        User.fromFirebase(fu, firestoreProfile, prefs)
    }

    val myThoughts by profileViewModel.myThoughts.collectAsStateWithLifecycle()
    val referralCount by profileViewModel.referralCount.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf("") }
    var showEditName by remember { mutableStateOf(false) }
    var editNameDraft by remember(user.name) { mutableStateOf(user.name) }
    val isUploadingPhoto by authViewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val isBusy by authViewModel.isBusy.collectAsStateWithLifecycle()
    val authMessage by authViewModel.authMessage.collectAsStateWithLifecycle()
    val profilePhotoPicker = rememberProfilePhotoPickerState { uri ->
        authViewModel.updateProfilePhoto(context, uri)
    }
    val isGoogleUser = remember(fu) {
        fu.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }
    val isEmailUser = remember(fu) {
        fu.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }
    val referralCode = user.referralCode.ifBlank { LocalPreferences.referralCodeForUid(user.uid) }
    val cityLabel = if (user.city.isNotBlank()) AppCity.displayName(user.city) else "No city set"
    val totalFeels = remember(myThoughts) { myThoughts.sumOf { it.feelCount } }
    val voiceScore = remember(myThoughts, user) {
        myThoughts.size * 10 + user.totalCommentsWritten * 5 + totalFeels
    }
    val postStreak = user.postStreak
    val badges = user.badges

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            authViewModel.logOut()
                            onLoggedOut()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log out", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        },
        bottomBar = {
            MainBottomBar(
                selectedTab = MainTab.Profile,
                profilePhotoUrl = user.photoUrl,
                profileName = user.name,
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.Home -> onNavigateHome()
                        MainTab.City -> onChangeCity()
                        MainTab.Snap -> onSnap()
                        MainTab.Profile -> Unit
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfileHeroCard(
                    name = user.name,
                    email = user.email,
                    photoUrl = user.photoUrl,
                    cityLabel = cityLabel,
                    postStreak = postStreak,
                    isUploadingPhoto = isUploadingPhoto,
                    onChangePhoto = profilePhotoPicker.onChangePhotoClick,
                    onChangeCity = onChangeCity,
                    onEditName = {
                        editNameDraft = user.name
                        showEditName = true
                    },
                )
            }

            item {
                ProfileStatsRow(
                    posts = myThoughts.size,
                    feels = totalFeels,
                    comments = user.totalCommentsWritten,
                    voiceScore = voiceScore,
                )
            }

            if (badges.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Badges",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        badges.forEach { badgeId ->
                            Text(
                                text = "${Badge.emoji(badgeId)} ${Badge.displayName(badgeId)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                AccountActionsCard(
                    onLogOut = {
                        authViewModel.logOut()
                        onLoggedOut()
                    },
                )
            }

            item {
                AppearanceCard(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                )
            }

            item {
                PremiumUpsellCard(
                    isPremium = user.isPremium,
                    onOpenPremium = onOpenPremium,
                    onOpenSponsorLead = onOpenSponsorLead,
                )
            }

            item {
                SettingsCard(
                    onActivity = onOpenNotificationInbox,
                    onBlockedUsers = onOpenBlockedUsers,
                    onNotifications = onOpenNotificationSettings,
                    onGuidelines = onOpenGuidelines,
                    onPrivacy = onOpenPrivacy,
                )
            }

            item {
                InviteCard(
                    referralCount = referralCount,
                    inviteUrl = AppLinks.inviteUrl(referralCode),
                    onCopy = { copyInviteLink(context, referralCode) },
                    onShare = { shareInviteLink(context, referralCode) },
                    showBadgeHint = referralCount < 3,
                )
            }

            item {
                Text(
                    text = "Your thoughts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (myThoughts.isEmpty()) {
                item {
                    EmptyThoughtsCard()
                }
            } else {
                items(myThoughts, key = { it.id }) { thought ->
                    ProfileThoughtCard(
                        thought = thought,
                        onDelete = { pendingDeleteId = thought.id },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        deletePassword = ""
                        deleteError = ""
                        showDeleteAccount = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete account")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    val deleteId = pendingDeleteId
    val deleteTarget = remember(deleteId, myThoughts) {
        deleteId?.let { id -> myThoughts.find { it.id == id } }
    }
    if (
        !deleteId.isNullOrBlank() &&
        deleteTarget != null &&
        deleteTarget.authorId == fu.uid
    ) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete thought?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.deleteThought(deleteId, fu.uid)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (profilePhotoPicker.showSourceSheet) {
        AlertDialog(
            onDismissRequest = profilePhotoPicker.onDismissSourceSheet,
            title = { Text("Profile photo") },
            text = { Text("Choose how to add your photo.") },
            confirmButton = {
                TextButton(onClick = profilePhotoPicker.onChooseGallery) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = profilePhotoPicker.onTakePhoto) {
                    Text("Camera")
                }
            },
        )
    }

    val pickerMessage = profilePhotoPicker.pickerMessage
    if (pickerMessage != null) {
        AlertDialog(
            onDismissRequest = profilePhotoPicker.onDismissPickerMessage,
            title = { Text("Photo") },
            text = { Text(pickerMessage) },
            confirmButton = {
                TextButton(onClick = profilePhotoPicker.onDismissPickerMessage) {
                    Text("OK")
                }
            },
        )
    }

    if (authMessage.isNotBlank() && !showEditName && !showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { authViewModel.clearMessage() },
            title = { Text("Profile") },
            text = { Text(authMessage) },
            confirmButton = {
                TextButton(onClick = { authViewModel.clearMessage() }) {
                    Text("OK")
                }
            },
        )
    }

    if (showEditName) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showEditName = false },
            title = { Text("Edit display name") },
            text = {
                OutlinedTextField(
                    value = editNameDraft,
                    onValueChange = { editNameDraft = it.take(60) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.updateUserName(editNameDraft) { showEditName = false }
                    },
                    enabled = editNameDraft.trim().isNotBlank() && !isBusy,
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditName = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showDeleteAccount = false },
            title = { Text("Delete account?") },
            text = {
                Column {
                    Text(
                        "This permanently deletes your account, all your posts, and your data. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isEmailUser) {
                        OutlinedTextField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it; deleteError = "" },
                            label = { Text("Confirm your password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(),
                        )
                    }
                    if (isGoogleUser && !isEmailUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleSignInButton(
                            enabled = !isBusy,
                            onSignIn = { token ->
                                authViewModel.deleteAccount(googleIdToken = token) { success, msg ->
                                    if (success) {
                                        showDeleteAccount = false
                                        onLoggedOut()
                                    } else {
                                        deleteError = msg
                                    }
                                }
                            },
                            onError = { deleteError = it },
                        )
                    }
                    if (deleteError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = deleteError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                if (isEmailUser) {
                    Button(
                        onClick = {
                            if (deletePassword.isBlank()) {
                                deleteError = "Enter your password to confirm."
                                return@Button
                            }
                            authViewModel.deleteAccount(password = deletePassword) { success, msg ->
                                if (success) {
                                    showDeleteAccount = false
                                    onLoggedOut()
                                } else {
                                    deleteError = msg
                                }
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(if (isBusy) "Deleting…" else "Delete forever")
                    }
                } else {
                    // Google-only users confirm via the button inside the dialog body.
                    Box(modifier = Modifier.size(0.dp))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAccount = false },
                    enabled = !isBusy,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AccountActionsCard(onLogOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLogOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log out")
        }
    }
}

@Composable
private fun AppearanceCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose light, dark, or match your phone",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(
    posts: Int,
    feels: Int,
    comments: Int,
    voiceScore: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileStatItem(value = posts.toString(), label = "Posts")
        ProfileStatItem(value = feels.toString(), label = "Feels")
        ProfileStatItem(value = comments.toString(), label = "Comments")
        ProfileStatItem(value = voiceScore.toString(), label = "Voice")
    }
}

@Composable
private fun ProfileStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileHeroCard(
    name: String,
    email: String,
    photoUrl: String,
    cityLabel: String,
    postStreak: Int,
    isUploadingPhoto: Boolean,
    onChangePhoto: () -> Unit,
    onChangeCity: () -> Unit,
    onEditName: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.clickable(enabled = !isUploadingPhoto, onClick = onChangePhoto),
            contentAlignment = Alignment.Center,
        ) {
            UserAvatar(
                name = name,
                photoUrl = photoUrl,
                size = 96.dp,
                showAccentRing = photoUrl.isNotBlank(),
            )
            if (isUploadingPhoto) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (postStreak > 0) {
            Text(
                text = "🔥 $postStreak day${if (postStreak == 1) "" else "s"} streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text(
            text = email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onEditName,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Edit name")
            }
            OutlinedButton(
                onClick = onChangeCity,
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(cityLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PremiumUpsellCard(
    isPremium: Boolean,
    onOpenPremium: () -> Unit,
    onOpenSponsorLead: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (isPremium) "Citysnap Premium active" else "Citysnap Premium — ₹99/mo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (isPremium) "Save posts, get locality alerts, support your city feed."
            else "Save unlimited posts and unlock locality alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenPremium) {
                Text(if (isPremium) "Manage" else "Learn more")
            }
            TextButton(onClick = onOpenSponsorLead) {
                Text("Advertise")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    onActivity: () -> Unit,
    onBlockedUsers: () -> Unit,
    onNotifications: () -> Unit,
    onGuidelines: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(8.dp),
    ) {
        Text(
            text = "Settings & safety",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        SettingsRow("Activity inbox", onActivity)
        SettingsRow("Blocked users", onBlockedUsers)
        SettingsRow("Notification preferences", onNotifications)
        SettingsRow("Community guidelines", onGuidelines)
        SettingsRow("Privacy policy", onPrivacy)
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InviteCard(
    referralCount: Int,
    inviteUrl: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    showBadgeHint: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "Invite friends",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (referralCount == 1) {
                "1 friend joined from your link"
            } else {
                "$referralCount friends joined from your link"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showBadgeHint) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Invite 3 friends to earn the Early Bangalore Voice badge 🎤",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = inviteUrl,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                .padding(12.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Copy link")
            }
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun ProfileThoughtCard(
    thought: Thought,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (thought.imageUrls.isNotEmpty()) {
            ThoughtImageCarousel(
                imageUrls = thought.imageUrls,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = thought.text.ifBlank { "Photo snap" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete thought",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${thought.feelCount} ${if (thought.feelCount == 1) "feel" else "feels"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (thought.city.isNotBlank()) {
                Text(
                    text = " · ${AppCity.displayName(thought.city)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyThoughtsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No thoughts yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Share what's on your mind from the feed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
