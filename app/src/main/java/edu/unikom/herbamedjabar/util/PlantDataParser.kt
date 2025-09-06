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
 * Returns a PlantData with fields: plantName, description, benefit, warning, isHerbal. If the plant
 * cannot be identified, returns a standard placeholder response.
 */
object PlantDataParser {

    /**
     * Parses plant data from AI-generated Markdown text.
     *
     * @param text The Markdown text to parse.
     * @return PlantData containing parsed fields.
     */
    fun parsePlantData(text: String): PlantData {
        if (isUnidentifiedPlant(text)) {
            return PlantData(
                plantName = "Tanaman tidak dapat di-identifikasi",
                description = "Pastikan gambar jelas dan fokus pada satu jenis tanaman.",
                benefit = "",
                warning = "",
                isHerbal = false,
            )
        }

        val plantName = parsePlantName(text)
        val description = parseSection(text, Section.DESCRIPTION).ifBlank { "" }
        val benefit = parseSection(text, Section.BENEFIT).ifBlank { "" }
        val warning = parseSection(text, Section.WARNING).ifBlank { "" }
        val herbalStatus = parseSection(text, Section.HERBAL_STATUS).ifBlank { "" }
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

    private val REGEX_OPTIONS =
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE, RegexOption.MULTILINE)

    private val DESCRIPTION_REGEX =
        Regex(
            pattern =
                """^#{1,6}\s*(?:📝\s*)?Deskripsi\b(?:\s*[:：])?(.*?)(?=^#{1,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b|^#{1,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b|^#{1,6}\s*Jenis\s+Tanaman\b|\z)""",
            options = REGEX_OPTIONS,
        )

    private val BENEFIT_REGEX =
        Regex(
            pattern =
                """^#{1,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b(?:\s*[:：])?(.*?)(?=^#{1,6}\s*(?:📝\s*)?Deskripsi\b|^#{1,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b|^#{1,6}\s*Jenis\s+Tanaman\b|\z)""",
            options = REGEX_OPTIONS,
        )

    private val WARNING_REGEX =
        Regex(
            pattern =
                """^#{1,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b(?:\s*[:：])?(.*?)(?=^#{1,6}\s*(?:📝\s*)?Deskripsi\b|^#{1,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b|^#{1,6}\s*Jenis\s+Tanaman\b|\z)""",
            options = REGEX_OPTIONS,
        )

    private val HERBAL_STATUS_REGEX =
        Regex(
            pattern =
                """^#{1,6}\s*Jenis\s+Tanaman\b(?:\s*[:：])?(.*?)(?=^#{1,6}\s*(?:📝\s*)?Deskripsi\b|^#{1,6}\s*(?:🩺\s*)?Potensi\s+Manfaat(?:\s*&\s*Kegunaan)?\b|^#{1,6}\s*(?:⚠️\s*)?Peringatan(?:\s*&\s*Efek\s*Samping)?\b|\z)""",
            options = REGEX_OPTIONS,
        )

    private val SECTION_REGEXES =
        mapOf(
            Section.DESCRIPTION to DESCRIPTION_REGEX,
            Section.BENEFIT to BENEFIT_REGEX,
            Section.WARNING to WARNING_REGEX,
            Section.HERBAL_STATUS to HERBAL_STATUS_REGEX,
        )

    private val UNIDENTIFIED_REGEX =
        Regex("""tanaman\s+tidak\s+(?:dapat\s+)?(?:di-?|ter)identifikasi""", RegexOption.IGNORE_CASE)

    private fun isUnidentifiedPlant(text: String): Boolean =
        UNIDENTIFIED_REGEX.containsMatchIn(text)

    private fun parsePlantName(text: String): String {
        val namePattern =
            Regex(
                pattern = """^(?:#{1,6}\s*)?(?:🌿\s*)?([^\r\n#*]+?)\s*\*?\s*Nama\s+Ilmiah(?:\s*[:：])?\b""",
                options = setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
            )
        return namePattern.find(text)?.destructured?.let { (name) -> name.trim() }
            ?: text.lineSequence().firstOrNull()?.replace(Regex("""^[#*\s🌿]+"""), "")?.trim()
            ?: ""
    }

    private fun parseSection(text: String, section: Section): String {
        val regex = SECTION_REGEXES[section] ?: return ""
        return regex.find(text)?.destructured?.let { (content) ->
            content.replace(Regex("""(?m)^\s*(?:[-*_]{3,})\s*$"""), "").trim()
        } ?: ""
    }

    private fun parsePlantType(text: String): Boolean {
        val normalized = text.replace("-", " ").lowercase()
        val negative = Regex("""\b(?:non|bukan|tidak)\s+herbal\b""").containsMatchIn(normalized)
        if (negative) return false
        return Regex("""\bherbal\b""").containsMatchIn(normalized)
    }
}
