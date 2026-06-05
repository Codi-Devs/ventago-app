package com.teco.ventago.features.invoicing.domain

object InvoiceHtmlSanitizer {
    fun sanitizeRichTextHtmlForSave(
        draftHtml: String,
        latestEditorHtml: String?,
    ): String = sanitizeRichTextHtml(latestEditorHtml ?: draftHtml)

    fun sanitizeRichTextHtml(value: String): String {
        return value
            .replace(QL_UI_SPAN_REGEX, "")
            .replace(DATA_LIST_ATTRIBUTE_REGEX, "")
            .replace(CONTENTEDITABLE_ATTRIBUTE_REGEX, "")
            .replace(Regex(">\\s+<"), "><")
            .trim()
    }

    private val QL_UI_SPAN_REGEX = Regex(
        pattern = "<span\\b(?=[^>]*\\bclass\\s*=\\s*([\"'])[^\"']*\\bql-ui\\b[^\"']*\\1)[^>]*>\\s*</span>",
        option = RegexOption.IGNORE_CASE,
    )
    private val DATA_LIST_ATTRIBUTE_REGEX = Regex(
        pattern = "\\s+data-list\\s*=\\s*([\"']).*?\\1",
        option = RegexOption.IGNORE_CASE,
    )
    private val CONTENTEDITABLE_ATTRIBUTE_REGEX = Regex(
        pattern = "\\s+contenteditable\\s*=\\s*([\"']).*?\\1",
        option = RegexOption.IGNORE_CASE,
    )
}
