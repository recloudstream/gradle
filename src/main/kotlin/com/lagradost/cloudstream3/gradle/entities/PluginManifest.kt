package com.lagradost.cloudstream3.gradle.entities

data class PluginManifest(
    val pluginClassName: String?,
    val name: String,
    val version: Int,
    val requiresResources: Boolean
)