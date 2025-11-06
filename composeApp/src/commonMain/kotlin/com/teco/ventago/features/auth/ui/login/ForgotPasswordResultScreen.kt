package com.teco.ventago.features.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.buttons.TextButtonM
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.auth.ui.login.viewmodel.LoginViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.rememberPlatformState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.check_email_inbox
import ventago.composeapp.generated.resources.dont_receive_email
import ventago.composeapp.generated.resources.email_sent_desc
import ventago.composeapp.generated.resources.for_help_contact_support_team
import ventago.composeapp.generated.resources.resend_email
import ventago.composeapp.generated.resources.support_team_email

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordResultScreen(navigate: (PosScreens) -> Unit) {
    val viewModel: LoginViewModel = koinViewModel<LoginViewModel>()
    val platformState = rememberPlatformState()
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W400,
            fontSize = 14.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.04.sp,
        )
        ) {
            append(stringResource(Res.string.for_help_contact_support_team))
        }

        append("\n")

        pushStringAnnotation(tag = "support_email", annotation = stringResource(Res.string.support_team_email))
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W700,
            fontSize = 12.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.05.sp,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary)
        ) {
            append(stringResource(Res.string.support_team_email))
        }
        pop()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Column (
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(Res.string.check_email_inbox),
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W700,
                    textAlign = TextAlign.Start,
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.email_sent_desc),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W500,
                    textAlign = TextAlign.Start,
                    letterSpacing = 0.02.sp,
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier = Modifier,
            text = stringResource(Res.string.dont_receive_email),
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight.W700,
                textAlign = TextAlign.Center,
                letterSpacing = 0.02.sp,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButtonM(
            label = stringResource(Res.string.resend_email),
            color = MaterialTheme.colorScheme.primary,
        ) {
            viewModel.forgotPassword()
        }

        ClickableText(
            modifier = Modifier.padding(bottom = 32.dp, top = 8.dp),
            text = annotatedString,
            style = TextStyle(
                textAlign = TextAlign.Center),
            onClick = {offset ->
                annotatedString.getStringAnnotations(tag = "support_email", start = offset, end = offset).firstOrNull()?.let {
                    platformState.openEmailIntent("soporte@tecodigi.com")
                }
            })
    }
}
