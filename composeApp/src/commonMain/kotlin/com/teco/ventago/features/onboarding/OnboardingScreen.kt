package com.teco.ventago.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.StepperIndicator
import com.teco.ventago.design_system.theme.headlineLargeBold
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.titleSmall
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.create_your_account
import ventago.composeapp.generated.resources.next
import ventago.composeapp.generated.resources.onboarding_1
import ventago.composeapp.generated.resources.onboarding_2
import ventago.composeapp.generated.resources.onboarding_3
import ventago.composeapp.generated.resources.register
import ventago.composeapp.generated.resources.skip
import ventago.composeapp.generated.resources.step_one_subtittle
import ventago.composeapp.generated.resources.step_one_tittle
import ventago.composeapp.generated.resources.step_three_subtittle
import ventago.composeapp.generated.resources.step_three_tittle
import ventago.composeapp.generated.resources.step_two_subtittle
import ventago.composeapp.generated.resources.step_two_tittle

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onLogin: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = stringResource(Res.string.step_one_tittle),
            description = stringResource(Res.string.step_one_subtittle),
            imageRes = Res.drawable.onboarding_1,
            buttonText = stringResource(Res.string.next),
            isLast = false
        ),
        OnboardingPage(
            title = stringResource(Res.string.step_two_tittle),
            description = stringResource(Res.string.step_two_subtittle),
            imageRes = Res.drawable.onboarding_2,
            buttonText = stringResource(Res.string.next),
            isLast = false
        ),
        OnboardingPage(
            title = stringResource(Res.string.step_three_tittle),
            description = stringResource(Res.string.step_three_subtittle),
            imageRes = Res.drawable.onboarding_3,
            buttonText = stringResource(Res.string.register),
            isLast = true
        )
    )

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { 3 }
    )

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF006C44),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF006C44))
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperIndicator(
                    totalSteps = pagerState.pageCount,
                    currentStep = pagerState.currentPage,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (pagerState.currentPage != pages.lastIndex) {
                    TextButtonS(
                        label = stringResource(Res.string.skip),
                        color = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        onFinish()
                    }
                }

            }


            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val current = pages[page]
                OnboardingPageView(page = current)
            }


            ButtonM(
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinish()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                containerColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.lastIndex) stringResource(Res.string.create_your_account) else pages[pagerState.currentPage].buttonText,
                    style = labelLarge().copy(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }


}

@Composable
fun OnboardingPageView(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = page.title,
            style = headlineLargeBold(),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.description,
            style = titleSmall(),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            contentScale = ContentScale.FillWidth, // scale proportionally to width
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: DrawableResource,
    val buttonText: String,
    val isLast: Boolean
)