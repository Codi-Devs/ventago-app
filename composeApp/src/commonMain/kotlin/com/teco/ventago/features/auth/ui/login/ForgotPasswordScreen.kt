package com.teco.ventago.features.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavOptions
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.organism.LoadingBottomSheet
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.auth.ui.login.viewmodel.LoginViewModel
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.email_invalid
import ventago.composeapp.generated.resources.forgot_password_desc
import ventago.composeapp.generated.resources.get_link

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navigate: (PosScreens) -> Unit) {
    val viewModel: LoginViewModel = koinViewModel<LoginViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.forgot_password_desc),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(400),
                textAlign = TextAlign.Center,
                letterSpacing = 0.02.sp,
            )
        )
        Spacer(modifier = Modifier.height(36.dp))
        DMOutlinedTextField(
            text = uiState.forgotEmail,
            label = stringResource(Res.string.email),
            modifier = Modifier.padding(bottom = 16.dp),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onChange = { newEmail ->
                viewModel.forgotEmailChanged(newEmail)
            },
            isError = remember { uiState.forgotEmailInvalid },
            supportingText = if (uiState.forgotEmailInvalid) stringResource(Res.string.email_invalid) else ""
        )

        ButtonM(
            onClick = {
                viewModel.forgotPassword()
            }) {
            Text(
                text = stringResource(Res.string.get_link),
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