package com.lagradost.cloudstream3.gradle.entities

data class PluginEntry(
    val url: String,
    val status: Int,
    val version: Int,
    val name: String,
    val internalName: String,
    val authors: List<String>,
    val description: String?,
    val repositoryUrl: String?,
    val adult: Boolean,
    val language: String?,
    val tvTypes: List<String>?,
    val iconUrl: String?,
    val apiVersion: Int
)