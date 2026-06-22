package presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.model.User
import domain.model.UserRole
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AiQuotaSummary
import presentation.components.AppIllustration
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.AppTopBar
import presentation.components.ProfileMenuItem
import presentation.components.ProfileBottomBar
import presentation.theme.AccentCoral
import presentation.theme.AccentCoralLight
import presentation.theme.NeubrutalWhite
import presentation.theme.PastelBlue
import presentation.theme.PastelMint
import presentation.theme.PastelPink
import presentation.theme.PastelPurple
import presentation.theme.PastelYellow
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalSubtleOnSurface
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.about_app
import setiker.composeapp.generated.resources.profile_ai_quota_section
import setiker.composeapp.generated.resources.account_section
import setiker.composeapp.generated.resources.downloaded_packs
import setiker.composeapp.generated.resources.downloads_label
import setiker.composeapp.generated.resources.following
import setiker.composeapp.generated.resources.help_center
import setiker.composeapp.generated.resources.liked_stickers
import setiker.composeapp.generated.resources.login_welcome_subtitle
import setiker.composeapp.generated.resources.logout
import setiker.composeapp.generated.resources.my_profile_title
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.packs_label
import setiker.composeapp.generated.resources.premium_badge
import setiker.composeapp.generated.resources.profile_ai_jobs_menu
import setiker.composeapp.generated.resources.profile_explore_packs
import setiker.composeapp.generated.resources.notifications_title
import setiker.composeapp.generated.resources.profile_guest_login_prompt
import setiker.composeapp.generated.resources.profile_processing_history
import setiker.composeapp.generated.resources.send_feedback
import setiker.composeapp.generated.resources.settings
import setiker.composeapp.generated.resources.settings_legal_section
import setiker.composeapp.generated.resources.settings_privacy
import setiker.composeapp.generated.resources.settings_retention
import setiker.composeapp.generated.resources.settings_terms
import setiker.composeapp.generated.resources.stickers_label
import setiker.composeapp.generated.resources.support_section
import util.rememberUrlLauncher

