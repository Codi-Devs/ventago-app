package com.teco.ventago.features.customers.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.customer.CustomerRow
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.customers.ui.list.viewmodel.CustomersListUiEvent
import com.teco.ventago.features.customers.ui.list.viewmodel.CustomersListViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.apply_filters
import ventago.composeapp.generated.resources.clear_filters
import ventago.composeapp.generated.resources.customers_empty
import ventago.composeapp.generated.resources.customers_filters_title
import ventago.composeapp.generated.resources.customers_search_placeholder
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.pos_clients_ruc
import ventago.composeapp.generated.resources.see_more

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersListScreen(
    viewModel: CustomersListViewModel = koinViewModel(),
    onCreateCustomer: () -> Unit,
    onCustomerSelected: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameFilterDraft by remember { mutableStateOf("") }
    var rucFilterDraft by remember { mutableStateOf("") }
    var emailFilterDraft by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CustomersListUiEvent.OpenCustomerDetails -> onCustomerSelected(event.customerId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DMOutlinedTextField(
                text = uiState.nameFilter,
                label = stringResource(Res.string.customers_search_placeholder),
                modifier = Modifier.weight(1f),
                onChange = viewModel::setNameFilter,
                leadingIcon = Icons.Rounded.Search,
                trailingIcon = Icons.Rounded.Tune,
                trailingIconClick = {
                    nameFilterDraft = uiState.nameFilter
                    rucFilterDraft = uiState.rucFilter
                    emailFilterDraft = uiState.emailFilter
                    showFilters = true
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            IconButton(onClick = viewModel::applyFilters) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            when {
                uiState.isLoading && uiState.customers.items.isEmpty() -> LoadingCustomersView()
                uiState.customers.items.isEmpty() && !uiState.isLoading -> EmptyCustomersView()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.customers.items, key = { it.id }) { customer ->
                            CustomerRow(
                                customer = customer,
                                selected = false,
                                onClick = { viewModel.openCustomer(customer.id) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.canLoadMore) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator()
                                    } else {
                                        TextButtonS(
                                            label = stringResource(Res.string.see_more),
                                            onClick = { viewModel.loadCustomers(reset = false) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onCreateCustomer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            CustomerFiltersSheet(
                name = nameFilterDraft,
                onNameChange = { nameFilterDraft = it },
                ruc = rucFilterDraft,
                onRucChange = { rucFilterDraft = it },
                email = emailFilterDraft,
                onEmailChange = { emailFilterDraft = it },
                onApply = {
                    viewModel.setNameFilter(nameFilterDraft)
                    viewModel.setRucFilter(rucFilterDraft)
                    viewModel.setEmailFilter(emailFilterDraft)
                    showFilters = false
                    viewModel.applyFilters()
                },
                onClear = {
                    showFilters = false
                    viewModel.clearFilters()
                }
            )
        }
    }
}

@Composable
private fun CustomerFiltersSheet(
    name: String,
    onNameChange: (String) -> Unit,
    ruc: String,
    onRucChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.customers_filters_title),
            style = MaterialTheme.typography.titleMedium
        )

        DMOutlinedTextField(
            text = name,
            label = stringResource(Res.string.name),
            onChange = onNameChange,
            modifier = Modifier.fillMaxWidth()
        )

        DMOutlinedTextField(
            text = ruc,
            label = stringResource(Res.string.pos_clients_ruc),
            onChange = onRucChange,
            modifier = Modifier.fillMaxWidth()
        )

        DMOutlinedTextField(
            text = email,
            label = stringResource(Res.string.email),
            onChange = onEmailChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        ButtonM(
            onClick = onApply,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Text(stringResource(Res.string.apply_filters))
        }

        OutlinedButtonM(onClick = onClear) {
            Text(stringResource(Res.string.clear_filters))
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun LoadingCustomersView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush())
                )
            }
        }
    }
}

@Composable
private fun EmptyCustomersView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.customers_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
