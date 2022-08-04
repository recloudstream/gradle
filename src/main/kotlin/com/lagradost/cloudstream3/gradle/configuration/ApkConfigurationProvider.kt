package com.lagradost.cloudstream3.gradle.configuration

import com.lagradost.cloudstream3.gradle.ApkInfo
import com.lagradost.cloudstream3.gradle.createProgressLogger
import com.lagradost.cloudstream3.gradle.download
import com.lagradost.cloudstream3.gradle.getCloudstream
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.BaseDexFileReader
import com.googlecode.d2j.reader.MultiDexFileReader
import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import java.lang.Integer.parseInt
import java.net.URL
import java.nio.file.Files

class ApkConfigurationProvider : IConfigurationProvider {

    override val name: String
        get() = "apk"

    override fun provide(project: Project, dependency: Dependency) {
        val extension = project.extensions.getCloudstream()
        val apkinfo = ApkInfo(extension, dependency.version ?: "prerelease")
        extension.apkinfo = apkinfo

        apkinfo.cache.mkdirs()

        if (!apkinfo.apkFile.exists()) {
            project.logger.lifecycle("Downloading apk")

            val url = URL(apkinfo.url)

            url.download(apkinfo.apkFile, createProgressLogger(project, "Download apk"))
        }

        if (!apkinfo.jarFile.exists()) {
            project.logger.lifecycle("Converting apk to jar")

            val reader: BaseDexFileReader = MultiDexFileReader.open(Files.readAllBytes(apkinfo.apkFile.toPath()))
            Dex2jar.from(reader).topoLogicalSort().skipDebug(false).noCode(true).to(apkinfo.jarFile.toPath())
        }

        project.dependencies.add("compileOnly", project.files(apkinfo.jarFile))
    }
}