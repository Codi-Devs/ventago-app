package com.teco.ventago.features.branches.ui.branches.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.teco.ventago.design_system.molecules.BranchItem
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.branches.ui.branches.manage.viewmodel.BranchesManageViewModel
import com.teco.ventago.navigation.BillingPointManageRoute
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesManageScreen(
    viewModel: BranchesManageViewModel = koinViewModel<BranchesManageViewModel>(),
    navController: NavHostController
) {

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val lazyListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                )
                .padding(top = 8.dp, bottom = 8.dp),
            state = lazyListState,
        ) {
            items(uiState.branches, key = { it.branchCode }) { branch ->
                BranchItem(modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
                ), branch = branch, onClick = {
                    navController.navigate(BillingPointManageRoute(branch.branchCode))
                })
            }

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