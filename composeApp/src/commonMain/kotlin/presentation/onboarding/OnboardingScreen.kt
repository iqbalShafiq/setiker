package presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.components.AppPrimaryButton
import presentation.components.PageIndicator
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.onboarding_ai_jobs
import setiker.composeapp.generated.resources.onboarding_ai_jobs_dark
import setiker.composeapp.generated.resources.onboarding_create_pack
import setiker.composeapp.generated.resources.onboarding_create_pack_dark
import setiker.composeapp.generated.resources.onboarding_done
import setiker.composeapp.generated.resources.onboarding_edit_polish
import setiker.composeapp.generated.resources.onboarding_edit_polish_dark
import setiker.composeapp.generated.resources.onboarding_next
import setiker.composeapp.generated.resources.onboarding_page1_desc
import setiker.composeapp.generated.resources.onboarding_page1_title
import setiker.composeapp.generated.resources.onboarding_page2_desc
import setiker.composeapp.generated.resources.onboarding_page2_title
import setiker.composeapp.generated.resources.onboarding_page3_desc
import setiker.composeapp.generated.resources.onboarding_page3_title
import setiker.composeapp.generated.resources.onboarding_skip

private val contentHorizontalPadding = 24.dp

@Composable
fun OnboardingScreenRoot(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToHome -> onFinished()
                is OnboardingEffect.ShowError -> Unit
            }
        }
    }
    OnboardingScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember { onboardingPages() }
    val pageCount = minOf(state.pageCount, pages.size)
    val pagerState = rememberPagerState(
        initialPage = state.pageIndex.coerceIn(0, pageCount - 1)
    ) { pageCount }
    val isLast = state.pageIndex >= state.pageCount - 1

    LaunchedEffect(state.pageIndex, pageCount) {
        val targetPage = state.pageIndex.coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                onIntent(OnboardingIntent.SelectPage(page))
            }
    }

    Scaffold(containerColor = neubrutalScreenBackground()) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = contentHorizontalPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { onIntent(OnboardingIntent.Skip) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = contentHorizontalPadding)
            ) {
                Text(stringResource(Res.string.onboarding_skip))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                OnboardingPageContent(page = pages[it])
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding)
                    .padding(bottom = contentHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPage = state.pageIndex,
                    onPageClick = { page -> onIntent(OnboardingIntent.SelectPage(page)) }
                )
                AppPrimaryButton(
                    text = stringResource(if (isLast) Res.string.onboarding_done else Res.string.onboarding_next),
                    onClick = { onIntent(if (isLast) OnboardingIntent.Finish else OnboardingIntent.Next) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(
                if (isSystemInDarkTheme()) page.darkImage else page.lightImage
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.34f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = contentHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(page.title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = neubrutalOnSurface(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(page.description),
                style = MaterialTheme.typography.bodyLarge,
                color = neubrutalSubtleOnSurface(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

private data class OnboardingPage(
    val title: StringResource,
    val description: StringResource,
    val lightImage: DrawableResource,
    val darkImage: DrawableResource
)

private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        title = Res.string.onboarding_page1_title,
        description = Res.string.onboarding_page1_desc,
        lightImage = Res.drawable.onboarding_create_pack,
        darkImage = Res.drawable.onboarding_create_pack_dark
    ),
    OnboardingPage(
        title = Res.string.onboarding_page2_title,
        description = Res.string.onboarding_page2_desc,
        lightImage = Res.drawable.onboarding_edit_polish,
        darkImage = Res.drawable.onboarding_edit_polish_dark
    ),
    OnboardingPage(
        title = Res.string.onboarding_page3_title,
        description = Res.string.onboarding_page3_desc,
        lightImage = Res.drawable.onboarding_ai_jobs,
        darkImage = Res.drawable.onboarding_ai_jobs_dark
    )
)
