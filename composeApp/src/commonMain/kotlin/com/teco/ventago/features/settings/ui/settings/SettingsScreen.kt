package com.teco.ventago.features.settings.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.design_system.buttons.DottedButton
import com.teco.ventago.design_system.buttons.SettingsTextButton
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.payments.PaymentItem
import com.teco.ventago.design_system.organism.BusinessImage
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.organism.SaveChangesBar
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.Gray50
import com.teco.ventago.design_system.theme.Gray80
import com.teco.ventago.design_system.theme.RedLight
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.design_system.theme.titleSmallBold
import com.teco.ventago.features.auth.ui.register.user.viewmodel.RegisterUiEvent
import com.teco.ventago.features.settings.ui.settings.viewmodel.SettingsState
import com.teco.ventago.features.settings.ui.settings.viewmodel.SettingsStateUiEvent
import com.teco.ventago.features.settings.ui.settings.viewmodel.SettingsViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.rememberPlatformState
import com.teco.ventago.utils.launchAutocompleteWidget
import com.teco.ventago.features.quotes.ui.settings.QuoteSettingsSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.action_settings
import ventago.composeapp.generated.resources.bank_transfer
import ventago.composeapp.generated.resources.business_address
import ventago.composeapp.generated.resources.business_address_notdot
import ventago.composeapp.generated.resources.business_info
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.change_business_address
import ventago.composeapp.generated.resources.change_business_logo
import ventago.composeapp.generated.resources.change_business_name
import ventago.composeapp.generated.resources.delete_account
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.ic_bank
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.no_address_selected
import ventago.composeapp.generated.resources.order_see_on_map
import ventago.composeapp.generated.resources.payment_methods
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.privacy_policy
import ventago.composeapp.generated.resources.request_camera_permission
import ventago.composeapp.generated.resources.save
import ventago.composeapp.generated.resources.select_photo_from_camera
import ventago.composeapp.generated.resources.select_photo_from_gallery
import ventago.composeapp.generated.resources.set_address
import ventago.composeapp.generated.resources.settings_configure_payment
import ventago.composeapp.generated.resources.sign_out
import ventago.composeapp.generated.resources.terms_and_conditions
import ventago.composeapp.generated.resources.yappy_logo
import kotlin.compareTo
import kotlin.text.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel<SettingsViewModel>(),
    navigate: (PosScreens) -> Unit) {
    val snackbarService: SnackbarService = koinInject()
    val uriHandler = LocalUriHandler.current
    val platformState = rememberPlatformState()
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val noAddressSelectedString = stringResource(Res.string.no_address_selected)

    // Start of State for the upload image bottom sheet
    val uploadImageSheetState = rememberModalBottomSheetState()
    val cameraManager = rememberCameraManager { viewModel.onImageSelected(it) }
    val galleryManager = rememberGalleryManager { viewModel.onImageSelected(it) }
    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(
            permissionType: PermissionType,
            status: PermissionStatus
        ) {
            when (status) {
                PermissionStatus.GRANTED -> {
                    when (permissionType) {
                        PermissionType.CAMERA -> viewModel.launchCamera()
                        PermissionType.GALLERY -> viewModel.launchGallery()
                    }
                }

                else -> {
                    viewModel.showPermissionRationalDialog(true)
                }
            }
        }
    })
    // End of State for the upload image bottom sheet

    var pendingEvent by remember { mutableStateOf<SettingsStateUiEvent?>(null) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            pendingEvent = event
        }
    }

    pendingEvent?.let { event ->
        when (event) {
            SettingsStateUiEvent.NoAddressSelected -> {
                LaunchedEffect(Unit) {
                    snackbarService.show(noAddressSelectedString)
                }
            }
            SettingsStateUiEvent.LaunchCamera -> {
                if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
                    cameraManager.launch()
                } else {
                    permissionsManager.askPermission(PermissionType.CAMERA)
                }
            }
            SettingsStateUiEvent.LaunchGallery -> {
                if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
                    galleryManager.launch()
                } else {
                    permissionsManager.askPermission(PermissionType.GALLERY)
                }
            }
            SettingsStateUiEvent.LaunchSettings -> permissionsManager.launchSettings()
            else -> println("Unhandled event: $event")
        }

        // Clear it after handling
        pendingEvent = null
    }

    Box (
        modifier = Modifier
            .fillMaxSize()
    ) {
        val isDirty  = rememberIsDirty(uiState, viewModel, uiState.sharedImage)
//        val canSave  = rememberCanSave(uiState, viewModel)
        Column (modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()) {
            // Profile Card
            Card(modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor(),
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = {  })
            {
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(Res.string.business_info),
                    style = titleMedium()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding( vertical = 8.dp)
                ) {
                    BusinessImage(uiState.imgUrl, uiState.imageBitmap) {
                        viewModel.showUploadImageSheet(true)
                    }
                }

                // Business Name
                DMOutlinedTextField(
                    text = uiState.newName,
                    stringResource(Res.string.name),
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onChange = { value ->
                        viewModel.onNameChange(value)
                    },
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    isError = (uiState.newName.isBlank() || uiState.newName.length < 3) && uiState.newName != uiState.actualName,
                    readOnly = uiState.invoicingEnabled
                )

                // Business Phone
                DMOutlinedTextField(
                    text = uiState.newPhone,
                    stringResource(Res.string.phone),
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onChange = { value ->
                        viewModel.onPhoneChanged(value)
                    },
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    isError = (uiState.newPhone.isBlank() || uiState.newPhone.length < 3
                            || !viewModel.isValidPhone(uiState.newPhone)) && uiState.newPhone != uiState.actualPhone,
                )

                DMOutlinedTextField(
                    text = uiState.newEmail,
                    stringResource(Res.string.email),
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onChange = { value ->
                        viewModel.onBusinessEmailChange(value)
                    },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    isError = (uiState.newEmail.isBlank() || uiState.newEmail.length < 3
                            || !viewModel.isValidPhone(uiState.newEmail)) && uiState.newEmail != uiState.actualEmail,
                )

                DMOutlinedTextField(
                    text = uiState.newRuc,
                    "RUC",
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onChange = { value ->
                        viewModel.onRucChange(value)
                    },
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    readOnly = true,
                    supportingText = "No editable"
//                    enabled = false,
//                    isError = (uiState.newRuc.isBlank() || uiState.newRuc.length < 3) && uiState.newRuc != uiState.actualRuc,
                )

                DMOutlinedTextField(
                    text = uiState.newWeb,
                    "Website",
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onChange = { value ->
                        viewModel.onWebSiteChange(value)
                    },
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                )

                // Business Address
                Row (
                    modifier = Modifier
                        .height(IntrinsicSize.Max)
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(10),
                        color = Gray80,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.order_see_on_map),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(10.dp, 10.dp)
                                .size(40.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f, fill = true)
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Top
                    ) {

                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = stringResource(Res.string.business_address),
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.onBackground),
                        )
                        Text(
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            text = if (uiState.isAddressFilled) uiState.actualAddress else stringResource(Res.string.no_address_selected),
                            textAlign = TextAlign.Start,
                            style = bodyMedium(color = MaterialTheme.colorScheme.onBackground),
                        )

                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButtonS(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        label = if (!uiState.isAddressFilled) {
                            stringResource(Res.string.set_address)
                        } else {
                            stringResource(Res.string.change_business_address)
                        }

                    ) {
                        try {
                            launchAutocompleteWidget(
                                onAddressSelected = { selected ->
                                    viewModel.setAddress(selected)
                                },
                                onCancelled = {
                                    viewModel.noAddressSelected()
                                }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            // Payments Card
            Card(modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor(),
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    navigate(PosScreens.Payments)
                })
            {
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(Res.string.payment_methods),
                    style = titleMedium()
                )
                if (uiState.loadingPaymentMethods) {
                    val brush = shimmerBrush()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .clip(shape = RoundedCornerShape(4.dp))
                                .background(brush = brush)
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .clip(shape = RoundedCornerShape(4.dp))
                                .background(brush = brush)
                        )
                    }
                    return@Card
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(Res.string.settings_configure_payment),
                    style = bodyMedium()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    uiState.availablePaymentMethods["paypal"]?.takeIf { it.visible && it.enabled }?.let { method ->
                        PaymentItem(
                            modifier = Modifier,
                            itemId = method.id,
                            title = {
                                Image(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 8.dp,
                                        top = 12.dp,
                                        bottom = 12.dp
                                    ),
                                    painter = painterResource(Res.drawable.paypal),
                                    contentDescription = "",
                                )
                            },
                            enabled = true,
                            onClick = { _ ->
                                navigate(PosScreens.Payments)
                            },
                        )
                    }

                    uiState.availablePaymentMethods["yappy"]?.takeIf { it.visible && it.enabled }?.let { method ->
                        PaymentItem(
                            modifier = Modifier,
                            itemId = method.id,
                            title = {
                                Image(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 8.dp,
                                        top = 12.dp,
                                        bottom = 12.dp
                                    ),
                                    painter = painterResource(Res.drawable.yappy_logo),
                                    contentDescription = "",
                                )
                            },
                            enabled = true,
                            onClick = { id ->
                                navigate(PosScreens.Payments)
                            },
                        )
                    }

                    uiState.availablePaymentMethods["transference"]?.takeIf { it.visible && it.enabled}?.let { method ->
                        PaymentItem(
                            modifier = Modifier,
                            itemId = method.id,
                            title = {
                                Image(
                                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                                    painter = painterResource(Res.drawable.ic_bank),
                                    contentDescription = "",
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground) // Tint the icon
                                )
                                Text(
                                    stringResource(Res.string.bank_transfer),
                                    style = titleSmallBold(),
                                    modifier = Modifier.padding(
                                        start = 8.dp,
                                        end = 8.dp,
                                        top = 10.dp,
                                        bottom = 10.dp
                                    )
                                )
                            },
                            enabled = true,
                            onClick = { _ ->
                                navigate(PosScreens.Payments)
                            },
                        )
                    }

                    if (viewModel.hasMorePaymentMethods()) {
                        DottedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding( start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .height(50.dp),
                            onClick = {
                                navigate(PosScreens.Payments)
                            }
                        )
                    }

                }

            }

