package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.entities.PluginManifest
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
        pluginClassName = extension.pluginClassName!!,
        name = this.name,
        version = this.version.toString(),
        authors = extension.authors.getOrElse(listOf()),
        repositoryUrl = extension.repositoryUrl.orNull,
        description = extension.description.orNull,
        isAdult = extension.isAdult.getOrElse(false),
        status = extension.status.getOrElse(3)
    )
}