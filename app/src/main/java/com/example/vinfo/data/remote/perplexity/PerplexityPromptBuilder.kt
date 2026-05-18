package com.example.vinfo.data.remote.perplexity

object PerplexityPromptBuilder {

    fun buildSystemPrompt(): String {
        return """
            You are a music metadata assistant.
            Return only valid JSON with these keys:
            artist, title, album, primary_genre, secondary_genre, genre_source, rym_rating, critics_summary, interview_summary, listening_guide, samples_used.
            Do not wrap the JSON in markdown.
            Use concise but information-rich Korean descriptions for summaries.
        """.trimIndent()
    }

    fun buildUserPrompt(artist: String, title: String, album: String?): String {
        return buildString {
            appendLine("Analyze the track and return the JSON payload only.")
            appendLine("Artist: $artist")
            appendLine("Title: $title")
            appendLine("Album: ${album.orEmpty()}")
        }.trim()
    }
}