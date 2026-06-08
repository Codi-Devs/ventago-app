package com.teco.ventago.features.auth.ui.register.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.auth.ui.register.user.viewmodel.RegisterUiEvent
import com.teco.ventago.features.auth.ui.register.user.viewmodel.RegisterViewModel
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.already_have_account
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.email_invalid
import ventago.composeapp.generated.resources.error_no_internet
import ventago.composeapp.generated.resources.error_try_later
import ventago.composeapp.generated.resources.login
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.password_too_short_login
import ventago.composeapp.generated.resources.prompt_password
import ventago.composeapp.generated.resources.register
import ventago.composeapp.generated.resources.register_terms_1
import ventago.composeapp.generated.resources.register_terms_2
import ventago.composeapp.generated.resources.register_terms_3
import ventago.composeapp.generated.resources.register_terms_4
import ventago.composeapp.generated.resources.welcome_register
import ventago.composeapp.generated.resources.welcome_register_desc
import ventago.composeapp.generated.resources.create_account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navigate: (PosScreens) -> Unit,
    goBack: () -> Unit
    ) {
    val viewModel: RegisterViewModel = koinViewModel<RegisterViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uriHandler = LocalUriHandler.current
    val snackbarService: SnackbarService = koinInject()
    val alreadyAccountAnnotated = buildAnnotatedString {
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W400,
            fontSize = 14.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.04.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        ) {
            append(stringResource(Res.string.already_have_account))
        }

        pushStringAnnotation(tag = "login", annotation = stringResource(Res.string.register))
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W600,
            fontSize = 16.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.04.sp,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary)
        ) {
            append(stringResource(Res.string.login))
        }
        pop()
    }
    val termsAnnotated = buildAnnotatedString {
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W400,
            fontSize = 12.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.04.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        ) {
            append(stringResource(Res.string.register_terms_1))
        }

        append("\n")

        pushStringAnnotation(tag = "register_terms_2", annotation = stringResource(Res.string.register_terms_2))
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W700,
            fontSize = 12.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.05.sp,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary)
        ) {
            append(stringResource(Res.string.register_terms_2))
        }
        pop()

        append(" ")

        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W400,
            fontSize = 12.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.04.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        ) {
            append(stringResource(Res.string.register_terms_3))
        }

        append(" ")

        pushStringAnnotation(tag = "register_terms_4", annotation = stringResource(Res.string.register_terms_4))
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W700,
            fontSize = 12.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.05.sp,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary)
        ) {
            append(stringResource(Res.string.register_terms_4))
        }
        pop()
    }
    val genericErrorMsg = stringResource(Res.string.error_no_internet)
    val tryLaterMsg = stringResource(Res.string.error_try_later)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterUiEvent.CanceledGoogleLogin -> Unit
                RegisterUiEvent.GenericError -> snackbarService.show(genericErrorMsg)
                RegisterUiEvent.MakingLoginError -> snackbarService.show(tryLaterMsg)
                RegisterUiEvent.TryLater -> snackbarService.show(tryLaterMsg)
                RegisterUiEvent.MissingBusiness -> navigate(PosScreens.BusinessRegisterScreen)
                else -> println("Unhandled event: $event")
            }
        }
    }


    Column (
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            modifier = Modifier.padding(bottom = 12.dp),
            text = stringResource(Res.string.welcome_register),
            style = TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(700),
                textAlign = TextAlign.Center,
            )
        )

        Text(
            modifier = Modifier.padding(bottom = 32.dp),
            text = stringResource(Res.string.welcome_register_desc),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(400),
                textAlign = TextAlign.Center,
                letterSpacing = 0.02.sp,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        DMOutlinedTextField(
            text = uiState.name,
            label = stringResource(Res.string.name),
            modifier = Modifier.padding(bottom = 4.dp),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            onChange = { newName ->
                viewModel.nameChanged(newName)
            },
            isError = uiState.invalidName,
        )

        DMOutlinedTextField(
            text = uiState.email,
            label = stringResource(Res.string.email),
            modifier = Modifier.padding(bottom = 4.dp),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onChange = { newEmail ->
                viewModel.emailChanged(newEmail)
            },
            isError = uiState.invalidEmail,
            supportingText = if (uiState.invalidEmail) stringResource(Res.string.email_invalid) else "",
        )

        DMOutlinedTextField(
            text = uiState.password,
            label = stringResource(Res.string.prompt_password),
            modifier = Modifier.padding(bottom = 16.dp),
            maxLines = 1,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            trailingIcon = if (uiState.showPassword)
                Icons.Filled.VisibilityOff else
                Icons.Filled.Visibility,
            trailingIconClick = {
                viewModel.passwordVisible()
            },
            visualTransformation = if (uiState.showPassword)
                VisualTransformation.None else
                PasswordVisualTransformation(),
            onChange = { newPass ->
                viewModel.passwordChanged(newPass)
            },
            isError = uiState.invalidPassword,
            supportingText = if (uiState.invalidPassword) stringResource(Res.string.password_too_short_login) else "",
        )

        ButtonM(
            modifier = Modifier.padding(bottom = 8.dp),
            onClick = {
                viewModel.emailRegister()
            }) {
            Text(
                text = stringResource(Res.string.create_account),
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.02.sp,
                )
            )
        }


        ClickableText(
            modifier = Modifier.padding(bottom = 32.dp, top = 16.dp),
            text = alreadyAccountAnnotated, onClick = {offset ->
                alreadyAccountAnnotated.getStringAnnotations(tag = "login", start = offset, end = offset).firstOrNull()?.let {
                    goBack()
                }
            })

        ClickableText(text = termsAnnotated,
            style = TextStyle(
                textAlign = TextAlign.Center
            ),
            onClick = { offset ->
                termsAnnotated.getStringAnnotations(tag = "register_terms_2", start = offset, end = offset).firstOrNull()?.let {
                    uriHandler.openUri("https://sites.google.com/view/ventago-terms")
                }

                termsAnnotated.getStringAnnotations(tag = "register_terms_4", start = offset, end = offset).firstOrNull()?.let {
                    uriHandler.openUri("https://sites.google.com/view/ventago-politicas-privacidad")
                }

            })
        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.hideLoading()
            }
        }
    }
}
