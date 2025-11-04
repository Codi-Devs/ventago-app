package com.teco.ventago.design_system.molecules.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teco.ventago.core.Paged
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.customers.domain.models.CustomerListItem

@Composable
fun CustomerListOrganism(
    customers: Paged<CustomerListItem>,
    selectedCustomer: CustomerListItem?,
    onItemClick: (CustomerListItem) -> Unit = {},
    modifier: Modifier = Modifier) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
            )
            .padding(top = 8.dp, bottom = 8.dp),
        state = lazyListState,
    ) {
        items(customers.items, key = { it.id }) { customer ->
            CustomerRow(customer = customer,
                selected = selectedCustomer?.id == customer.id,
                onClick = onItemClick)
        }
    }
}

//@Composable
//fun ClientListOrganism(viewModel: ClientListViewModel, modifier: Modifier = Modifier) {
//    val uiState by viewModel.uiState.collectAsState()
////    val list = remember { uiState.customers.items }
//    val lazyListState = rememberLazyListState()
//
//    val showAlertDialog = remember { mutableStateOf(false) }
//
//    Column(
//        modifier = modifier
//            .padding(horizontal = 0.dp)
//            .fillMaxSize()
//    ) {
//
//
//        Spacer(modifier = Modifier.weight(1f))
//
//        if (viewModel.state.customer.value.name.isNotEmpty()) {
//            ButtonM(
//                modifier = Modifier.padding(horizontal = 16.dp),
//                onClick = {
//                    viewModel.selectCustomer(-1)
//                }) {
//                Text(stringResource(Res.string.pos_delete_client))
//            }
//
//            OutlinedButtonM(
//                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
//                onClick = {
//                    showAlertDialog.value = true
//                }) {
//                Text(stringResource(Res.string.pos_unlink_client))
//            }
//        }
//
//
//
//        DMAlertDialog(
//            title = stringResource(Res.string.delete),
//            message = stringResource(Res.string.pos_sure_delete_account),
//            show = remember { showAlertDialog }.value,
//            onDismiss = {
//                showAlertDialog.value = false
//            },
//            onConfirm = {
//                viewModel.removeSelectedCustomer()
//                showAlertDialog.value = false
//            },
//            confirmText = stringResource(Res.string.delete),
//            dismissText = stringResource(Res.string.cancel)
//        )
//
//    }
//}