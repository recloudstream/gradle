package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.download
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import java.net.URI
import javax.inject.Inject

@CacheableTask
abstract class GenerateSourcesTask : DefaultTask() {

    @get:Inject
    abstract val progressLoggerFactory: ProgressLoggerFactory

    @get:Input
    abstract val urlPrefix: Property<String>

    @get:OutputFile
    abstract val sourcesJarFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val logger = progressLoggerFactory
            .newOperation("Download sources")
            .apply { description = "Download sources" }

        val url = URI("${urlPrefix.get()}/app-sources.jar").toURL()
        url.download(sourcesJarFile.get().asFile, logger)
    }
}
