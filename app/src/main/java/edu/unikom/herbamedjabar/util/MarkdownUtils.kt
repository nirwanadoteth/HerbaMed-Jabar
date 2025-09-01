package edu.unikom.herbamedjabar.util

import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object MarkdownUtils {
    private val flavour by lazy { CommonMarkFlavourDescriptor() }
    private val parser by lazy { MarkdownParser(flavour) }

    /**
     * Converts a Markdown string to HTML.
     *
     * @param input The Markdown input string. If null, treated as an empty string.
     * @param formatList If true, adds line breaks before numbered list items for better parsing.
     * @return The HTML representation of the Markdown input.
     */
    fun parseMarkdownToHtml(input: String?, formatList: Boolean = false): String {
        val raw = input ?: ""
        val formatted = if (formatList) addLineBreaksToNumberedList(raw) else raw
        val parsedTree = parser.buildMarkdownTreeFromString(formatted)
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
        // Insert a newline before inline "n. " that aren't already at line start.
        // - Not at start-of-input, not already preceded by \n, and consume optional spaces before the number.
        val pattern = Regex("""(?<!\A)(?<![\n\r])\s*(\d+\.\s)""")
        return text.replace(pattern, "\n$1")
    }
}
