package com.teco.ventago.design_system.molecules.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.navigation.PosScreens

@Composable
fun CustomerRow(
    modifier: Modifier = Modifier,
    customer: CustomerListItem,
    selected: Boolean,
    onClick: (CustomerListItem) -> Unit = {},
) {

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor(),
        ),
        shape = RoundedCornerShape(10.dp),
        onClick = { onClick(customer) }) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = customer.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = bodyMediumBold(
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                if (customer.ruc != null && customer.ruc.isNotEmpty()) {
                    Text(
                        text = customer.ruc,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = bodySmall(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                } else {
                    customer.email?.let { email ->
                        if (email.isNotEmpty() && !email.contains("pos.com")) {
                            Text(
                                text = customer.email,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = bodySmall(
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                }



            }

            Spacer(modifier = Modifier.weight(1f))

//            Icon(
//                imageVector = Icons.Rounded.MoreVert,
//                contentDescription = "",
//                tint = MaterialTheme.colorScheme.secondary
//            )


        }
    }
}


@Composable
fun PosCustomerSelection(
   customer: CustomerListItem?,
    navigate: (PosScreens) -> Unit
) {
    customer?.let { customer ->
        Surface(
            color = MaterialTheme.colorScheme.secondary,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { navigate(PosScreens.SearchCustomerScreen) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically), // ✅ this ensures text stays vertically centered
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = customer.name,
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!customer.ruc.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = customer.ruc!!,
                            style = labelSmall(
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.9f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.FindReplace,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    } ?: run {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            border = BorderStroke(
                color = MaterialTheme.colorScheme.secondary,
                width = 2.dp,
            ),
            shape = RoundedCornerShape(12.dp),
            onClick = { navigate(PosScreens.SearchCustomerScreen) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Seleccionar cliente",
                    modifier = Modifier.padding(16.dp),
                    style = bodyMedium(color = MaterialTheme.colorScheme.secondary)
                )
            }

        }
    }
}