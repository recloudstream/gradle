package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class MakePluginsJsonTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val pluginEntriesJson: Property<String>

    @TaskAction
    fun makePluginsJson() {
        outputFile.asFile.get().writeText(
            pluginEntriesJson.get()
        )

        logger.lifecycle("Created ${outputFile.asFile.get()}")
    }
}