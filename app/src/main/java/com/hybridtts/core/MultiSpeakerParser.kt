package com.hybridtts.core

data class DialogueLine(
    val speakerTag: String,
    val spokenText: String,
    val lineIndex: Int
)

object MultiSpeakerParser {

    private val SPEAKER_REGEX = Regex("^\\[([a-zA-Z0-9_\\s\\-]+)\\]\\s*:\\s*(.*)$", RegexOption.MULTILINE)

    fun parseDialogue(script: String): List<DialogueLine> {
        val lines = mutableListOf<DialogueLine>()
        val rawLines = script.lines()
        var currentSpeaker = "Narrator"
        var index = 0

        for (raw in rawLines) {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) continue

            val match = SPEAKER_REGEX.find(trimmed)
            if (match != null) {
                currentSpeaker = match.groupValues[1].trim()
                val text = match.groupValues[2].trim()
                if (text.isNotBlank()) {
                    lines.add(DialogueLine(currentSpeaker, text, index++))
                }
            } else {
                // Continuation of previous speaker line
                lines.add(DialogueLine(currentSpeaker, trimmed, index++))
            }
        }

        return lines
    }

    fun extractCharacterLineCounts(script: String): Map<String, Int> {
        val dialogues = parseDialogue(script)
        val counts = mutableMapOf<String, Int>()
        for (line in dialogues) {
            counts[line.speakerTag] = (counts[line.speakerTag] ?: 0) + 1
        }
        return counts
    }
}
