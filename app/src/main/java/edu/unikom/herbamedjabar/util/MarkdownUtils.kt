package edu.unikom.herbamedjabar.util

import android.text.Spanned
import androidx.collection.LruCache
import androidx.core.text.HtmlCompat
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

object MarkdownUtils {
    private val flavour by lazy { CommonMarkFlavourDescriptor() }
    private val parserTL by lazy { ThreadLocal.withInitial { MarkdownParser(flavour) } }

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
        val parsedTree = parserTL.get()!!.buildMarkdownTreeFromString(formatted)
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
            append("|md64:")
            append(input?.let(::fnv1a64) ?: 0L)
            append("|mode:")
            append(htmlMode)
        }
        // Fast path: check under lock, then compute outside the lock
        synchronized(spannedCache) { spannedCache[compositeKey] }
            ?.let {
                return it
            }
        val html = parseMarkdownToHtml(input, formatList)
        val spanned = HtmlCompat.fromHtml(html, htmlMode)
        // Second check before inserting to avoid duplicate work
        synchronized(spannedCache) {
            spannedCache[compositeKey]?.let {
                return it
            }
            spannedCache.put(compositeKey, spanned)
        }
        return spanned
    }

    /**
     * Heuristically inserts a newline before inline numbered list tokens (e.g., "1. ", "2. ") when
     * they follow common list-introducing punctuation (':', ';', ')', ']'). This reduces false
     * positives in prose while helping the parser recognize lists.
     */
    private fun addLineBreaksToNumberedList(text: String): String {
        // Insert a newline only after [:;)]], consuming optional spaces before the number.
        return text.replace(numberedListPattern, "\n$1")
    }

    // Only after colon/semicolon/closing bracket to reduce false positives
    private val numberedListPattern by lazy { Regex("(?<=[:;\\)\\]])\\s*(\\d+\\.\\s)") }

    // 64-bit FNV-1a: fast, non-crypto, good dispersion for cache keys
    private fun fnv1a64(s: String): Long {
        var h = -0x340d631b9dd648dbL /* FNV offset basis */
        val prime = 0x100000001b3L
        for (c in s) {
            h = h xor c.code.toLong()
            h *= prime
        }
        return h
    }
}
