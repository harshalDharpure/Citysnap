package com.prod.singles_date.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import com.prod.singles_date.data.LocalPreferences
import com.prod.singles_date.messaging.PendingNotification
import com.prod.singles_date.model.ThemeMode
import com.prod.singles_date.ui.screens.BlockedUsersScreen
import com.prod.singles_date.ui.screens.ChatScreen
import com.prod.singles_date.ui.screens.CitySelectScreen
import com.prod.singles_date.ui.screens.FeedScreen
import com.prod.singles_date.ui.screens.LegalPage
import com.prod.singles_date.ui.screens.LegalScreen
import com.prod.singles_date.ui.screens.LoginScreen
import com.prod.singles_date.ui.screens.MessagesScreen
import com.prod.singles_date.ui.screens.NotificationInboxScreen
import com.prod.singles_date.ui.screens.NotificationSettingsScreen
import com.prod.singles_date.ui.screens.PostDetailScreen
import com.prod.singles_date.ui.screens.PremiumScreen
import com.prod.singles_date.ui.screens.ProfileScreen
import com.prod.singles_date.ui.screens.PublicUserProfileScreen
import com.prod.singles_date.ui.screens.SignupScreen
import com.prod.singles_date.ui.screens.SponsorLeadScreen
import com.prod.singles_date.ui.screens.SplashScreen
import com.prod.singles_date.ui.screens.WelcomeScreen
import com.prod.singles_date.util.AppLinks
import com.prod.singles_date.viewmodel.AuthViewModel
import com.prod.singles_date.viewmodel.ChatViewModel
import com.prod.singles_date.viewmodel.ProfileViewModel
import com.prod.singles_date.viewmodel.SessionState
import com.prod.singles_date.viewmodel.ThoughtViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    pendingDeepLink: AppLinks.DeepLink? = null,
    onDeepLinkHandled: () -> Unit = {},
    pendingNotification: PendingNotification? = null,
    onNotificationHandled: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val owner = LocalContext.current as ComponentActivity
    val context = LocalContext.current
    val authViewModel: AuthViewModel = hiltViewModel(owner)
    val thoughtViewModel: ThoughtViewModel = hiltViewModel(owner)
    val chatViewModel: ChatViewModel = hiltViewModel(owner)
    val profileViewModel: ProfileViewModel = hiltViewModel(owner)
    val prefs = remember { LocalPreferences(context) }

    val firebaseUser by authViewModel.firebaseUser.collectAsStateWithLifecycle()

    LaunchedEffect(firebaseUser?.uid) {
        thoughtViewModel.setCurrentUid(firebaseUser?.uid)
        chatViewModel.setCurrentUid(firebaseUser?.uid)
        firebaseUser?.uid?.let { authViewModel.registerPushToken(it) }
    }

    val messagesUnreadCount by chatViewModel.totalUnreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        chatViewModel.error.collect { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                chatViewModel.clearError()
            }
        }
    }

    LaunchedEffect(Unit) {
        thoughtViewModel.uiEvents.collect { event ->
            val text = when (event) {
                is ThoughtViewModel.UiEvent.Message -> event.text
                is ThoughtViewModel.UiEvent.Error -> event.text
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var pendingExit by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        pendingExit = false
    }

    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }

    BackHandler(
        enabled = navController.previousBackStackEntry == null &&
            (currentRoute == Routes.Feed || currentRoute == Routes.Welcome),
    ) {
        if (pendingExit) {
            owner.finish()
        } else {
            pendingExit = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val pendingOpenPostId by thoughtViewModel.pendingOpenPostId.collectAsStateWithLifecycle()

    LaunchedEffect(pendingOpenPostId) {
        val thoughtId = pendingOpenPostId ?: return@LaunchedEffect
        val route = navController.currentDestination?.route
        if (route == Routes.Splash || route == Routes.Welcome || route == Routes.Login) return@LaunchedEffect
        navController.navigate(Routes.postDetail(thoughtId))
        thoughtViewModel.clearPendingOpenPostId()
    }

    LaunchedEffect(pendingNotification) {
        val n = pendingNotification ?: return@LaunchedEffect
        when {
            n.isMessage -> navController.navigate(Routes.chat(n.conversationId, n.senderId))
            n.isDailyPrompt -> thoughtViewModel.setPendingPostPrompt(n.promptText)
            n.isThoughtDeepLink -> thoughtViewModel.setPendingOpenPostId(n.thoughtId)
            n.type == "locality_topic" && n.promptText.isNotBlank() ->
                thoughtViewModel.setPendingPostPrompt(n.promptText)
        }
        onNotificationHandled()
    }

    LaunchedEffect(pendingDeepLink) {
        when (val link = pendingDeepLink) {
            null -> Unit
            is AppLinks.DeepLink.Thought -> {
                thoughtViewModel.setPendingOpenPostId(link.thoughtId)
                onDeepLinkHandled()
            }
            is AppLinks.DeepLink.Invite -> {
                prefs.setPendingReferralCode(link.referralCode)
                authViewModel.setPendingReferralCode(link.referralCode)
                onDeepLinkHandled()
            }
        }
    }

    fun needsOnboarding(city: String, locality: String): Boolean {
        return city.isBlank()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        modifier = modifier,
    ) {
        composable(Routes.Splash) {
            val session = authViewModel.session.collectAsStateWithLifecycle().value

            SplashScreen()

            LaunchedEffect(session) {
                when (session) {
                    SessionState.Loading -> Unit
                    is SessionState.LoggedIn -> {
                        val city = session.userProfile?.city.orEmpty()
                        val locality = session.userProfile?.locality.orEmpty()
                        val destination = if (needsOnboarding(city, locality)) {
                            Routes.CitySelect
                        } else {
                            Routes.Feed
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                    SessionState.LoggedOut -> {
                        val guestCity = prefs.getGuestCity()
                        val guestLocality = prefs.getGuestLocality()
                        val destination = when {
                            needsOnboarding(guestCity, guestLocality) -> Routes.Welcome
                            else -> Routes.Feed
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(Routes.Welcome) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value

            LaunchedEffect(isLoggedIn, profile) {
                if (!isLoggedIn) return@LaunchedEffect
                val city = profile?.city.orEmpty()
                val locality = profile?.locality.orEmpty()
                val destination = if (needsOnboarding(city, locality)) Routes.CitySelect else Routes.Feed
                navController.navigate(destination) {
                    popUpTo(Routes.Welcome) { inclusive = true }
                }
            }

            WelcomeScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(Routes.Login) },
                onNavigateToSignUp = { navController.navigate(Routes.Signup) },
                onNavigateToCityBrowse = { navController.navigate(Routes.CitySelect) },
            )
        }

        composable(Routes.Login) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value

            LaunchedEffect(isLoggedIn, profile) {
                if (!isLoggedIn) return@LaunchedEffect
                val city = profile?.city.orEmpty()
                val locality = profile?.locality.orEmpty()
                val destination = if (needsOnboarding(city, locality)) Routes.CitySelect else Routes.Feed
                if (navController.previousBackStackEntry != null) {
                    if (destination == Routes.CitySelect) {
                        navController.navigate(Routes.CitySelect) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                } else {
                    navController.navigate(destination) {
                        popUpTo(Routes.Welcome) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Routes.Signup) },
                onLoggedIn = { /* handled by LaunchedEffect above */ },
            )
        }

        composable(Routes.Signup) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value

            LaunchedEffect(isLoggedIn, profile) {
                if (!isLoggedIn) return@LaunchedEffect
                navController.navigate(Routes.CitySelect) {
                    popUpTo(Routes.Welcome) { inclusive = true }
                }
            }

            SignupScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignedUp = { /* handled by LaunchedEffect above */ },
            )
        }

        composable(Routes.CitySelect) {
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value
            val authMessage by authViewModel.authMessage.collectAsStateWithLifecycle()
            var isSavingOnboarding by remember { mutableStateOf(false) }

            LaunchedEffect(authMessage) {
                if (authMessage.isNotBlank()) {
                    Toast.makeText(context, authMessage, Toast.LENGTH_LONG).show()
                }
            }

            CitySelectScreen(
                isSaving = isSavingOnboarding,
                onContinue = { city, locality ->
                    prefs.saveGuestOnboarding(city, locality)
                    thoughtViewModel.setSelectedCity(city)
                    thoughtViewModel.setSelectedLocality(locality)
                    if (isLoggedIn) {
                        isSavingOnboarding = true
                        authViewModel.saveOnboarding(city, locality) { success ->
                            isSavingOnboarding = false
                            if (success) {
                                navController.navigate(Routes.Feed) {
                                    popUpTo(Routes.CitySelect) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        navController.navigate(Routes.Feed) {
                            popUpTo(Routes.CitySelect) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.Feed) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value

            LaunchedEffect(isLoggedIn, profile?.city, profile?.locality) {
                if (!isLoggedIn) return@LaunchedEffect
                // Prefer Firestore profile; fall back to prefs saved during onboarding
                // so we don't bounce back before the profile listener catches up.
                val city = profile?.city?.takeIf { it.isNotBlank() } ?: prefs.getGuestCity()
                val locality = profile?.locality?.takeIf { it.isNotBlank() } ?: prefs.getGuestLocality()
                if (needsOnboarding(city, locality)) {
                    navController.navigate(Routes.CitySelect)
                }
            }

            LaunchedEffect(isLoggedIn, profile?.city, profile?.locality) {
                val city = profile?.city?.takeIf { it.isNotBlank() } ?: prefs.getGuestCity()
                val locality = profile?.locality?.takeIf { it.isNotBlank() } ?: prefs.getGuestLocality()
                if (city.isNotBlank()) thoughtViewModel.setSelectedCity(city)
                thoughtViewModel.setSelectedLocality(locality)
            }

            LaunchedEffect(Unit) {
                val city = prefs.getFeedCityFilter()
                val locality = prefs.getFeedLocalityFilter()
                thoughtViewModel.setFeedCityFilter(city)
                if (city.isNotBlank() && locality.isNotBlank()) {
                    thoughtViewModel.setFeedLocalityFilter(locality)
                }
            }

            FeedScreen(
                authViewModel = authViewModel,
                thoughtViewModel = thoughtViewModel,
                onOpenProfile = {
                    if (isLoggedIn) navController.navigate(Routes.Profile)
                    else navController.navigate(Routes.Login)
                },
                onRequireLogin = { navController.navigate(Routes.Login) },
                onChangeCity = { navController.navigate(Routes.CitySelect) },
                onOpenPost = { thoughtId -> navController.navigate(Routes.postDetail(thoughtId)) },
                onOpenUserProfile = { uid -> navController.navigate(Routes.userProfile(uid)) },
                onOpenNotifications = { navController.navigate(Routes.NotificationInbox) },
                onOpenMessages = { navController.navigate(Routes.Messages) },
                messagesUnreadCount = messagesUnreadCount,
            )
        }

        composable(Routes.Messages) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value
            val activeCity = profile?.city?.takeIf { it.isNotBlank() } ?: prefs.getGuestCity()
            MessagesScreen(
                chatViewModel = chatViewModel,
                currentUser = profile,
                isLoggedIn = isLoggedIn,
                activeCity = activeCity,
                onOpenChat = { conversationId, otherUid ->
                    navController.navigate(Routes.chat(conversationId, otherUid))
                },
                onOpenProfile = {
                    if (isLoggedIn) navController.navigate(Routes.Profile)
                    else navController.navigate(Routes.Login)
                },
                onOpenFeed = {
                    navController.popBackStack(Routes.Feed, inclusive = false)
                },
                onChangeCity = { navController.navigate(Routes.CitySelect) },
                onOpenSnap = {
                    thoughtViewModel.requestOpenComposer()
                    navController.popBackStack(Routes.Feed, inclusive = false)
                },
                onRequireLogin = { navController.navigate(Routes.Login) },
            )
        }

        composable(
            route = Routes.Chat,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("otherUid") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
            val otherUid = backStackEntry.arguments?.getString("otherUid").orEmpty()
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            ChatScreen(
                conversationId = conversationId,
                otherUid = otherUid,
                currentUid = fu?.uid,
                chatViewModel = chatViewModel,
                thoughtViewModel = thoughtViewModel,
                onBack = {
                    chatViewModel.clearActiveChat()
                    navController.popBackStack()
                },
                onRequireLogin = { navController.navigate(Routes.Login) },
            )
        }

        composable(
            route = Routes.PostDetail,
            arguments = listOf(navArgument("thoughtId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val thoughtId = backStackEntry.arguments?.getString("thoughtId").orEmpty()
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn = authViewModel.isLoggedIn.collectAsStateWithLifecycle().value
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            val activeCity = profile?.city?.takeIf { it.isNotBlank() } ?: prefs.getGuestCity()
            val activeLocality = profile?.locality?.takeIf { it.isNotBlank() } ?: prefs.getGuestLocality()
            val user = if (fu != null) {
                profile ?: com.prod.singles_date.model.User(
                    uid = fu.uid,
                    name = fu.email.orEmpty().substringBefore('@').ifBlank { "You" },
                    email = fu.email.orEmpty(),
                )
            } else null

            PostDetailScreen(
                thoughtId = thoughtId,
                thoughtViewModel = thoughtViewModel,
                currentUser = user,
                currentUid = fu?.uid,
                isLoggedIn = isLoggedIn,
                activeCity = activeCity,
                activeLocality = activeLocality,
                onBack = { navController.popBackStack() },
                onRequireLogin = { navController.navigate(Routes.Login) },
                onMessageAuthor = if (isLoggedIn && fu != null && user != null) {
                    { authorId ->
                        chatViewModel.openChatWith(authorId) { conversationId ->
                            navController.navigate(Routes.chat(conversationId, authorId))
                        }
                    }
                } else null,
            )
        }

        composable(Routes.BlockedUsers) {
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            if (fu == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                BlockedUsersScreen(
                    thoughtViewModel = thoughtViewModel,
                    currentUid = fu.uid,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.NotificationSettings) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            NotificationSettingsScreen(
                onBack = { navController.popBackStack() },
                uid = fu?.uid,
                serverNotifyFeels = profile?.notifyFeels,
                serverNotifyComments = profile?.notifyComments,
                serverNotifyPrompts = profile?.notifyPrompts,
                serverNotifyMessages = profile?.notifyMessages,
                onUpdatePrefs = { feels, comments, prompts, messages ->
                    authViewModel.updateNotificationPrefs(feels, comments, prompts, messages)
                },
            )
        }

        composable(Routes.NotificationInbox) {
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            if (fu == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                NotificationInboxScreen(
                    thoughtViewModel = thoughtViewModel,
                    currentUid = fu.uid,
                    onBack = { navController.popBackStack() },
                    onOpenPost = { id -> navController.navigate(Routes.postDetail(id)) },
                )
            }
        }

        composable(Routes.Premium) {
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            PremiumScreen(
                isPremium = profile?.isPremium == true,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SponsorLead) {
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            val profile by authViewModel.currentUser.collectAsStateWithLifecycle()
            if (fu == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                SponsorLeadScreen(
                    defaultCity = profile?.city?.ifBlank { prefs.getGuestCity() }.orEmpty(),
                    currentUid = fu.uid,
                    userEmail = profile?.email ?: fu.email.orEmpty(),
                    onSubmit = { business, email, city, budget, message, onDone ->
                        thoughtViewModel.submitSponsorLead(fu.uid, business, email, city, budget, message, onDone)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = Routes.UserProfile,
            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid").orEmpty()
            val fu = authViewModel.firebaseUser.collectAsStateWithLifecycle().value
            var showMessageButton by remember(uid, fu?.uid) { mutableStateOf(false) }
            LaunchedEffect(uid, fu?.uid) {
                showMessageButton = fu != null && uid != fu.uid && chatViewModel.canMessage(uid)
            }
            PublicUserProfileScreen(
                authorUid = uid,
                currentUid = fu?.uid,
                thoughtViewModel = thoughtViewModel,
                showMessageButton = showMessageButton,
                onMessage = {
                    chatViewModel.openChatWith(uid) { conversationId ->
                        navController.navigate(Routes.chat(conversationId, uid))
                    }
                },
                onBack = { navController.popBackStack() },
                onOpenPost = { id -> navController.navigate(Routes.postDetail(id)) },
                onRequireLogin = { navController.navigate(Routes.Login) },
            )
        }

        composable(Routes.LegalGuidelines) {
            LegalScreen(page = LegalPage.Guidelines, onBack = { navController.popBackStack() })
        }

        composable(Routes.LegalPrivacy) {
            LegalScreen(page = LegalPage.Privacy, onBack = { navController.popBackStack() })
        }

        composable(Routes.Profile) {
            ProfileScreen(
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    val guestCity = prefs.getGuestCity()
                    val guestLocality = prefs.getGuestLocality()
                    val destination = if (needsOnboarding(guestCity, guestLocality)) {
                        Routes.Welcome
                    } else {
                        Routes.Feed
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.Feed) { inclusive = true }
                    }
                },
                onChangeCity = { navController.navigate(Routes.CitySelect) },
                onOpenBlockedUsers = { navController.navigate(Routes.BlockedUsers) },
                onOpenNotificationSettings = { navController.navigate(Routes.NotificationSettings) },
                onOpenNotificationInbox = { navController.navigate(Routes.NotificationInbox) },
                onOpenPremium = { navController.navigate(Routes.Premium) },
                onOpenSponsorLead = { navController.navigate(Routes.SponsorLead) },
                onOpenGuidelines = { navController.navigate(Routes.LegalGuidelines) },
                onOpenPrivacy = { navController.navigate(Routes.LegalPrivacy) },
                onSnap = {
                    thoughtViewModel.requestOpenComposer()
                    navController.popBackStack()
                },
                onNavigateHome = { navController.popBackStack() },
                onOpenMessages = { navController.navigate(Routes.Messages) },
                messagesUnreadCount = messagesUnreadCount,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
        }
    }
}
