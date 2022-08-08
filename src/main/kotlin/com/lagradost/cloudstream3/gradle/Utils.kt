package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.entities.*
import groovy.json.JsonBuilder

fun Project.makeManifest(): PluginManifest {
    val extension = this.extensions.getCloudstream()

    require(this.version != "unspecified") {
        "No version is set"
    }

    require(extension.pluginClassName != null) {
        "No plugin class found, make sure your plugin class is annotated with @CloudstreamPlugin"
    }
    
    return PluginManifest(
        pluginClassName = extension.pluginClassName,
        name = this.name,
        pluginVersion = this.version.toString()
    )
}

fun Project.makePluginEntry(): PluginEntry {
    val extension = this.extensions.getCloudstream()

    require(this.version != "unspecified") {
        "No version is set"
    }

    val repo = extension.repository

    return PluginEntry(
        url = (if (repo == null) "" else repo.getRawLink("${this.name}.cs3", "builds")),
        status = extension.status.getOrElse(3),
        version = this.version.toString(),
        name = this.name,
        internalName = this.name,
        authors = extension.authors.getOrElse(listOf()),
        description = extension.description.orNull,
        repositoryUrl = (if (repo == null) null else repo.url),
        isAdult = extension.adult.getOrElse(false)
    )
}