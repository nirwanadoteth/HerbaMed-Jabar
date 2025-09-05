package edu.unikom.herbamedjabar.util

import android.text.Spanned
import androidx.collection.LruCache
import androidx.core.text.HtmlCompat
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object MarkdownUtils {
    private val flavour by lazy { CommonMarkFlavourDescriptor() }
    private val parser by lazy { MarkdownParser(flavour) }

    private const val HTML_CACHE_SIZE = 64_000
    // Cache parsed Spanned results keyed by "flag|markdown" to avoid reparsing during binds
    private val htmlCache =
        object : LruCache<String, Spanned>(HTML_CACHE_SIZE) {
            override fun sizeOf(key: String, value: Spanned): Int = value.length
        }

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
     * Parse markdown and return an Android Spanned (already HTML->Spanned converted).
     * Results are cached to reduce GC and CPU when RecyclerView binds rapidly.
     */
    fun parseMarkdownToSpanned(input: String?, key: String, formatList: Boolean = false): Spanned {
        val cached = htmlCache[key]

        return cached
            ?: run {
                val html = MarkdownUtils.parseMarkdownToHtml(input, formatList)
                HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).also {
                    htmlCache.put(key, it)
                }
            }
    }

    /**
     * Adds line breaks before numbered list items (e.g., "1. ", "2. ") except at the start of the
     * string. This helps Markdown parsers correctly identify list items.
     *
     * @param text The input string to format.
     * @return The formatted string with line breaks before numbered list items.
     */
    private fun addLineBreaksToNumberedList(text: String): String {
        // Insert a newline before inline "n. " that aren't already at line start.
        // - Not at start-of-input, not already preceded by \n, and consume optional spaces before
        // the number.
        val pattern = Regex("""(?<!\A)(?<![\n\r])\s*(\d+\.\s)""")
        return text.replace(pattern, "\n$1")
    }
}
