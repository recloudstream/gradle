package com.lagradost.cloudstream3.gradle.entities

data class PluginManifest(
    val pluginClassName: String,
    val name: String,
    val version: String,
    val authors: List<String>,
    val sourceUrl: String?,
    val updateUrl: String?,
)