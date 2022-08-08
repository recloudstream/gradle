package com.lagradost.cloudstream3.gradle.entities

data class PluginManifest(
    val pluginClassName: String?,
    val name: String,
    val pluginVersion: String
)