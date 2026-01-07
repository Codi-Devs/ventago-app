package com.teco.ventago.design_system.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleSmall
import org.jetbrains.compose.resources.painterResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.ic_arrow_forward_ios

@Composable
fun ButtonM(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.primary,
            contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary
            /* Other colors use values from MaterialTheme */
        ),
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        content = content,
        shape = RoundedCornerShape(10.dp),
        enabled = enabled
    )
}

@Composable
fun OutlinedButtonM(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton (
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.onPrimary,
            contentColor = contentColor ?: MaterialTheme.colorScheme.primary,

            /* Other colors use values from MaterialTheme */
        ),
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        border= border ?: ButtonDefaults.outlinedButtonBorder(enabled),
        content = content,
        shape = RoundedCornerShape(10.dp),
        enabled = enabled
    )
}

@Composable
fun SettingsTextButton(
    label: String,
    color: Color? = null,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.padding(0.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(0.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = color ?: MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier)
            Icon(
                modifier = Modifier
                    .size(width = 30.dp, height = 30.dp)
                    .padding(end = 8.dp),
                painter = painterResource(Res.drawable.ic_arrow_forward_ios),
                contentDescription = "Action",
                tint = color ?: MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TextButtonM(
    label: String,
    color: Color? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(0.dp)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(0.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = TextStyle(
                    fontWeight = FontWeight.W700,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.5.sp
                ),
                color = color ?: MaterialTheme.colorScheme.primary
            )
            icon?.let {
                Icon(
                    it,
                    contentDescription = "Share",
                )
            }

        }
    }
}

@Composable
fun TextButtonBL(
    label: String,
    color: Color? = null,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.padding(0.dp)) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(0.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = color ?: MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TextButtonS(
    modifier: Modifier = Modifier,
    label: String,
    color: Color? = null,
    prefixIcon: Painter? = null,
    icon: ImageVector? = null,
    overrideContentPadding: Boolean = false,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.padding(0.dp),
        contentPadding = if (overrideContentPadding) PaddingValues(0.dp) else PaddingValues()
        ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(30.dp)
                .padding(0.dp)
                .wrapContentSize()
        ) {
            if (prefixIcon != null) {
                Icon(
                    painter = prefixIcon,
                    contentDescription = "Prefix Icon",
                    modifier = Modifier.size(18.dp),
                    tint = color ?: MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = titleSmall(),
                color = color ?: MaterialTheme.colorScheme.primary
            )
            icon?.let {
                Icon(
                    it,
                    contentDescription = "Share",
                )
            }
        }
    }
}


@Composable
fun SocialButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Painter? = null,
) {
    ElevatedButton(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 3.dp,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(56.dp)
                .padding(0.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = icon!!,
                contentDescription = "Prefix Icon",
                modifier = Modifier.size(24.dp),
                tint= Color.Unspecified
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 21.dp),
                style = MaterialTheme.typography.bodyMedium.merge(
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

}
