package edu.unikom.herbamedjabar.util

data class PlantData(
    val plantName: String,
    val description: String,
    val benefit: String,
    val warning: String,
    val herbalStatus: String = "Unknown"
)

/**
 * Utility object for parsing structured plant data from AI-generated Markdown text.
 *
 * Expected input format:
 * - Contains sections with emoji headers, e.g.:
 *   🌿 [Plant Name]*Nama Ilmiah
 *   ### 📝 Deskripsi
 *   ### 🩺 Potensi Manfaat & Kegunaan
 *   ### ⚠️ Peringatan & Efek Samping
 *   ### Jenis Tanaman
 *
 * Returns a map with keys: plantName, description, benefit, warning.
 * If the plant cannot be identified, returns a standard message.
 */
object PlantDataParser {

    /**
     * Parses plant data from AI-generated Markdown text.
     *
     * @param text The Markdown text to parse.
     * @return PlantData containing parsed fields.
     */
    fun parsePlantData(text: String): PlantData {
        val originalText = text

        if (isUnidentifiedPlant(originalText)) {
            return PlantData(
                plantName = "Tanaman tidak dapat di-identifikasi",
                description = "Pastikan gambar jelas dan fokus pada satu jenis tanaman.",
                benefit = "",
                warning = "",
                herbalStatus = "Unknown"
            )
        }

        val plantName = parsePlantName(originalText)
        val description = parseSection(originalText, Section.DESCRIPTION).ifBlank { "" }
        val benefit = parseSection(originalText, Section.BENEFIT).ifBlank { "" }
        val warning = parseSection(originalText, Section.WARNING).ifBlank { "" }
        val herbalStatus = parseSection(originalText, Section.HERBAL_STATUS).ifBlank { "Unknown" }

        return PlantData(
            plantName = plantName,
            description = description,
            benefit = benefit,
            warning = warning,
            herbalStatus = herbalStatus
        )
    }

    private enum class Section {
        DESCRIPTION, BENEFIT, WARNING, HERBAL_STATUS
    }

    private fun isUnidentifiedPlant(text: String): Boolean =
        Regex("""tanaman\s+tidak\s+dapat\s+di-?identifikasi""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    private fun parsePlantName(text: String): String {
        val namePattern = Regex(
            pattern = """^(?:#{1,6}\s*)?(?:🌿\s*)?(.*?)\s*\*?Nama\s+Ilmiah\b""",
            options = setOf(
                RegexOption.DOT_MATCHES_ALL,
                RegexOption.IGNORE_CASE,
                RegexOption.MULTILINE
            )
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
