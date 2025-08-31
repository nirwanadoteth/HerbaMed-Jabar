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

    /**
     * Parses plant data from AI-generated Markdown text.
     *
     * @param text The Markdown text to parse.
     * @return Map with keys: plantName, description, benefit, warning.
     */
    @JvmStatic
    fun parsePlantData(text: String): Map<String, String> {
        val dataMap = mutableMapOf<String, String>()
        val originalText = text

        if (isUnidentifiedPlant(originalText)) {
            dataMap[KEY_PLANT_NAME] = "Tanaman tidak dapat di-identifikasi"
            dataMap[KEY_DESCRIPTION] = "Pastikan gambar jelas dan fokus pada satu jenis tanaman."
            dataMap[KEY_BENEFIT] = ""
            dataMap[KEY_WARNING] = ""
            return dataMap
        }

        dataMap[KEY_PLANT_NAME] = parsePlantName(originalText)
        dataMap[KEY_DESCRIPTION] = parseSection(originalText, Section.DESCRIPTION)
        dataMap[KEY_BENEFIT] = parseSection(originalText, Section.BENEFIT)
        dataMap[KEY_WARNING] = parseSection(originalText, Section.WARNING)

        dataMap.putIfAbsent(KEY_DESCRIPTION, "")
        dataMap.putIfAbsent(KEY_BENEFIT, "")
        dataMap.putIfAbsent(KEY_WARNING, "")

        return dataMap
    }

    private enum class Section {
        DESCRIPTION, BENEFIT, WARNING
    }

    private fun isUnidentifiedPlant(text: String): Boolean =
        text.contains("tanaman tidak dapat diidentifikasi", ignoreCase = true)

    private fun parsePlantName(text: String): String {
        val namePattern = Regex("🌿(.*?)\\*Nama Ilmiah", setOf(RegexOption.DOT_MATCHES_ALL))
        return namePattern.find(text)?.destructured?.let { (name) -> name.trim() }
            ?: text.lines().firstOrNull()?.replace(Regex("[#*🌿]"), "")?.trim()
            ?: "Nama tidak ditemukan"
    }

    private fun parseSection(text: String, section: Section): String {
        val (pattern, fallback) = when (section) {
            Section.DESCRIPTION ->
                Regex(
                    "### 📝 Deskripsi(.*?)(?=### 🩺 Potensi Manfaat & Kegunaan|### ⚠️ Peringatan & Efek Samping|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""
            Section.BENEFIT ->
                Regex(
                    "### 🩺 Potensi Manfaat & Kegunaan(.*?)(?=### 📝 Deskripsi|### ⚠️ Peringatan & Efek Samping|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""
            Section.WARNING ->
                Regex(
                    "### ⚠️ Peringatan & Efek Samping(.*?)(?=### 📝 Deskripsi|### 🩺 Potensi Manfaat & Kegunaan|$)",
                    setOf(RegexOption.DOT_MATCHES_ALL)
                ) to ""
        }
        return pattern.find(text)?.destructured?.let { (content) ->
            content.replace("---", "").trim()
        } ?: fallback
    }
}
