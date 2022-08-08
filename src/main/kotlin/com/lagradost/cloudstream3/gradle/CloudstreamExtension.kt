package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

abstract class CloudstreamExtension @Inject constructor(project: Project) {
    val userCache = project.gradle.gradleUserHomeDir.resolve("caches").resolve("cloudstream")

    val apiVersion = 1

    var apkinfo: ApkInfo? = null
        internal set

    var repository: Repo? = null
        internal set

    fun overrideUrlPrefix(url: String) {
        if (apkinfo == null) {
            apkinfo = ApkInfo(this, "pre-release")
        }
        apkinfo!!.urlPrefix = url
    }

    fun setRepo(user: String, repo: String) {
        repository = Repo(user, repo)
    }
    fun setRepo(identifier: String) {
        val split = identifier
            .removePrefix("https://")
            .removePrefix("github.com")
            .removeSurrounding("/")
            .split("/")
        repository = Repo(split[0], split[1])
    }

    internal var pluginClassName: String? = null

    val description: Property<String> = project.objects.property(String::class.java)
    val authors: ListProperty<String> = project.objects.listProperty(String::class.java)
    val adult: Property<Boolean> = project.objects.property(Boolean::class.java)
    val status: Property<Int> = project.objects.property(Int::class.java)
}

class ApkInfo(extension: CloudstreamExtension, release: String) {
    val cache = extension.userCache.resolve("cloudstream")

    var urlPrefix = "https://github.com/recloudstream/cloudstream/releases/download/${release}"
    val jarFile = cache.resolve("cloudstream.jar")
}

class Repo(val user: String, val repo: String) {
    val url: String
        get() = "https://github.com/${user}/${repo}"

    fun getRawLink(filename: String, branch: String): String {
        return "https://raw.githubusercontent.com/${user}/${repo}/${branch}/${filename}"
    }
}

fun ExtensionContainer.getCloudstream(): CloudstreamExtension {
    return getByName("cloudstream") as CloudstreamExtension
}

fun ExtensionContainer.findCloudstream(): CloudstreamExtension? {
    return findByName("cloudstream") as CloudstreamExtension?
}