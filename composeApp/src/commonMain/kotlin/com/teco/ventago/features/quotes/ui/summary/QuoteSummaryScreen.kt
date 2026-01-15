package com.teco.ventago.features.quotes.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavOptionsBuilder
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.pos.ui.viewmodel.CartCalc
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.ui.quoteStyleOptions
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.toDecimalString
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.additional_info
import ventago.composeapp.generated.resources.expiry_date
import ventago.composeapp.generated.resources.generate_quote
import ventago.composeapp.generated.resources.quote_additional_info_hint
import ventago.composeapp.generated.resources.quote_items_label
import ventago.composeapp.generated.resources.quote_subtotal_label
import ventago.composeapp.generated.resources.quote_taxes_label
import ventago.composeapp.generated.resources.quote_total_label
import ventago.composeapp.generated.resources.quote_style
import ventago.composeapp.generated.resources.quote_summary
import ventago.composeapp.generated.resources.save_default_additional_info
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteSummaryScreen(
    viewModel: PosViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val summary = CartCalc.summarize(uiState)
    val additionalInfoLabel = stringResource(Res.string.additional_info)
    val expiryLabel = stringResource(Res.string.expiry_date)
    val styleLabel = stringResource(Res.string.quote_style)
    val additionalInfoHint = stringResource(Res.string.quote_additional_info_hint)
    val styleOptions = quoteStyleOptions()
    val richTextState = rememberRichTextState()
    val currentSpanStyle = richTextState.currentSpanStyle
    val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true
    val isOrderedList = richTextState.isOrderedList
    val isUnorderedList = richTextState.isUnorderedList
    var defaultsApplied by remember { mutableStateOf(false) }
    var hasEditedAdditionalInfo by remember { mutableStateOf(false) }
    fun normalizeHtml(value: String?): String {
        val raw = value.orEmpty().trim()
        if (raw.isBlank()) return ""
        return raw.replace("\\s".toRegex(), "")
    }
    fun isBlankHtml(value: String?): Boolean {
        val normalized = normalizeHtml(value)
        return normalized.isEmpty() ||
            normalized == "<p></p>" ||
            normalized == "<p><br></p>" ||
            normalized == "<p><br/></p>"
    }
    val currentInfo = uiState.quoteAdditionalInfo.orEmpty()
    val defaultInfo = uiState.quotesSettings?.defaultAdditionalInfo.orEmpty()
    val showSaveDefaultAdditionalInfo = hasEditedAdditionalInfo &&
        !isBlankHtml(currentInfo) &&
        normalizeHtml(currentInfo) != normalizeHtml(defaultInfo)
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(defaultInfo) {
        if (defaultsApplied) {
            hasEditedAdditionalInfo = false
        }
    }

    LaunchedEffect(uiState.quoteAdditionalInfo) {
        val html = uiState.quoteAdditionalInfo.orEmpty()
        if (html != richTextState.toHtml()) {
            richTextState.setHtml(html)
        }
        if (!defaultsApplied && uiState.quotesSettings != null) {
            defaultsApplied = true
            hasEditedAdditionalInfo = false
        }
    }

    LaunchedEffect(richTextState) {
        snapshotFlow { richTextState.toHtml() }
            .distinctUntilChanged()
            .collectLatest { html ->
                if (!defaultsApplied && isBlankHtml(uiState.quoteAdditionalInfo) && isBlankHtml(html)) {
                    return@collectLatest
                }
                if (html != uiState.quoteAdditionalInfo.orEmpty()) {
                    if (defaultsApplied) {
                        hasEditedAdditionalInfo = true
                    }
                    viewModel.setQuoteMeta(additionalInfo = html)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(text = stringResource(Res.string.quote_summary), style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = vanishedBackgroundColor()
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.quote_items_label))
                    Text(uiState.cart.sumOf { it.quantity }.toString())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.quote_subtotal_label))
                    Text(summary.subtotal.toDecimalString())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.quote_taxes_label))
                    Text(summary.tax.toDecimalString())
                }
                Divider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.quote_total_label), style = bodyMediumBold())
                    Text(
                        summary.totalBeforeTip.toDecimalString(),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormatToggleButton(
                selected = isBold,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                icon = { Icon(Icons.Filled.FormatBold, contentDescription = "Bold") }
            )
            FormatToggleButton(
                selected = isItalic,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                icon = { Icon(Icons.Filled.FormatItalic, contentDescription = "Italic") }
            )
            FormatToggleButton(
                selected = isUnderline,
                onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                icon = { Icon(Icons.Filled.FormatUnderlined, contentDescription = "Underline") }
            )
            FormatToggleButton(
                selected = isUnorderedList,
                onClick = { richTextState.toggleUnorderedList() },
                icon = { Icon(Icons.Filled.FormatListBulleted, contentDescription = "Bullet list") }
            )
            FormatToggleButton(
                selected = isOrderedList,
                onClick = { richTextState.toggleOrderedList() },
                icon = { Icon(Icons.Filled.FormatListNumbered, contentDescription = "Numbered list") }
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedRichTextEditor(
            state = richTextState,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(additionalInfoLabel) },
            supportingText = { Text(additionalInfoHint) },
            minLines = 5,
            maxLines = 10
        )

        if (showSaveDefaultAdditionalInfo) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    viewModel.saveQuoteAdditionalInfoAsDefault()
                }) {
                    Text(text = stringResource(Res.string.save_default_additional_info))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        InstallmentDueDateFieldKmp(
            valueIso = uiState.quoteExpiryDate.orEmpty(),
            onDatePickedIso = { viewModel.setQuoteMeta(expiryDate = it) },
            modifier = Modifier.fillMaxWidth(),
            label = expiryLabel
        )

        Spacer(Modifier.height(12.dp))
        val selectedStyleIndex = styleOptions.indexOfFirst { it.key == uiState.quoteStyle }.takeIf { it >= 0 } ?: 0
        DMDropDownField(
            label = styleLabel,
            items = styleOptions.map { it.label },
            selectedIndex = selectedStyleIndex,
            modifier = Modifier.fillMaxWidth(),
            onItemSelected = { index, _ -> viewModel.setQuoteMeta(style = styleOptions[index].key) }
        )

        Spacer(Modifier.height(24.dp))
        ButtonM(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    if (uiState.quoteId == null) {
                        viewModel.createQuote()
                    } else {
                        viewModel.updateQuote()
                    }
                    navigate(PosScreens.QuoteSuccessScreen, null)
                }
            }
        ) {
            Text(text = stringResource(Res.string.generate_quote))
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
private fun FormatToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            icon()
        }
    }
}

@Composable
private fun QuoteSettingsSkeleton() {
    val brush = shimmerBrush()
    Column {
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )
        Spacer(Modifier.height(12.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        Spacer(Modifier.height(12.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        Spacer(Modifier.height(12.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
    }
}
