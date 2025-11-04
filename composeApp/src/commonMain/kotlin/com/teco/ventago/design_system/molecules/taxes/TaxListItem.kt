package com.teco.ventago.design_system.molecules.taxes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.Gray30
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.delete
import ventago.composeapp.generated.resources.edit

@Composable
fun TaxListItem(code: String, value: Int, editOnClick: () -> Unit, removeOnClick: () -> Unit) {
    var displayMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
            Text(
                text = code,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.Black,
            )
            Text(
                text = "$value %",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = Gray30,
            )
        }
        IconButton(onClick = { displayMenu = !displayMenu }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Show Menu",
//
            )
            DropdownMenu(expanded = displayMenu, onDismissRequest = { displayMenu = false }) {
                DropdownMenuItem(text = { Text(text = stringResource(Res.string.edit)) },
                    onClick = editOnClick,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Edit, contentDescription = "Edit"
                        )
                    })
                DropdownMenuItem(text = { Text(text = stringResource(Res.string.delete)) },
                    onClick = removeOnClick,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete, contentDescription = "Delete"
                        )
                    })
            }
        }

    }


}