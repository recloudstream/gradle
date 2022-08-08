package com.lagradost.cloudstream3.gradle.entities

data class PluginManifest(
    val pluginClassName: String,
    val name: String,
    val version: String,
    val authors: List<String>,
    val repositoryUrl: String?,
    val description: String?,
    val isAdult: Boolean,
    val status: Int
)