package com.teco.ventago.features.pos.ui.customer.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.customer.CustomerListOrganism
import com.teco.ventago.design_system.organism.EmptyClientsContent
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.pos.ui.customer.list.viewmodel.ClientListViewModel
import com.teco.ventago.isTablet
import com.teco.ventago.navigation.NavResults
import com.teco.ventago.navigation.PosScreens
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.pos_delete_client
import ventago.composeapp.generated.resources.pos_sure_delete_account
import ventago.composeapp.generated.resources.pos_unlink_client

@Composable
fun ClientListActions(
    backStackEntry: NavBackStackEntry?,
    navigate: (PosScreens) -> Unit) {

    IconButton(onClick = {
        navigate(PosScreens.AddCustomerScreen)
    }
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ClientListScreen(
    viewModel: ClientListViewModel = koinViewModel(),
    onCustomerSelected: (CustomerListItem) -> Unit,
    navigateBack: () -> Unit) {
    if (isTablet()) {
        ClientTabletListContent(viewModel, onCustomerSelected, navigateBack)
    } else {
        CustomerListContent(viewModel, onCustomerSelected)
    }
}

@Composable
private fun ClientTabletListContent(viewModel: ClientListViewModel, onCustomerSelected: (CustomerListItem) -> Unit, navigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (uiState.customers.items.isEmpty() && uiState.selectedCustomer == null) {
            EmptyClientsContent()
        } else {
            CustomerListContent(viewModel, onCustomerSelected)
        }

//        AddClientListOrganism(viewModel) {
//            navigateBack()
//        }
    }
}

@Composable
private fun CustomerListContent(viewModel: ClientListViewModel, onCustomerSelected: (CustomerListItem) -> Unit,) {
    val uiState by viewModel.uiState.collectAsState()
    val showAlertDialog = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
    ) {

        CustomerListOrganism(
            customers = uiState.customers,
            selectedCustomer = uiState.selectedCustomer,
            onItemClick = { customer ->
                if (uiState.invoicingEnabled && customer.invoiceCustomer <= 0) {
                    // Deselect customer if already selected
                } else {
                    onCustomerSelected(customer)
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.selectedCustomer?.name?.isNotEmpty() ?: false) {
            ButtonM(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
//                    viewModel.selectCustomer(-1)
                }) {
                Text(stringResource(Res.string.pos_delete_client))
            }

            OutlinedButtonM(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = {
                    showAlertDialog.value = true
                }) {
                Text(stringResource(Res.string.pos_unlink_client))
            }
        }



        DMAlertDialog(
            title = stringResource(Res.string.delete),
            message = stringResource(Res.string.pos_sure_delete_account),
            show = remember { showAlertDialog }.value,
            onDismiss = {
                showAlertDialog.value = false
            },
            onConfirm = {
//                viewModel.removeSelectedCustomer()
                showAlertDialog.value = false
            },
            confirmText = stringResource(Res.string.delete),
            dismissText = stringResource(Res.string.cancel)
        )
    }
}