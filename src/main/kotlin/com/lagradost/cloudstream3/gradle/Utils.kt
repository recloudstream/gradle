package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.entities.PluginManifest
import groovy.json.JsonBuilder

fun Project.makeManifest(): PluginManifest {
    val extension = this.extensions.getCloudstream()

    return PluginManifest(
        pluginClassName = extension.pluginClassName!!,
        name = this.name,
        version = this.version.toString(),
        authors = extension.authors.get(),
        repositoryUrl = extension.repositoryUrl.get(),
        description = extension.description.get(),
        isAdult = extension.isAdult.get(),
        status = extension.status.get() ?: 3
    )
}