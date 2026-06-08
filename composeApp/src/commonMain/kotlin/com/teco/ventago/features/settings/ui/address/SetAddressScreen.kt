package com.teco.ventago.features.settings.ui.address

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.Gray80
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.features.settings.ui.address.viewmodel.SetAddressViewModel
import com.teco.ventago.utils.launchAutocompleteWidget
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.business_address
import ventago.composeapp.generated.resources.change_business_address
import ventago.composeapp.generated.resources.order_see_on_map
import ventago.composeapp.generated.resources.save_address
import ventago.composeapp.generated.resources.set_address

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBusinessAddressScreen(viewModel: SetAddressViewModel) {

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row (
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .padding(top = 8.dp, start = 0.dp, end = 0.dp, bottom = 8.dp),
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
                    text = if (uiState.isAddressFilled) uiState.actualAddress else "No address selected",
                    textAlign = TextAlign.Start,
                    style = bodyMedium(color = MaterialTheme.colorScheme.onBackground),
                )

            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.isAddressFilled) {
            TextButtonS(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                label = stringResource(Res.string.change_business_address),
            ) {
                launchAutocompleteWidget(
                    onAddressSelected = { selected ->
                        viewModel.setAddress(selected)
                    },
                    onCancelled = {}
                )
            }
        }


        ButtonM(modifier = Modifier.padding(bottom = 16.dp),
            onClick = {
                if (!uiState.isAddressFilled) {
                    launchAutocompleteWidget(
                        onAddressSelected = { selected ->
                            viewModel.setAddress(selected)
                        },
                        onCancelled = {}
                    )
                } else {
                     viewModel.updateBusinessAddress()
                }
            }, enabled = !uiState.isAddressFilled || uiState.updateButtonEnabled
        ) {
            if (!uiState.isAddressFilled) {
                Text(stringResource(Res.string.set_address))
            } else {
                Text(stringResource(Res.string.save_address))
            }
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

@Composable
fun AddressSelectorView(
    currentAddress: String?,
    onSelectAddressClick: () -> Unit,
    onSaveClick: () -> Unit
) {

}