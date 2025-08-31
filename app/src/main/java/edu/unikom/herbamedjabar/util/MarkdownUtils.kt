package edu.unikom.herbamedjabar.util

import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object MarkdownUtils {
    /**
     * Converts a Markdown string to HTML.
     *
     * @param input The Markdown input string. If null, treated as an empty string.
     * @param formatList If true, adds line breaks before numbered list items for better parsing.
     * @return The HTML representation of the Markdown input.
     */
    @JvmStatic
    fun parseMarkdownToHtml(input: String?, formatList: Boolean = false): String {
        val raw = input ?: ""
        val formatted = if (formatList) addLineBreaksToNumberedList(raw) else raw
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(formatted)
        return HtmlGenerator(formatted, parsedTree, flavour).generateHtml()
    }

    /**
     * Adds line breaks before numbered list items (e.g., "1. ", "2. ") except at the start of the string.
     * This helps Markdown parsers correctly identify list items.
     *
     * @param text The input string to format.
     * @return The formatted string with line breaks before numbered list items.
     */
    private fun addLineBreaksToNumberedList(text: String): String {
        return text.replace(Regex("""(\d+\.\s*)""")) { match ->
            if (match.range.first == 0) match.value else "\n${match.value}"
        }
    }
}
