package com.teco.ventago.features.customers.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.CustomerDetails
import com.teco.ventago.features.customers.ui.details.viewmodel.CustomerDetailsUiEvent
import com.teco.ventago.features.customers.ui.details.viewmodel.CustomerDetailsViewModel
import com.teco.ventago.utils.DateFormat.getOrdersFormattedDate
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.active
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.customers_add_address
import ventago.composeapp.generated.resources.customers_add_address_action
import ventago.composeapp.generated.resources.customers_address_country_code
import ventago.composeapp.generated.resources.customers_address_district
import ventago.composeapp.generated.resources.customers_address_email_optional
import ventago.composeapp.generated.resources.customers_address_line_label
import ventago.composeapp.generated.resources.customers_address_location_code_label
import ventago.composeapp.generated.resources.customers_address_location_code_placeholder
import ventago.composeapp.generated.resources.customers_address_province
import ventago.composeapp.generated.resources.customers_address_sheet_add_title
import ventago.composeapp.generated.resources.customers_address_sheet_edit_title
import ventago.composeapp.generated.resources.customers_address_township
import ventago.composeapp.generated.resources.customers_billing_addresses
import ventago.composeapp.generated.resources.customers_delete_address_confirm_message
import ventago.composeapp.generated.resources.customers_delete_address_confirm_title
import ventago.composeapp.generated.resources.customers_delete_customer
import ventago.composeapp.generated.resources.customers_delete_customer_confirm_message
import ventago.composeapp.generated.resources.customers_delete_customer_confirm_title
import ventago.composeapp.generated.resources.customers_dv_label
import ventago.composeapp.generated.resources.customers_edit_customer
import ventago.composeapp.generated.resources.customers_fe_type
import ventago.composeapp.generated.resources.customers_fe_type_contributing
import ventago.composeapp.generated.resources.customers_fe_type_final_consumer
import ventago.composeapp.generated.resources.customers_fe_type_foreigner
import ventago.composeapp.generated.resources.customers_fe_type_government
import ventago.composeapp.generated.resources.customers_general_information
import ventago.composeapp.generated.resources.customers_identification
import ventago.composeapp.generated.resources.customers_last_five_total
import ventago.composeapp.generated.resources.customers_no_billing_addresses
import ventago.composeapp.generated.resources.customers_no_customer_data
import ventago.composeapp.generated.resources.customers_no_recent_orders
import ventago.composeapp.generated.resources.customers_order_label
import ventago.composeapp.generated.resources.customers_orders_resume
import ventago.composeapp.generated.resources.customers_ruc_label
import ventago.composeapp.generated.resources.customers_see_all_orders
import ventago.composeapp.generated.resources.customers_taxpayer_type
import ventago.composeapp.generated.resources.customers_taxpayer_type_juridical
import ventago.composeapp.generated.resources.customers_taxpayer_type_natural
import ventago.composeapp.generated.resources.customers_total_orders
import ventago.composeapp.generated.resources.customers_legal_name
import ventago.composeapp.generated.resources.customers_retry
import ventago.composeapp.generated.resources.customers_status
import ventago.composeapp.generated.resources.customers_update_address_action
import ventago.composeapp.generated.resources.customers_date
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.edit
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.inactive
import ventago.composeapp.generated.resources.invalid_email
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.total

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: Long,
    viewModel: CustomerDetailsViewModel = koinViewModel(),
    onBackAfterDelete: () -> Unit,
    onEdit: (Long) -> Unit,
    onSeeAllOrders: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val addressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddressDeleteDialog by remember { mutableStateOf(false) }
    var selectedAddressId by remember { mutableStateOf<Long?>(null) }

    var showAddressSheet by remember { mutableStateOf(false) }
    var addressLineInput by remember { mutableStateOf("") }
    var locationCodeInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var editingAddressId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(customerId) {
        viewModel.load(customerId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CustomerDetailsUiEvent.CustomerDeleted -> onBackAfterDelete()
                CustomerDetailsUiEvent.AddressMutationSuccess -> {
                    showAddressSheet = false
                    editingAddressId = null
                    addressLineInput = ""
                    locationCodeInput = ""
                    emailInput = ""
                    emailError = false
                }
            }
        }
    }

    val customer = uiState.customer

    if (uiState.isLoading && customer == null) {
        CustomerDetailsLoading()
    } else if (customer == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.errorMessage ?: stringResource(Res.string.customers_no_customer_data),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            OutlinedButtonM(onClick = { viewModel.load(customerId) }) {
                Text(stringResource(Res.string.customers_retry))
            }
        }
    } else {
        val isForeignCustomer = customer.feCustomerType == "04"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GeneralInfoCard(customer = customer, isForeignCustomer = isForeignCustomer)

            BillingAddressesCard(
                addresses = uiState.addresses,
                onAddAddress = {
                    editingAddressId = null
                    addressLineInput = ""
                    locationCodeInput = ""
                    emailInput = ""
                    emailError = false
                    showAddressSheet = true
                },
                onEditAddress = { address ->
                    editingAddressId = address.id
                    addressLineInput = address.addressLine
                    locationCodeInput = address.locationCode.orEmpty()
                    emailInput = address.email.orEmpty()
                    emailError = false
                    showAddressSheet = true
                },
                onDeleteAddress = { addressId ->
                    selectedAddressId = addressId
                    showAddressDeleteDialog = true
                }
            )

            OrdersResumeCard(
                ordersCount = uiState.ordersCount,
                recentOrdersAmount = uiState.recentOrdersAmount,
                recentOrders = uiState.recentOrders,
                customerId = customer.id,
                onSeeAllOrders = onSeeAllOrders
            )

            ButtonM(
                onClick = { onEdit(customer.id) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(stringResource(Res.string.customers_edit_customer))
            }

            OutlinedButtonM(
                onClick = { showDeleteDialog = true },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                contentColor = MaterialTheme.colorScheme.error
            ) {
                Text(stringResource(Res.string.customers_delete_customer))
            }
        }
    }

    if (showAddressSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressSheet = false },
            sheetState = addressSheetState
        ) {
            AddressFormSheet(
                isEditMode = editingAddressId != null,
                addressLine = addressLineInput,
                locationCode = locationCodeInput,
                email = emailInput,
                emailError = emailError,
                onAddressLineChange = { addressLineInput = it },
                onLocationCodeChange = { locationCodeInput = it },
                onEmailChange = {
                    emailInput = it
                    if (emailError) emailError = false
                },
                onSubmit = {
                    if (addressLineInput.isBlank()) return@AddressFormSheet
                    val normalizedEmail = emailInput.trim().ifBlank { null }
                    if (normalizedEmail != null && !isValidAddressEmail(normalizedEmail)) {
                        emailError = true
                        return@AddressFormSheet
                    }
                    emailError = false
                    val locationCode = locationCodeInput.ifBlank { null }
                    val addressId = editingAddressId
                    if (addressId == null) {
                        viewModel.createAddress(addressLineInput, locationCode, normalizedEmail)
                    } else {
                        viewModel.updateAddress(addressId, addressLineInput, locationCode, normalizedEmail)
                    }
                }
            )
        }
    }

    if (showDeleteDialog) {
        DMAlertDialog(
            title = stringResource(Res.string.customers_delete_customer_confirm_title),
            message = stringResource(Res.string.customers_delete_customer_confirm_message),
            show = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCustomer()
            },
            confirmText = stringResource(Res.string.delete),
            dismissText = stringResource(Res.string.cancel)
        )
    }

    if (showAddressDeleteDialog && selectedAddressId != null) {
        DMAlertDialog(
            title = stringResource(Res.string.customers_delete_address_confirm_title),
            message = stringResource(Res.string.customers_delete_address_confirm_message),
            show = true,
            onDismiss = {
                showAddressDeleteDialog = false
                selectedAddressId = null
            },
            onConfirm = {
                val addressId = selectedAddressId
                showAddressDeleteDialog = false
                selectedAddressId = null
                if (addressId != null) {
                    viewModel.deleteAddress(addressId)
                }
            },
            confirmText = stringResource(Res.string.delete),
            dismissText = stringResource(Res.string.cancel)
        )
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState,
            onDismissRequest = viewModel::hideLoading
        )
    }
}

