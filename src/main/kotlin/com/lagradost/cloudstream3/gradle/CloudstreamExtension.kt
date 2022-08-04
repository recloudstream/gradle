package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class CloudstreamExtension @Inject constructor(project: Project) {
    val userCache = project.gradle.gradleUserHomeDir.resolve("caches").resolve("cloudstream")

    var apkinfo: ApkInfo? = null
        internal set

    fun overrideUrl(url: String) {
        apkinfo!!.url = url
    }

    internal var pluginClassName: String? = null
}

class ApkInfo(extension: CloudstreamExtension, release: String) {
    val cache = extension.userCache.resolve("cloudstream")

    var url = "https://github.com/recloudstream/cloudstream/releases/download/${release}/app-debug.apk"
    val apkFile = cache.resolve("cloudstream.apk")
    val jarFile = cache.resolve("cloudstream.jar")
}

fun ExtensionContainer.getCloudstream(): CloudstreamExtension {
    return getByName("cloudstream") as CloudstreamExtension
}

fun ExtensionContainer.findCloudstream(): CloudstreamExtension? {
    return findByName("cloudstream") as CloudstreamExtension?
}