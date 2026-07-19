package com.lagradost.cloudstream3.gradle.tasks

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.Property

@CacheableTask
abstract class MakePluginsJsonTask : DefaultTask() {

    @get:Input
    abstract val repositoryName: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val nuvioOutputFile: RegularFileProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginEntryFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nuvioEntryFiles: ConfigurableFileCollection

    @TaskAction
    fun makePluginsJson() {
        val slurper = JsonSlurper()
        val entries = pluginEntryFiles.files
            .filter { it.exists() }
            .map { slurper.parse(it) }

        val json = JsonBuilder(entries).toPrettyString()
        outputFile.asFile.get().writeText(json)
        logger.lifecycle("Created ${outputFile.asFile.get()}")

        val nuvioEntries = nuvioEntryFiles.files
            .filter { it.exists() }
            .map { slurper.parse(it) }

        if (nuvioEntries.isNotEmpty()) {
            val manifest = mapOf(
                "name" to repositoryName.get(),
                "version" to "1.0.0",
                "scrapers" to nuvioEntries
            )

            val nuvioJson = JsonBuilder(manifest).toPrettyString()

            nuvioOutputFile.asFile.get().writeText(nuvioJson)
            logger.lifecycle("Created ${nuvioOutputFile.asFile.get()}")
        } else {
            nuvioOutputFile.asFile.get().delete()
        }
    }
}