@Composable
private fun GeneralInfoCard(
    customer: CustomerDetails,
    isForeignCustomer: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(Res.string.customers_general_information), style = titleMediumBold())
                }
                StatusChip(status = customer.status)
            }

            InfoRow(stringResource(Res.string.customers_legal_name), customer.legalName ?: "-")
            if (isForeignCustomer) {
                InfoRow(
                    stringResource(Res.string.customers_identification),
                    customer.foreignIdNumber ?: customer.rucNumber ?: "-"
                )
            } else {
                InfoRow(stringResource(Res.string.customers_ruc_label), customer.rucNumber ?: "-")
                InfoRow(stringResource(Res.string.customers_dv_label), customer.rucCheckDigit ?: "-")
            }
            InfoRow(stringResource(Res.string.customers_fe_type), customerTypeLabel(customer.feCustomerType))
            InfoRow(stringResource(Res.string.customers_taxpayer_type), taxpayerTypeLabel(customer.taxpayerType))
            InfoRow(stringResource(Res.string.email), customer.email ?: "-")
            InfoRow(stringResource(Res.string.phone), customer.phone1 ?: "-")
            InfoRow(
                stringResource(Res.string.customers_status),
                if (customer.status == 1) stringResource(Res.string.active) else stringResource(Res.string.inactive)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Divider()
            Spacer(modifier = Modifier.height(4.dp))

            if (isForeignCustomer) {
                InfoRow(stringResource(Res.string.customers_address_country_code), customer.countryCode ?: "-")
            } else {
                InfoRow(stringResource(Res.string.customers_address_line_label), customer.addressLine ?: "-", maxLines = 2)
                InfoRow(stringResource(Res.string.customers_address_province), customer.province ?: "-")
                InfoRow(stringResource(Res.string.customers_address_district), customer.district ?: "-")
                InfoRow(stringResource(Res.string.customers_address_township), customer.corregimiento ?: "-")
                InfoRow(stringResource(Res.string.customers_address_country_code), customer.countryCode ?: "-")
            }
        }
    }
}

