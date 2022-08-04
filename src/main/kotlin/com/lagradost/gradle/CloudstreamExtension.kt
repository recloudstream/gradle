package com.lagradost.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class CloudstreamExtension @Inject constructor(project: Project) {
    val projectType: Property<ProjectType> =
        project.objects.property(ProjectType::class.java).convention(ProjectType.PLUGIN)

    val userCache = project.gradle.gradleUserHomeDir.resolve("caches").resolve("cloudstream")

    var discord: ApkInfo? = null
}

class ApkInfo(extension: CloudstreamExtension, val version: Int) {
    val cache = extension.userCache.resolve("cloudstream")

    val apkFile = cache.resolve("cloudstream-$version.apk")
    val jarFile = cache.resolve("cloudstream-$version.jar")
}

fun ExtensionContainer.getCloudstream(): CloudstreamExtension {
    return getByName("cloudstream") as CloudstreamExtension
}