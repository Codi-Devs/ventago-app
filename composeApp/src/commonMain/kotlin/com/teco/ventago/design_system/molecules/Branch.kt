package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.utils.getImageRequest

@Composable
fun BranchItem(
    modifier: Modifier = Modifier,
    branch: Branch,
    isLogoLoading: Boolean = false,
    onClick: (String) -> Unit = {},
    onUploadLogoClick: () -> Unit = {},
    onViewLogoClick: () -> Unit = {},
    onDeleteLogoClick: () -> Unit = {},
) {
    val hasLogo = branch.logoUrl.isValidLogoUrl()

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
            branch.tradeName
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?.let { tradeName ->
                    Text(
                        text = tradeName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            Text(
                text = "Código ${branch.branchCode}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            StatusChip(active = branch.status == 1)
            Spacer(modifier = Modifier.height(8.dp))
            BranchLogoStatus(
                branch = branch,
                hasLogo = hasLogo,
                isLogoLoading = isLogoLoading,
                onUploadLogoClick = onUploadLogoClick,
                onViewLogoClick = onViewLogoClick,
                onDeleteLogoClick = onDeleteLogoClick,
            )
        },
        trailingSlot = {
        }
    )
}

@Composable
private fun BranchLogoStatus(
    branch: Branch,
    hasLogo: Boolean,
    isLogoLoading: Boolean,
    onUploadLogoClick: () -> Unit,
    onViewLogoClick: () -> Unit,
    onDeleteLogoClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BranchLogoThumb(branch = branch, hasLogo = hasLogo, isLogoLoading = isLogoLoading)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (hasLogo) "Logo configurado" else "Sin logo configurado",
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButtonS(
                    label = if (hasLogo) "Reemplazar logo" else "Agregar logo",
                    prefixIcon = rememberVectorPainter(Icons.Outlined.Image),
                    overrideContentPadding = true,
                ) {
                    if (!isLogoLoading) onUploadLogoClick()
                }
                if (hasLogo) {
                    IconButton(
                        modifier = Modifier.size(30.dp),
                        enabled = !isLogoLoading,
                        onClick = onViewLogoClick,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Ver logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(30.dp),
                        enabled = !isLogoLoading,
                        onClick = onDeleteLogoClick,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Eliminar logo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchLogoThumb(
    branch: Branch,
    hasLogo: Boolean,
    isLogoLoading: Boolean,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        if (hasLogo) {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, branch.logoUrl.orEmpty()),
                contentDescription = "Logo de sucursal",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Text(
                text = branch.branchCode.takeLast(2).ifBlank { "S" },
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
            )
        }
        if (isLogoLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

private fun String?.isValidLogoUrl(): Boolean {
    return !this.isNullOrBlank() && !this.equals("null", ignoreCase = true)
}


@Composable
fun FiscalBillingPointItem(
    modifier: Modifier = Modifier,
    billingPoint: FiscalBillingPoint,
    hasConfiguredPrinter: Boolean = false,
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
            if (hasConfiguredPrinter) {
                Icon(
                    imageVector = Icons.Rounded.Print,
                    contentDescription = "Impresora configurada",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
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