//            if (uiState.hasQuotesAccess) {
                QuoteSettingsSection(
                    additionalInfo = uiState.defaultQuoteAdditionalInfo,
                    style = uiState.defaultQuoteStyle,
                    quotePrefix = uiState.quotePrefix,
                    onAdditionalInfoChange = { viewModel.setDefaultQuoteAdditionalInfo(it) },
                    onStyleChange = { viewModel.setDefaultQuoteStyle(it) },
                    onQuotePrefixChange = { viewModel.setQuotePrefix(it) }
                )
//            }

            Card(modifier = Modifier.fillMaxWidth().padding(all = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor(),
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = {})
            {
                if (uiState.invoicingEnabled) {
                    SettingsTextButton(
                        label = "Sucursales",
                        onClick = {
                            navigate(PosScreens.Branches)
                        }
                    )
                }


                SettingsTextButton(
                    label = stringResource(Res.string.terms_and_conditions),
                    onClick = {
                        uriHandler.openUri("https://sites.google.com/view/ventago-terms")
                    }
                )

                SettingsTextButton(
                    label = stringResource(Res.string.privacy_policy),
                    onClick = {
                        uriHandler.openUri("https://sites.google.com/view/ventago-politicas-privacidad")
                    }
                )

                SettingsTextButton(
                    label = stringResource(Res.string.sign_out),
                    onClick = {
                        viewModel.signOut()
                    },
                    color = RedLight
                )
            }




            if (isDirty) {
                Spacer(Modifier.height(80.dp)) // to avoid last item being hidden by the SaveChangesBar
            }

        }

        // ===== Sticky Save Bar overlay =====
        val navBarHeightPadding = 4.dp // keep above bottom NavigationBar (tune if needed)
        SaveChangesBar(
            visible = isDirty || uiState.sharedImage != null,
            canSave = true,
            onSave = {
                viewModel.saveChanges()
            },
            onDiscard = {
                viewModel.resetChanges()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = navBarHeightPadding)
        )

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.resetImage()
                viewModel.hideLoading()
            }
        }

        if (uiState.showUploadImageSheet) {
            ModalBottomSheet(
                containerColor = cardContainerColor(),
                sheetState = uploadImageSheetState,
                onDismissRequest = { viewModel.showUploadImageSheet(false) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButtonS(
                        label = stringResource(Res.string.select_photo_from_camera),
                        prefixIcon = rememberVectorPainter(Icons.Outlined.CameraAlt)
                    ) {
                        viewModel.launchCamera()
                        scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                            viewModel.showUploadImageSheet(false)
                        }
                    }

                    TextButtonS(
                        label = stringResource(Res.string.select_photo_from_gallery),
                        prefixIcon = rememberVectorPainter(Icons.Outlined.Image)
                    ) {
                        viewModel.launchGallery()
                        scope.launch { uploadImageSheetState.hide() }.invokeOnCompletion {
                            viewModel.showUploadImageSheet(false)
                        }
                    }
                }
            }
        }


        DMAlertDialog(
            title = stringResource(Res.string.select_photo_from_camera),
            message = stringResource(Res.string.request_camera_permission),
            confirmText = stringResource(Res.string.action_settings),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                viewModel.showPermissionRationalDialog(false)
                viewModel.launchSettings()
            },
            onDismiss = {
                viewModel.showPermissionRationalDialog(false)
            },
            show = uiState.showPermissionRationalDialog
        )
    }




}

@Composable
private fun rememberIsDirty(uiState: SettingsState, viewModel: SettingsViewModel, image: SharedImage?): Boolean {
    // Example validations; adapt to your uiState names
    return remember(uiState) {
        val quoteSettingsDirty = viewModel.isQuoteSettingsDirty(uiState)
        uiState.newName != uiState.actualName ||
                uiState.newPhone != uiState.actualPhone ||
                uiState.newRuc != uiState.actualRuc ||
                uiState.newEmail != uiState.actualEmail ||
                uiState.newWeb != uiState.actualWeb ||
                uiState.newAddress?.placeAddress != null ||
                image != null ||
                quoteSettingsDirty
    }
}

//@Composable
//private fun rememberCanSave(uiState: SettingsState, viewModel: SettingsViewModel): Boolean {
//    val nameValid = uiState.newName.isNotBlank() && uiState.newName.length >= 3
//    val phoneValid = uiState.newPhone.isNotBlank() && uiState.newPhone.length >= 3 &&
//            viewModel.isValidPhone(uiState.actualPhone)
//    return nameValid && phoneValid
//}
