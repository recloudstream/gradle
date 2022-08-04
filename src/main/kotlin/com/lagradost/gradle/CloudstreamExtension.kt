package com.lagradost.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class CloudstreamExtension @Inject constructor(project: Project) {
    val userCache = project.gradle.gradleUserHomeDir.resolve("caches").resolve("cloudstream")


    var apkinfo: ApkInfo? = null
        internal set

    internal var pluginClassName: String? = null
}

class ApkInfo(extension: CloudstreamExtension, val version: Int) {
    val cache = extension.userCache.resolve("cloudstream")

    val apkFile = cache.resolve("cloudstream-$version.apk")
    val jarFile = cache.resolve("cloudstream-$version.jar")
}

fun ExtensionContainer.getCloudstream(): CloudstreamExtension {
    return getByName("cloudstream") as CloudstreamExtension
}

fun ExtensionContainer.findCloudstream(): CloudstreamExtension? {
    return findByName("cloudstream") as CloudstreamExtension?
}