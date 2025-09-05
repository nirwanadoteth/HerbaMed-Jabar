package edu.unikom.herbamedjabar.util

data class PlantData(
    val plantName: String,
    val description: String,
    val benefit: String,
    val warning: String,
    val isHerbal: Boolean,
)

/**
 * Utility object for parsing structured plant data from AI-generated Markdown text.
 *
 * Expected input format:
 * - Contains sections with emoji headers, e.g.: 🌿 [Plant Name]*Nama Ilmiah
 *
 * ### 📝 Deskripsi
 *
 * ### 🩺 Potensi Manfaat & Kegunaan
 *
 * ### ⚠️ Peringatan & Efek Samping
 *
 * ### Jenis Tanaman
 *
 * Returns a PlantData with fields: plantName, description, benefit, warning, herbalStatus. If the
 * plant cannot be identified, returns a standard placeholder response.
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
                isHerbal = false,
            )
        }

        val plantName = parsePlantName(originalText)
        val description = parseSection(originalText, Section.DESCRIPTION).ifBlank { "" }
        val benefit = parseSection(originalText, Section.BENEFIT).ifBlank { "" }
        val warning = parseSection(originalText, Section.WARNING).ifBlank { "" }
        val herbalStatus = parseSection(originalText, Section.HERBAL_STATUS).ifBlank { "" }
        val isHerbal = parsePlantType(herbalStatus)

        return PlantData(
            plantName = plantName,
            description = description,
            benefit = benefit,
            warning = warning,
            isHerbal = isHerbal,
        )
    }

    private enum class Section {
        DESCRIPTION,
        BENEFIT,
        WARNING,
        HERBAL_STATUS,
    }

    private fun isUnidentifiedPlant(text: String): Boolean =
        Regex("""tanaman\s+tidak\s+dapat\s+di-?identifikasi""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)

    private fun parsePlantName(text: String): String {
        val namePattern =
            Regex(
                pattern = """^(?:#{1,6}\s*)?(?:🌿\s*)?(.*?)\s*\*?Nama\s+Ilmiah\b""",
                options =
                    setOf(
                        RegexOption.DOT_MATCHES_ALL,
                        RegexOption.IGNORE_CASE,
                        RegexOption.MULTILINE,
                    ),
            )
        return namePattern.find(text)?.destructured?.let { (name) -> name.trim() }
            ?: text.lineSequence().firstOrNull()?.replace(Regex("""^[#*\s🌿]+"""), "")?.trim()
            ?: ""
    }

    private fun parseSection(text: String, section: Section): String {
        val opts =
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        val (pattern, fallback) =
            when (section) {
                Section.DESCRIPTION ->
                    Regex(
                        pattern =
                            """^#{2,6}\s*(?:📝\s*)?Deskripsi\b(.*?)(?=^#{2,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b|^#{2,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b|^#{2,6}\s*Jenis\s+Tanaman\b|\z)""",
                        options = opts,
                    ) to ""

                Section.BENEFIT ->
                    Regex(
                        pattern =
                            """^#{2,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b(.*?)(?=^#{2,6}\s*(?:📝\s*)?Deskripsi\b|^#{2,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b|^#{2,6}\s*Jenis\s+Tanaman\b|\z)""",
                        options = opts,
                    ) to ""

                Section.WARNING ->
                    Regex(
                        pattern =
                            """^#{2,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b(.*?)(?=^#{2,6}\s*(?:📝\s*)?Deskripsi\b|^#{2,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b|^#{2,6}\s*Jenis\s+Tanaman\b|\z)""",
                        options = opts,
                    ) to ""

                Section.HERBAL_STATUS ->
                    Regex(
                        pattern =
                            """^#{2,6}\s*Jenis\s+Tanaman\b(.*?)(?=^#{2,6}\s*(?:📝\s*)?Deskripsi\b|^#{2,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b|^#{2,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b|\z)""",
                        options = opts,
                    ) to ""
            }
        return pattern.find(text)?.destructured?.let { (content) ->
            content.replace(Regex("""(?m)^\s*---\s*$"""), "").trim()
        } ?: fallback
    }

    private fun parsePlantType(text: String): Boolean = text.replace("-", " ").trim().equals("herbal", ignoreCase = true)
}
