package com.prod.singles_date.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prod.singles_date.R
import com.prod.singles_date.data.LocalPreferences
import com.prod.singles_date.model.AppCity
import com.prod.singles_date.model.AppLocality
import com.prod.singles_date.model.User
import com.prod.singles_date.ui.components.CityMoodCard
import com.prod.singles_date.ui.components.CommentsSheet
import com.prod.singles_date.ui.components.EditDialog
import com.prod.singles_date.ui.components.FeedFilterBar
import com.prod.singles_date.ui.components.MainBottomBar
import com.prod.singles_date.ui.components.MainTab
import com.prod.singles_date.ui.components.PostDialog
import com.prod.singles_date.ui.components.ThoughtCard
import com.prod.singles_date.ui.util.copyThoughtToClipboard
import com.prod.singles_date.ui.util.shareCityMoodCard
import com.prod.singles_date.ui.util.shareThoughtAsImage
import com.prod.singles_date.ui.util.shareThoughtToWhatsApp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.prod.singles_date.ui.components.ReportReasonDialog
import androidx.compose.material.icons.filled.Notifications
import com.prod.singles_date.util.FeedSortMode
import com.prod.singles_date.util.DailyPrompts
import com.prod.singles_date.util.NetworkMonitor
import com.prod.singles_date.viewmodel.AuthViewModel
import com.prod.singles_date.viewmodel.ThoughtViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    authViewModel: AuthViewModel,
    thoughtViewModel: ThoughtViewModel,
    onOpenProfile: () -> Unit,
    onRequireLogin: () -> Unit,
    onChangeCity: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
) {
    val thoughts by thoughtViewModel.visibleThoughts.collectAsStateWithLifecycle()
    val feeledIds by thoughtViewModel.feeledThoughtIds.collectAsStateWithLifecycle()
    val firebaseUser by authViewModel.firebaseUser.collectAsStateWithLifecycle()
    val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
    val fu = firebaseUser
    val isLoggedIn = fu != null

    LaunchedEffect(fu?.uid) {
        thoughtViewModel.setCurrentUid(fu?.uid)
        fu?.uid?.let { authViewModel.registerPushToken(it) }
    }

    val user: User? = if (fu != null) {
        profile ?: run {
            val email = fu.email.orEmpty()
            User(uid = fu.uid, name = email.substringBefore('@').ifBlank { "You" }, email = email)
        }
    } else null

    val context = LocalContext.current
    val prefs = remember { LocalPreferences(context) }
    val appName = stringResource(R.string.app_name)

    val activeCity = when {
        profile?.city?.isNotBlank() == true -> profile!!.city
        else -> prefs.getGuestCity()
    }
    val activeLocality = when {
        profile?.locality?.isNotBlank() == true -> profile!!.locality
        else -> prefs.getGuestLocality()
    }

    LaunchedEffect(activeCity) {
        if (activeCity.isNotBlank()) thoughtViewModel.setSelectedCity(activeCity)
    }
    LaunchedEffect(activeLocality) {
        thoughtViewModel.setSelectedLocality(activeLocality)
    }

    val feedLocalityFilter by thoughtViewModel.feedLocalityFilter.collectAsStateWithLifecycle()
    val feedCategoryFilter by thoughtViewModel.feedCategoryFilter.collectAsStateWithLifecycle()
    val feedSortMode by thoughtViewModel.feedSortMode.collectAsStateWithLifecycle()
    val workOnlyFilter by thoughtViewModel.workOnlyFilter.collectAsStateWithLifecycle()
    val snapsOnlyFilter by thoughtViewModel.snapsOnlyFilter.collectAsStateWithLifecycle()
    val savedThoughtIds by thoughtViewModel.savedThoughtIds.collectAsStateWithLifecycle()
    val canLoadMore by thoughtViewModel.canLoadMore.collectAsStateWithLifecycle()
    val unreadNotifications by thoughtViewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val searchQuery by thoughtViewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by thoughtViewModel.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val sort = prefs.getFeedSortMode()
        thoughtViewModel.setFeedSortMode(if (sort == "hot") FeedSortMode.HOT else FeedSortMode.NEW)
    }

    val dayOfYear = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val isPosting by thoughtViewModel.isPosting.collectAsStateWithLifecycle()
    val postError by thoughtViewModel.postError.collectAsStateWithLifecycle()
    val cityMood by thoughtViewModel.cityMood.collectAsStateWithLifecycle()
    val pendingPostPrompt by thoughtViewModel.pendingPostPrompt.collectAsStateWithLifecycle()

    var showPost by remember { mutableStateOf(false) }
    var postPrompt by remember { mutableStateOf<String?>(null) }
    var awaitingPostResult by remember { mutableStateOf(false) }
    var editThoughtId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var reportThoughtId by remember { mutableStateOf<String?>(null) }
    var reportCommentTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    val isOnline = remember { mutableStateOf(NetworkMonitor.isOnline(context)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showFeedTips by remember { mutableStateOf(!prefs.hasSeenFeedTips()) }
    val requestOpenComposer by thoughtViewModel.requestOpenComposer.collectAsStateWithLifecycle()

    LaunchedEffect(requestOpenComposer) {
        if (requestOpenComposer) {
            if (isLoggedIn) showPost = true else onRequireLogin()
            thoughtViewModel.clearOpenComposerRequest()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val code = prefs.getPendingReferralCode()
            if (code.isNotBlank()) {
                authViewModel.applyPendingReferral(code)
                prefs.clearPendingReferralCode()
            }
        }
    }

    LaunchedEffect(pendingPostPrompt, isLoggedIn) {
        val prompt = pendingPostPrompt ?: return@LaunchedEffect
        postPrompt = prompt
        if (isLoggedIn) showPost = true else onRequireLogin()
        thoughtViewModel.clearPendingPostPrompt()
    }

    LaunchedEffect(isPosting, postError) {
        if (!awaitingPostResult || isPosting) return@LaunchedEffect
        if (postError == null) {
            thoughtViewModel.setFeedLocalityFilter("")
            thoughtViewModel.setSnapsOnlyFilter(false)
            thoughtViewModel.setSearchQuery("")
            prefs.setFeedLocalityFilter("")
            showPost = false
            postPrompt = null
            runCatching { listState.animateScrollToItem(0) }
        }
        awaitingPostResult = false
    }

    val dailyPrompt = remember(activeCity, dayOfYear) {
        if (activeCity.isNotBlank()) DailyPrompts.promptFor(activeCity, dayOfYear) else null
    }
    var showDailyPrompt by remember(activeCity, dayOfYear) {
        mutableStateOf(dailyPrompt != null && prefs.getLastPromptDay() != dayOfYear)
    }

    val emptyMessage = when {
        !isOnline.value -> "You're offline.\nCheck your connection and pull down to refresh."
        searchQuery.isNotBlank() -> "No posts match your search.\nTry different keywords."
        snapsOnlyFilter -> "No photo snaps yet.\nBe the first to share one!"
        feedLocalityFilter.isNotBlank() -> "Nothing in this area yet.\nTry All ${AppCity.displayName(activeCity)} or post first."
        isLoggedIn -> "No thoughts yet.\nTap Snap to share one."
        else -> "No thoughts yet."
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onChangeCity),
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Column {
                            Text(
                                text = if (activeCity.isNotBlank()) {
                                    AppCity.displayName(activeCity)
                                } else {
                                    appName
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (activeLocality.isNotBlank()) {
                                Text(
                                    text = AppLocality.displayName(activeLocality),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = onOpenNotifications) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Activity")
                        }
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    if (!isLoggedIn) {
                        TextButton(onClick = onRequireLogin) {
                            Text("Log in", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
        },
        bottomBar = {
            MainBottomBar(
                selectedTab = MainTab.Home,
                profilePhotoUrl = user?.photoUrl.orEmpty(),
                profileName = user?.name.orEmpty(),
                onTabSelected = { tab ->
                    when (tab) {
                        MainTab.Home -> scope.launch { listState.animateScrollToItem(0) }
                        MainTab.Snap -> when {
                            !isLoggedIn -> onRequireLogin()
                            activeCity.isBlank() -> onChangeCity()
                            else -> showPost = true
                        }
                        MainTab.City -> onChangeCity()
                        MainTab.Profile -> onOpenProfile()
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (activeCity.isNotBlank()) {
                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { thoughtViewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(),
                    )
                }
                FeedFilterBar(
                    cityId = activeCity,
                    selectedLocality = feedLocalityFilter,
                    snapsOnly = snapsOnlyFilter,
                    workOnly = workOnlyFilter,
                    selectedCategory = feedCategoryFilter,
                    sortMode = feedSortMode,
                    onLocalitySelected = {
                        thoughtViewModel.setFeedLocalityFilter(it)
                        prefs.setFeedLocalityFilter(it)
                    },
                    onSnapsOnlySelected = { thoughtViewModel.setSnapsOnlyFilter(it) },
                    onWorkOnlySelected = { thoughtViewModel.setWorkOnlyFilter(it) },
                    onCategorySelected = {
                        thoughtViewModel.setFeedCategoryFilter(it)
                        prefs.setFeedCategoryFilter(it)
                    },
                    onSortModeSelected = {
                        thoughtViewModel.setFeedSortMode(it)
                        prefs.setFeedSortMode(if (it == FeedSortMode.HOT) "hot" else "new")
                    },
                )
            }

            if (showFeedTips) {
                FeedTipsBanner(
                    onDismiss = {
                        showFeedTips = false
                        prefs.setSeenFeedTips(true)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (!isOnline.value) {
                Text(
                    text = "You're offline — pull down to refresh",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFFFFC107).copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (showDailyPrompt && dailyPrompt != null) {
                DailyPromptBanner(
                    prompt = dailyPrompt,
                    onWrite = {
                        if (isLoggedIn) {
                            postPrompt = dailyPrompt
                            showPost = true
                        } else {
                            onRequireLogin()
                        }
                        showDailyPrompt = false
                        prefs.setLastPromptDay(dayOfYear)
                    },
                    onDismiss = {
                        showDailyPrompt = false
                        prefs.setLastPromptDay(dayOfYear)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (!isLoggedIn) {
                GuestFeedBanner(
                    onSignIn = onRequireLogin,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isOnline.value = NetworkMonitor.isOnline(context)
                    thoughtViewModel.refreshFeed()
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (thoughts.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        cityMood?.let { mood ->
                            item(key = "city_mood") {
                                CityMoodCard(
                                    mood = mood,
                                    onShare = { shareCityMoodCard(context, mood.city, mood.lines) },
                                )
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    cityMood?.let { mood ->
                        item(key = "city_mood") {
                            CityMoodCard(
                                mood = mood,
                                onShare = {
                                    shareCityMoodCard(context, mood.city, mood.lines)
                                },
                            )
                        }
                    }
                    itemsIndexed(thoughts, key = { _, t -> t.id }) { index, thought ->
                        ThoughtCard(
                            thought = thought,
                            hasFeeled = thought.id in feeledIds,
                            userLocality = activeLocality,
                            rank = index + 1,
                            authorPhotoUrl = when {
                                thought.authorPhotoUrl.isNotBlank() -> thought.authorPhotoUrl
                                thought.authorId == fu?.uid -> user?.photoUrl.orEmpty()
                                else -> ""
                            },
                            onFeelThis = {
                                if (isLoggedIn && fu != null) {
                                    thoughtViewModel.onFeelThis(thought.id, fu.uid)
                                } else onRequireLogin()
                            },
                            onCopy = {
                                copyThoughtToClipboard(context, thought.text, thought.id, thought.city.ifBlank { activeCity })
                                thoughtViewModel.incrementShareCount(thought.id)
                            },
                            onShareImage = {
                                shareThoughtAsImage(
                                    context = context,
                                    thoughtText = thought.text,
                                    authorName = thought.displayAuthorName(),
                                    cityId = thought.city.ifBlank { activeCity },
                                    feelCount = thought.feelCount,
                                    imageUrl = thought.imageUrls.firstOrNull(),
                                )
                                thoughtViewModel.incrementShareCount(thought.id)
                            },
                            onWhatsApp = {
                                shareThoughtToWhatsApp(context, thought.text, thought.id, thought.city.ifBlank { activeCity })
                                thoughtViewModel.incrementShareCount(thought.id)
                            },
                            onComment = {
                                if (isLoggedIn) thoughtViewModel.setCommentsTarget(thought.id)
                                else onRequireLogin()
                            },
                            showComments = isLoggedIn,
                            showFeelButton = isLoggedIn,
                            onEdit = if (isLoggedIn && fu != null && thought.authorId == fu.uid) {
                                { editThoughtId = thought.id }
                            } else null,
                            onDelete = if (isLoggedIn && fu != null && thought.authorId == fu.uid) {
                                { pendingDeleteId = thought.id }
                            } else null,
                            onReport = if (isLoggedIn && fu != null && thought.authorId != fu.uid) {
                                { reportThoughtId = thought.id }
                            } else null,
                            onBlock = if (isLoggedIn && fu != null && thought.authorId != fu.uid) {
                                { thoughtViewModel.blockAuthor(fu.uid, thought.authorId) }
                            } else null,
                            onAuthorClick = if (thought.authorId.isNotBlank()) {
                                { onOpenUserProfile(thought.authorId) }
                            } else null,
                            onSave = if (isLoggedIn && fu != null && user?.isPremium == true) {
                                {
                                    val save = thought.id !in savedThoughtIds
                                    thoughtViewModel.toggleSaveThought(fu.uid, thought.id, save)
                                }
                            } else null,
                            isSaved = thought.id in savedThoughtIds,
                            onOpenDetail = { onOpenPost(thought.id) },
                        )
                    }
                    if (canLoadMore) {
                        item(key = "load_more") {
                            TextButton(
                                onClick = { thoughtViewModel.loadMoreFeed() },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                Text("Load more posts")
                            }
                        }
                    }
                }
            }
            }
        }

        if (showPost && isLoggedIn && fu != null && user != null && activeCity.isNotBlank()) {
            PostDialog(
                cityLabel = AppCity.displayName(activeCity),
                localityLabel = activeLocality.takeIf { it.isNotBlank() }
                    ?.let { AppLocality.displayName(it) },
                onDismiss = {
                    if (!isPosting) {
                        showPost = false
                        postPrompt = null
                        thoughtViewModel.clearPostError()
                    }
                },
                promptText = postPrompt,
                isPosting = isPosting,
                postError = postError,
                onPostThought = { text, category, postType, imageUris ->
                    awaitingPostResult = true
                    thoughtViewModel.clearPostError()
                    thoughtViewModel.postThought(
                        rawText = text,
                        authorId = fu.uid,
                        authorName = user.name,
                        authorPhotoUrl = user.photoUrl,
                        city = activeCity,
                        locality = activeLocality,
                        category = category,
                        postType = postType,
                        context = context,
                        imageUris = imageUris,
                    )
                },
            )
        }

        val activeThoughtId = thoughtViewModel.activeCommentsThoughtId.collectAsStateWithLifecycle().value
        val comments = thoughtViewModel.activeComments.collectAsStateWithLifecycle().value
        if (!activeThoughtId.isNullOrBlank() && isLoggedIn && fu != null && user != null) {
            CommentsSheet(
                currentUserName = user.name,
                comments = comments,
                currentUserId = fu.uid,
                onDismiss = { thoughtViewModel.setCommentsTarget(null) },
                onPostComment = { body ->
                    thoughtViewModel.addComment(activeThoughtId, fu.uid, user.name, body)
                },
                onReportComment = { commentId ->
                    reportCommentTarget = activeThoughtId to commentId
                },
            )
        }

        val etId = editThoughtId
        val et = remember(etId, thoughts) { etId?.let { id -> thoughts.find { it.id == id } } }
        if (et != null && isLoggedIn && fu != null && et.authorId == fu.uid) {
            EditDialog(
                initialText = et.text,
                allowEmptySave = et.imageUrls.isNotEmpty(),
                onDismiss = { editThoughtId = null },
                onSave = { new ->
                    thoughtViewModel.updateThought(
                        thoughtId = et.id,
                        rawText = new,
                        allowEmpty = et.imageUrls.isNotEmpty(),
                    )
                    editThoughtId = null
                },
            )
        }

        val deleteId = pendingDeleteId
        val deleteTarget = remember(deleteId, thoughts) {
            deleteId?.let { id -> thoughts.find { it.id == id } }
        }
        if (
            !deleteId.isNullOrBlank() &&
            deleteTarget != null &&
            isLoggedIn &&
            fu != null &&
            deleteTarget.authorId == fu.uid
        ) {
            AlertDialog(
                onDismissRequest = { pendingDeleteId = null },
                title = { Text("Delete thought?") },
                text = { Text("This can't be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            thoughtViewModel.deleteThought(deleteId, fu.uid)
                            pendingDeleteId = null
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteId = null }) {
                        Text("Cancel")
                    }
                },
            )
        }

        val reportId = reportThoughtId
        if (reportId != null && fu != null) {
            ReportReasonDialog(
                title = "Report post",
                onDismiss = { reportThoughtId = null },
                onConfirm = { reason ->
                    thoughtViewModel.reportThought(reportId, fu.uid, reason)
                    reportThoughtId = null
                },
            )
        }

        val commentReport = reportCommentTarget
        if (commentReport != null && fu != null) {
            val (thoughtId, commentId) = commentReport
            ReportReasonDialog(
                title = "Report comment",
                onDismiss = { reportCommentTarget = null },
                onConfirm = { reason ->
                    thoughtViewModel.reportComment(thoughtId, commentId, fu.uid, reason)
                    reportCommentTarget = null
                },
            )
        }
    }
}

@Composable
private fun FeedTipsBanner(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .padding(12.dp),
    ) {
        Text("Quick tips", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text("• Hot = trending in your city\n• Local Notes = jobs, rent, longer stories\n• Tap a name to see their profile", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onDismiss) { Text("Got it") }
    }
}

@Composable
private fun GuestFeedBanner(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onSignIn)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Browsing as guest · Sign in to post or comment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DailyPromptBanner(
    prompt: String,
    onWrite: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text("Today's prompt", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(prompt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onWrite) { Text("Write this") }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
