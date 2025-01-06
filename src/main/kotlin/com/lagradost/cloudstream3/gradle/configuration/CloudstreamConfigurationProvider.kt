package com.lagradost.cloudstream3.gradle.configuration

import com.lagradost.cloudstream3.gradle.ApkInfo
import com.lagradost.cloudstream3.gradle.createProgressLogger
import com.lagradost.cloudstream3.gradle.download
import com.lagradost.cloudstream3.gradle.getCloudstream
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.lang.Integer.parseInt
import java.net.URI
import java.nio.file.Files

class CloudstreamConfigurationProvider : IConfigurationProvider {

    override val name: String
        get() = "cloudstream"

    override fun provide(project: Project, dependency: Dependency) {
        val extension = project.extensions.getCloudstream()
        if (extension.apkinfo == null) {
            extension.apkinfo = ApkInfo(extension, dependency.version ?: "pre-release")
        }
        val apkinfo = extension.apkinfo!!

        apkinfo.cache.mkdirs()

        if (!apkinfo.jarFile.exists()) {
            project.logger.lifecycle("Fetching JAR")

            val url = URI("${apkinfo.urlPrefix}/classes.jar").toURL()
            url.download(apkinfo.jarFile, createProgressLogger(project, "Download JAR"))
        }

        project.dependencies.add("compileOnly", project.files(apkinfo.jarFile))
    }
}