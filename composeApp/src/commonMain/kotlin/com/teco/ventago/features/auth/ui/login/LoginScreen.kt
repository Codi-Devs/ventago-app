package com.teco.ventago.features.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.SocialButton
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.DMDivider
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.auth.ui.login.viewmodel.LoginUiEvent
import com.teco.ventago.features.auth.ui.login.viewmodel.LoginViewModel
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Forgot_password_ask
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.cancelled_google_login
import ventago.composeapp.generated.resources.dont_have_account_ask
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.email_invalid
import ventago.composeapp.generated.resources.email_not_found
import ventago.composeapp.generated.resources.error_no_internet
import ventago.composeapp.generated.resources.error_try_later
import ventago.composeapp.generated.resources.go_register
import ventago.composeapp.generated.resources.google
import ventago.composeapp.generated.resources.google_social_media
import ventago.composeapp.generated.resources.invalid_sign_in
import ventago.composeapp.generated.resources.login
import ventago.composeapp.generated.resources.or
import ventago.composeapp.generated.resources.password_too_short_login
import ventago.composeapp.generated.resources.prompt_password
import ventago.composeapp.generated.resources.register
import ventago.composeapp.generated.resources.welcome
import ventago.composeapp.generated.resources.welcome_back_sub


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navigate: (PosScreens) -> Unit) {
    val viewModel: LoginViewModel = koinViewModel<LoginViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    val snackbarHostState = remember { SnackbarHostState() }

    val showRegisterDialog = mutableStateOf(false)

    val cancelledGoogleLoginMsg = stringResource(Res.string.cancelled_google_login)
    val invalidSignInMsg = stringResource(Res.string.invalid_sign_in)
    val genericErrorMsg = stringResource(Res.string.error_no_internet)
    val tryLaterMsg = stringResource(Res.string.error_try_later)
    LaunchedEffect(Unit) {

        viewModel.events.collect { event ->
            when (event) {
                LoginUiEvent.CanceledGoogleLogin -> snackbarHostState.showSnackbar(cancelledGoogleLoginMsg)
                LoginUiEvent.GenericError -> snackbarHostState.showSnackbar(genericErrorMsg)
                LoginUiEvent.MakingLoginError -> snackbarHostState.showSnackbar(invalidSignInMsg)
                LoginUiEvent.TryLater -> snackbarHostState.showSnackbar(tryLaterMsg)
                LoginUiEvent.MissingBusiness -> navigate(PosScreens.BusinessRegisterScreen)
                LoginUiEvent.ShowRegisterDialog -> {
                    showRegisterDialog.value = true
                }
                else -> println("Unhandled event: $event")
            }
        }
    }

    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.02.sp,
            color = MaterialTheme.colorScheme.onSurface
            )
        ) {
            append(stringResource(Res.string.dont_have_account_ask) + "")
        }

        pushStringAnnotation(tag = "register", annotation = stringResource(Res.string.register))
        withStyle(style = SpanStyle(
            fontWeight = FontWeight.W600,
            fontSize = 16.sp,
            fontFamily = latoFontFamily(),
            letterSpacing = 0.04.sp,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary)
        ) {
            append(" ${stringResource(Res.string.register)}")
        }
        pop()
    }
    
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 32.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(Res.string.welcome),
                style = TextStyle(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight(700),
                    textAlign = TextAlign.Center,
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.welcome_back_sub),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight(400),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.02.sp
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            SocialButton(
                stringResource(Res.string.google),
                icon = painterResource(Res.drawable.google_social_media),
                onClick = { viewModel.launchGoogleLogin() },
            )

            Spacer(modifier = Modifier.height(32.dp))

            DMDivider(
                label = stringResource(Res.string.or)
            )

            Spacer(modifier = Modifier.height(32.dp))

            DMOutlinedTextField(
                text = uiState.email,
                label = stringResource(Res.string.email),
                modifier = Modifier.padding(bottom = 8.dp),
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
                modifier = Modifier.padding(bottom = 0.dp),
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

            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End) {

                TextButtonS(
                    modifier = Modifier.padding(0.dp),
                    label = stringResource(Res.string.Forgot_password_ask),
                    overrideContentPadding = true,
                ) {
                    navigate(PosScreens.ForgotPasswordScreen)
                }

            }

            Spacer(modifier = Modifier.height(32.dp))

            ButtonM(
                onClick = {
                    viewModel.emailLogin()
                }) {
                Text(
                    text = stringResource(Res.string.login),
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

            Spacer(modifier = Modifier.height(32.dp))

            ClickableText(text = annotatedString, onClick = {offset ->
                annotatedString.getStringAnnotations(tag = "register", start = offset, end = offset).firstOrNull()?.let {
                    navigate(PosScreens.RegisterScreen)
                }
            })

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showRegisterDialog.value) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    showRegisterDialog.value = false
                },
                confirmButton = {
                    TextButtonS(label = stringResource(Res.string.go_register)) {
                        showRegisterDialog.value = false
                        navigate(PosScreens.RegisterScreen)
                    }
                },
                text = {
                    Column {
                        Text(
                            stringResource(Res.string.email_not_found, uiState.email),
                            style = TextStyle(fontFamily = latoFontFamily()),
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                },
                dismissButton = {
                    TextButtonS(label = stringResource(Res.string.cancel)) {
                        showRegisterDialog.value = false
                    }
                }
            )
        }

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

