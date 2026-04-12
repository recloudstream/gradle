package com.lagradost.cloudstream3.gradle.tasks

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

abstract class MakePluginsJsonTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginEntryFiles: ConfigurableFileCollection

    @TaskAction
    fun makePluginsJson() {
        val slurper = JsonSlurper()
        val entries = pluginEntryFiles.files
            .filter { it.exists() }
            .map { slurper.parse(it) }

        val json = JsonBuilder(entries).toPrettyString()
        outputFile.asFile.get().writeText(json)
        logger.lifecycle("Created ${outputFile.asFile.get()}")
    }
}
