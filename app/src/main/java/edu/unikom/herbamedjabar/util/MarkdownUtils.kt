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

    private const val SPANNED_CACHE_CHAR_BUDGET = 64_000
    // Cache parsed Spanned results keyed by "flag|markdown" to avoid reparsing during binds
    private val spannedCache =
        object : LruCache<String, Spanned>(SPANNED_CACHE_CHAR_BUDGET) {
            override fun sizeOf(key: String, value: Spanned): Int = value.length
        }

    /**
     * Clears all in-memory caches used by MarkdownUtils, including the Spanned LruCache.
     *
     * Call this method from Application.onTrimMemory() or onLowMemory() to proactively release
     * memory under system pressure. Safe to call from any thread.
     *
     * Example usage: MarkdownUtils.clearCache()
     */
    @JvmStatic
    fun clearCache() {
        // Safe across threads due to coarse synchronization elsewhere
        synchronized(spannedCache) { spannedCache.evictAll() }
        // If additional caches are added in the future (e.g., parser/flavour/HtmlGenerator),
        // clear them here as well.
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
     * Parse markdown and return an Android Spanned (already HTML->Spanned converted). Results are
     * cached to reduce GC and CPU when RecyclerView binds rapidly.
     *
     * Thread safety: spannedCache access is synchronized to allow safe use from any thread.
     */
    fun parseMarkdownToSpanned(
        input: String?,
        key: String,
        formatList: Boolean = false,
        htmlMode: Int = HtmlCompat.FROM_HTML_MODE_COMPACT,
    ): Spanned {
        val compositeKey = buildString {
            append(key)
            append("|fmt:")
            append(if (formatList) 1 else 0)
            append("|md:")
            append(input?.hashCode() ?: 0)
            append("|len:")
            append(input?.length ?: 0)
            append("|mode:")
            append(htmlMode)
        }
        synchronized(spannedCache) {
            val cached = spannedCache[compositeKey]
            if (cached != null) return cached
            val html = parseMarkdownToHtml(input, formatList)
            val spanned = HtmlCompat.fromHtml(html, htmlMode)
            spannedCache.put(compositeKey, spanned)
            return spanned
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
        return text.replace(numberedListPattern, "\n$1")
    }

    // Preceded by a non-newline, then optional spaces before "n. "
    private val numberedListPattern by lazy { Regex("(?<=[^\\n\\r])\\s*(\\d+\\.\\s)") }
}
