package com.example.vinfo.domain.model

import java.security.MessageDigest
import java.util.Locale

fun buildTrackId(artist: String, title: String): String {
    val normalized = "${artist.trim().lowercase(Locale.US)}|${title.trim().lowercase(Locale.US)}"
    val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
    return digest.take(16).joinToString("") { byte -> "%02x".format(byte) }
}