@Composable
fun ProfileScreenRoot(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateExplore: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateAiJobs: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val openUrl = rememberUrlLauncher()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    ProfileScreen(
        state = state,
        onLogout = {
            viewModel.logout()
            onLogout()
        },
        onNavigateHome = onNavigateHome,
        onNavigateExplore = onNavigateExplore,
        onNavigateHistory = onNavigateHistory,
        onNavigateAiJobs = onNavigateAiJobs,
        onNavigateNotifications = onNavigateNotifications,
        onOpenPrivacy = { state.legalSummary?.privacyUrl?.let(openUrl) },
        onOpenTerms = { state.legalSummary?.termsUrl?.let(openUrl) },
        onOpenRetention = {
            state.legalSummary?.let { summary ->
                openUrl(summary.retentionUrl ?: summary.privacyUrl)
            }
        },
        onBackClick = onBackClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    onLogout: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateExplore: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateAiJobs: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenRetention: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = neubrutalBorderColor()
    val shadowColor = neubrutalShadowColor()
    val cardSurface = neubrutalCardSurface()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.my_profile_title),
                onBackClick = null
            )
        },
        bottomBar = {
            ProfileBottomBar(
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogout
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                illustration = AppIllustration.AuthCloud
            )
        } else if (state.user == null) {
            EmptyState(
                title = stringResource(Res.string.profile_guest_login_prompt),
                description = stringResource(Res.string.login_welcome_subtitle),
                illustration = AppIllustration.AuthCloud,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Profile Card
                item {
                    ProfileCard(
                        user = state.user,
                        borderColor = borderColor,
                        shadowColor = shadowColor,
                        cardSurface = cardSurface,
                        onClick = onSettingsClick
                    )
                }

                // Stats Card
                item {
                    StatsCard(
                        stickersCount = state.stickersCount,
                        packsCount = state.packsCount,
                        downloadsCount = state.downloadsCount,
                        borderColor = borderColor,
                        shadowColor = shadowColor,
                        cardSurface = cardSurface
                    )
                }

                item {
                    Column {
                        Text(
                            text = stringResource(Res.string.profile_ai_quota_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = neubrutalOnSurface(),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        AiQuotaSummary(
                            usage = state.aiUsage,
                            isLoading = state.isLoadingAiUsage,
                            hasError = state.aiUsageLoadFailed,
                            operationCostsInInfoDialog = true
                        )
                    }
                }

                // Account Section
                item {
                    Column {
                        SectionTitle(text = stringResource(Res.string.account_section))
                        Spacer(modifier = Modifier.height(8.dp))
                        MenuCard(borderColor = borderColor, shadowColor = shadowColor, cardSurface = cardSurface) {
                            ProfileMenuItem(
                                icon = Icons.Default.Image,
                                label = stringResource(Res.string.my_stickers_title),
                                iconBackgroundColor = PastelYellow,
                                onClick = onNavigateHome
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Favorite,
                                label = stringResource(Res.string.profile_explore_packs),
                                iconBackgroundColor = PastelPink,
                                onClick = onNavigateExplore
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Download,
                                label = stringResource(Res.string.profile_processing_history),
                                iconBackgroundColor = PastelBlue,
                                onClick = onNavigateHistory
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Star,
                                label = stringResource(Res.string.profile_ai_jobs_menu),
                                iconBackgroundColor = PastelPurple,
                                onClick = onNavigateAiJobs
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Notifications,
                                label = if (state.notificationUnreadCount > 0) {
                                    "${stringResource(Res.string.notifications_title)} (${state.notificationUnreadCount})"
                                } else {
                                    stringResource(Res.string.notifications_title)
                                },
                                iconBackgroundColor = PastelYellow,
                                onClick = onNavigateNotifications
                            )
                        }
                    }
                }

                item {
                    Column {
                        SectionTitle(text = stringResource(Res.string.settings_legal_section))
                        Spacer(modifier = Modifier.height(8.dp))
                        MenuCard(borderColor = borderColor, shadowColor = shadowColor, cardSurface = cardSurface) {
                            ProfileMenuItem(
                                icon = Icons.Default.Description,
                                label = stringResource(Res.string.settings_privacy),
                                iconBackgroundColor = PastelMint,
                                onClick = onOpenPrivacy
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Info,
                                label = stringResource(Res.string.settings_terms),
                                iconBackgroundColor = PastelYellow,
                                onClick = onOpenTerms
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Storage,
                                label = stringResource(Res.string.settings_retention),
                                iconBackgroundColor = PastelBlue,
                                onClick = onOpenRetention
                            )
                        }
                    }
                }

                // Support Section
                item {
                    Column {
                        SectionTitle(text = stringResource(Res.string.support_section))
                        Spacer(modifier = Modifier.height(8.dp))
                        MenuCard(borderColor = borderColor, shadowColor = shadowColor, cardSurface = cardSurface) {
                            ProfileMenuItem(
                                icon = Icons.Default.Help,
                                label = stringResource(Res.string.help_center),
                                iconBackgroundColor = PastelMint,
                                onClick = {}
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Feedback,
                                label = stringResource(Res.string.send_feedback),
                                iconBackgroundColor = PastelYellow,
                                onClick = {}
                            )
                            MenuDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Info,
                                label = stringResource(Res.string.about_app),
                                iconBackgroundColor = PastelBlue,
                                onClick = {}
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    user: User,
    borderColor: androidx.compose.ui.graphics.Color,
    shadowColor: androidx.compose.ui.graphics.Color,
    cardSurface: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(offsetX = NeubrutalShadowOffset, offsetY = NeubrutalShadowOffset, cornerRadius = NeubrutalCardRadius, color = shadowColor)
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(cardSurface)
            .neubrutalBorderWithGloss(
                color = borderColor,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AccentCoralLight)
                .neubrutalBorderWithGloss(
                    color = borderColor,
                    cornerRadius = 32.dp,
                    shape = CircleShape,
                    highlightColor = neubrutalGlossyHighlightColor()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = AccentCoral
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name ?: user.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AccentCoral
                )
            }
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalMutedOnSurface()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentCoral)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(Res.string.premium_badge),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeubrutalWhite
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = neubrutalMutedOnSurface()
        )
    }
}

@Composable
private fun StatsCard(
    stickersCount: Int,
    packsCount: Int,
    downloadsCount: Int,
    borderColor: androidx.compose.ui.graphics.Color,
    shadowColor: androidx.compose.ui.graphics.Color,
    cardSurface: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(offsetX = NeubrutalShadowOffset, offsetY = NeubrutalShadowOffset, cornerRadius = NeubrutalCardRadius, color = shadowColor)
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(cardSurface)
            .neubrutalBorderWithGloss(
                color = borderColor,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            icon = Icons.Default.Image,
            count = formatCount(stickersCount),
            label = stringResource(Res.string.stickers_label)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(neubrutalSubtleOnSurface())
        )
        StatItem(
            icon = Icons.Default.Star,
            count = formatCount(packsCount),
            label = stringResource(Res.string.packs_label)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(neubrutalSubtleOnSurface())
        )
        StatItem(
            icon = Icons.Default.Download,
            count = formatCount(downloadsCount),
            label = stringResource(Res.string.downloads_label)
        )
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = neubrutalOnSurface()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = neubrutalMutedOnSurface()
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = neubrutalMutedOnSurface(),
        letterSpacing = 1.sp
    )
}

@Composable
private fun MenuCard(
    borderColor: androidx.compose.ui.graphics.Color,
    shadowColor: androidx.compose.ui.graphics.Color,
    cardSurface: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neubrutalShadow(offsetX = NeubrutalShadowOffset, offsetY = NeubrutalShadowOffset, cornerRadius = NeubrutalCardRadius, color = shadowColor)
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(cardSurface)
            .neubrutalBorderWithGloss(
                color = borderColor,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
    ) {
        content()
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(neubrutalSubtleOnSurface().copy(alpha = 0.3f))
    )
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K"
        else -> count.toString()
    }
}

// MARK: - Previews

@Preview
@Composable
private fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileState(
                user = User(
                    id = "1",
                    username = "catlover",
                    email = "cat@example.com",
                    name = "Cat Lover",
                    role = UserRole(id = "1", name = "user"),
                    isActive = true,
                    createdAt = 0L
                ),
                isLoading = false,
                stickersCount = 1248,
                packsCount = 86,
                downloadsCount = 3200
            ),
            onLogout = {}
        )
    }
}

@Preview
@Composable
private fun ProfileScreenLoadingPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileState(isLoading = true),
            onLogout = {}
        )
    }
}

@Preview
@Composable
private fun ProfileScreenEmptyPreview() {
    MaterialTheme {
        ProfileScreen(
            state = ProfileState(user = null, isLoading = false),
            onLogout = {}
        )
    }
}