@Composable
private fun BillingAddressesCard(
    addresses: List<CustomerAddress>,
    onAddAddress: () -> Unit,
    onEditAddress: (CustomerAddress) -> Unit,
    onDeleteAddress: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(Res.string.customers_billing_addresses), style = bodyMediumBold())
            }

            if (addresses.isEmpty()) {
                Text(stringResource(Res.string.customers_no_billing_addresses), style = bodyMedium())
            } else {
                addresses.forEachIndexed { index, address ->
                    AddressRow(
                        address = address,
                        onEdit = { onEditAddress(address) },
                        onDelete = { onDeleteAddress(address.id) }
                    )

                    if (index < addresses.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            ButtonM(onClick = onAddAddress) {
                Text(stringResource(Res.string.customers_add_address))
            }
        }
    }
}

@Composable
private fun AddressRow(
    address: CustomerAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = address.addressLine,
                    style = bodyMedium(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (address.isDefault) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Predeterminada",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = labelSmall(color = MaterialTheme.colorScheme.onSecondaryContainer)
                        )
                    }
                }
            }

            val locationText = listOf(
                address.province,
                address.district,
                address.corregimiento
            ).filterNotNull().filter { it.isNotBlank() }.joinToString(", ")

            if (locationText.isNotBlank()) {
                Text(
                    text = locationText,
                    style = bodyMedium(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            address.email?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = bodyMedium(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(Res.string.edit)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun OrdersResumeCard(
    ordersCount: Long,
    recentOrdersAmount: Double,
    recentOrders: List<com.teco.ventago.features.orders.domain.models.Order>,
    customerId: Long,
    onSeeAllOrders: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(Res.string.customers_orders_resume), style = bodyMediumBold())
            }

            InfoRow(stringResource(Res.string.customers_total_orders), ordersCount.toString())
            InfoRow(
                stringResource(Res.string.customers_last_five_total),
                formatNumberToMoney(recentOrdersAmount.toString())
            )

            Divider()

            if (recentOrders.isEmpty()) {
                Text(stringResource(Res.string.customers_no_recent_orders), style = bodyMedium())
            } else {
                recentOrders.forEachIndexed { index, order ->
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(stringResource(Res.string.customers_order_label), order.internalNumber)
                        InfoRow(stringResource(Res.string.customers_date), getOrdersFormattedDate(order.createdAt))
                        InfoRow(stringResource(Res.string.total), formatNumberToMoney(order.totalAmount))
                    }

                    if (index < recentOrders.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            OutlinedButtonM(onClick = { onSeeAllOrders(customerId) }) {
                Text(stringResource(Res.string.customers_see_all_orders))
            }
        }
    }
}

@Composable
private fun AddressFormSheet(
    isEditMode: Boolean,
    addressLine: String,
    locationCode: String,
    email: String,
    emailError: Boolean,
    onAddressLineChange: (String) -> Unit,
    onLocationCodeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isEditMode) {
                stringResource(Res.string.customers_address_sheet_edit_title)
            } else {
                stringResource(Res.string.customers_address_sheet_add_title)
            },
            style = MaterialTheme.typography.titleMedium
        )

        DMOutlinedTextField(
            text = addressLine,
            label = stringResource(Res.string.customers_address_line_label),
            onChange = onAddressLineChange,
            modifier = Modifier.fillMaxWidth()
        )

        DMOutlinedTextField(
            text = locationCode,
            label = stringResource(Res.string.customers_address_location_code_label),
            onChange = onLocationCodeChange,
            modifier = Modifier.fillMaxWidth(),
            supportingText = stringResource(Res.string.customers_address_location_code_placeholder)
        )

        DMOutlinedTextField(
            text = email,
            label = stringResource(Res.string.customers_address_email_optional),
            onChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email,
            isError = emailError,
            supportingText = if (emailError) {
                stringResource(Res.string.invalid_email)
            } else {
                ""
            }
        )

        ButtonM(onClick = onSubmit) {
            Text(
                if (isEditMode) {
                    stringResource(Res.string.customers_update_address_action)
                } else {
                    stringResource(Res.string.customers_add_address_action)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun isValidAddressEmail(email: String): Boolean {
    val parts = email.split("@")
    if (parts.size != 2) return false
    if (parts[0].isBlank()) return false
    val domain = parts[1]
    if (domain.isBlank()) return false
    if (domain.startsWith(".") || domain.endsWith(".")) return false
    return domain.contains(".")
}

@Composable
private fun StatusChip(status: Int) {
    val isActive = status == 1
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Text(
            text = if (isActive) stringResource(Res.string.active) else stringResource(Res.string.inactive),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = labelSmall(
                color = if (isActive) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        )
    }
}

@Composable
private fun customerTypeLabel(code: String?): String {
    return when (code) {
        "01" -> stringResource(Res.string.customers_fe_type_contributing)
        "02" -> stringResource(Res.string.customers_fe_type_final_consumer)
        "03" -> stringResource(Res.string.customers_fe_type_government)
        "04" -> stringResource(Res.string.customers_fe_type_foreigner)
        else -> code ?: "-"
    }
}

@Composable
private fun taxpayerTypeLabel(code: String?): String {
    return when (code) {
        "1" -> stringResource(Res.string.customers_taxpayer_type_natural)
        "2" -> stringResource(Res.string.customers_taxpayer_type_juridical)
        else -> code ?: "-"
    }
}

@Composable
private fun InfoRow(label: String, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = bodyMedium(),
            textAlign = TextAlign.End,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun CustomerDetailsLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 260.dp else 150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush())
            )
        }
    }
}
