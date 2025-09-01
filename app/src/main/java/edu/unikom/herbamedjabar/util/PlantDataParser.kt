package edu.unikom.herbamedjabar.util

/**
 * Utility object for parsing structured plant data from AI-generated Markdown text.
 *
 * Expected input format:
 * - Contains sections with emoji headers, e.g.:
 *   🌿 [Plant Name]*Nama Ilmiah
 *   ### 📝 Deskripsi
 *   ### 🩺 Potensi Manfaat & Kegunaan
 *   ### ⚠️ Peringatan & Efek Samping
 *
 * Returns a map with keys: plantName, description, benefit, warning.
 * If the plant cannot be identified, returns a standard message.
 */
object PlantDataParser {
    private const val KEY_PLANT_NAME = "plantName"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_BENEFIT = "benefit"
    private const val KEY_WARNING = "warning"
    private const val KEY_HERBAL_STATUS = "herbalStatus"

    /**
     * Parses plant data from AI-generated Markdown text.
     *
     * @param text The Markdown text to parse.
     * @return Map with keys: plantName, description, benefit, warning.
     */
    fun parsePlantData(text: String): Map<String, String> {
        val dataMap = mutableMapOf<String, String>()
        val originalText = text

        if (isUnidentifiedPlant(originalText)) {
            dataMap[KEY_PLANT_NAME] = "Tanaman tidak dapat di-identifikasi"
            dataMap[KEY_DESCRIPTION] = "Pastikan gambar jelas dan fokus pada satu jenis tanaman."
            dataMap[KEY_BENEFIT] = ""
            dataMap[KEY_WARNING] = ""
            dataMap[KEY_HERBAL_STATUS] = "Unknown"
            return dataMap
        }

        dataMap[KEY_PLANT_NAME] = parsePlantName(originalText)
        dataMap[KEY_DESCRIPTION] = parseSection(originalText, Section.DESCRIPTION)
        dataMap[KEY_BENEFIT] = parseSection(originalText, Section.BENEFIT)
        dataMap[KEY_WARNING] = parseSection(originalText, Section.WARNING)
        dataMap[KEY_HERBAL_STATUS] = parseSection(originalText, Section.HERBAL_STATUS)

        dataMap.putIfAbsent(KEY_DESCRIPTION, "")
        dataMap.putIfAbsent(KEY_BENEFIT, "")
        dataMap.putIfAbsent(KEY_WARNING, "")
        dataMap.putIfAbsent(KEY_HERBAL_STATUS, "Unknown")

        return dataMap

    }

    private enum class Section {
        DESCRIPTION, BENEFIT, WARNING, HERBAL_STATUS
    }

    private fun isUnidentifiedPlant(text: String): Boolean =
        Regex("""tanaman\s+tidak\s+dapat\s+di-?identifikasi""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    private fun parsePlantName(text: String): String {
        val namePattern = Regex(
            pattern = "🌿\\s*(.*?)\\s*\\*?Nama\\s+Ilmiah\\b",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return namePattern.find(text)?.destructured?.let { (name) -> name.trim() }
            ?: text.lines().firstOrNull()?.replace(Regex("[#*🌿]"), "")?.trim()
            ?: "Nama tidak ditemukan"
    }

    private fun parseSection(text: String, section: Section): String {
        val (pattern, fallback) = when (section) {
            Section.DESCRIPTION ->
                Regex(
                    "### 📝 Deskripsi(.*?)(?=### 🩺 Potensi Manfaat & Kegunaan|### ⚠️ Peringatan & Efek Samping|### Jenis Tanaman|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""

            Section.BENEFIT ->
                Regex(
                    "### 🩺 Potensi Manfaat & Kegunaan(.*?)(?=### 📝 Deskripsi|### ⚠️ Peringatan & Efek Samping|### Jenis Tanaman|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""

            Section.WARNING ->
                Regex(
                    "### ⚠️ Peringatan & Efek Samping(.*?)(?=### 📝 Deskripsi|### 🩺 Potensi Manfaat & Kegunaan|### Jenis Tanaman|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""

            Section.HERBAL_STATUS ->
                Regex(
                    "### Jenis Tanaman(.*?)(?=### 📝 Deskripsi|### 🩺 Potensi Manfaat & Kegunaan|### ⚠️ Peringatan & Efek Samping|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""
        }
        return pattern.find(text)?.destructured?.let { (content) ->
            content.replace("---", "").trim()
        } ?: fallback
    }
}
