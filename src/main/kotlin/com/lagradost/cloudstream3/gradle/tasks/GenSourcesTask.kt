package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import jadx.api.impl.SimpleCodeWriter
import jadx.plugins.input.dex.DexInputPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.function.Function
import java.net.URL
import com.lagradost.cloudstream3.gradle.download
import com.lagradost.cloudstream3.gradle.createProgressLogger

abstract class GenSourcesTask : DefaultTask() {
    @TaskAction
    fun genSources() {
        val extension = project.extensions.getCloudstream()
        val apkinfo = extension.apkinfo!!

        val sourcesJarFile = apkinfo.cache.resolve("cloudstream-sources.jar")

        val url = URL("${apkinfo.urlPrefix}/app-sources.jar")

        url.download(sourcesJarFile, createProgressLogger(project, "Download sources"))
    }
}