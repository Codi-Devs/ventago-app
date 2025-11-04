package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint

@Composable
fun BranchItem(
    modifier: Modifier = Modifier,
    branch: Branch,
    onClick: (String) -> Unit = {},
) {
    ListRowCard(
        modifier = modifier,
        onClick = { onClick(branch.branchCode) },
        startSlot = {
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CountBadge(branch.fiscalBillingPoints.size, "Puntos")
            }
        },
        contentSlot = {
            Text(
                text = branch.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = bodyMediumBold()
            )
            StatusChip(active = branch.status == 1)
        },
        trailingSlot = {
        }
    )
}


@Composable
fun FiscalBillingPointItem(
    modifier: Modifier = Modifier,
    billingPoint: FiscalBillingPoint,
    onClick: (String) -> Unit = {},
    onOptionsClick: (String) -> Unit = {},
) {
    ListRowCard(
        modifier = modifier,
        onClick = { onClick(billingPoint.billingPoint) },
        startSlot = {
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = billingPoint.billingPoint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = titleMediumBold()
                )
            }
        },
        contentSlot = {
            Text(
                text = billingPoint.description ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = bodyMediumBold()
            )

        },
        trailingSlot = {
            Spacer(modifier = Modifier.weight(1f, fill = true))
            IconButton(onClick = { onOptionsClick(billingPoint.billingPoint) }) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}