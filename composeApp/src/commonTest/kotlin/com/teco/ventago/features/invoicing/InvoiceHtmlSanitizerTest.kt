package com.teco.ventago.features.invoicing

import com.teco.ventago.features.invoicing.domain.InvoiceHtmlSanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class InvoiceHtmlSanitizerTest {
    @Test
    fun sanitizeRichTextHtmlForSavePrefersLatestEditorHtmlOverDraftState() {
        val staleDraft = "<h2><strong>asdasdasd:</strong></h2>"
        val latestEditorHtml = "<h2><strong>asdasdasd Oscar:</strong></h2>"

        val sanitized = InvoiceHtmlSanitizer.sanitizeRichTextHtmlForSave(
            draftHtml = staleDraft,
            latestEditorHtml = latestEditorHtml,
        )

        assertEquals(latestEditorHtml, sanitized)
    }

    @Test
    fun sanitizeRichTextHtmlRemovesEditorOnlyListMarkup() {
        val raw = """
            <h2><strong>asdasdasd:</strong></h2><ol><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span>asdasd: asdasd</li><li data-list="ordered"><span class="ql-ui" contenteditable="false"></span>asdasdad:  asd</li></ol><blockquote>asdasdasdasdasdas</blockquote>
        """.trimIndent()

        val sanitized = InvoiceHtmlSanitizer.sanitizeRichTextHtml(raw)

        assertEquals(
            "<h2><strong>asdasdasd:</strong></h2><ol><li>asdasd: asdasd</li><li>asdasdad:  asd</li></ol><blockquote>asdasdasdasdasdas</blockquote>",
            sanitized,
        )
        assertFalse(sanitized.contains("ql-ui"))
        assertFalse(sanitized.contains("data-list"))
        assertFalse(sanitized.contains("contenteditable"))
    }
}
