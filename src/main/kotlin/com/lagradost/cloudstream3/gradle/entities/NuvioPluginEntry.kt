package com.lagradost.cloudstream3.gradle.entities

data class NuvioPluginEntry(
    val id: String,
    val name: String,
    val description: String?,
    /** Version code as in "1.0.0" */
    val version: String,
    val author: String?,
    /** "movie", "tv",  */
    val supportedTypes: List<String>,
    /**  "ProviderName.js" */
    val filename: String,
    val enabled: Boolean,
    /** "mkv", "m3u8", "mp4", "mpd" */
    val formats: List<String>?,
    /** Url */
    val logo: String?,
    /** "en", "hin" */
    val contentLanguage: List<String>?,
    /** "ios" */
    val disabledPlatforms: List<String>?,
    val supportsExternalPlayer: Boolean?,
    val hasSettings: Boolean?,
)