package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.entities.PluginManifest
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

abstract class GenerateManifestTask : DefaultTask() {

    @get:InputFile
    @get:SkipWhenEmpty
    abstract val pluginClassFile: RegularFileProperty

    @get:Input
    abstract val pluginName: Property<String>

    @get:Input
    abstract val pluginVersion: Property<Int>

    @get:Input
    abstract val requiresResources: Property<Boolean>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val manifest = PluginManifest(
            pluginClassName = pluginClassFile.asFile.get().readText().trim(),
            name = pluginName.get(),
            version = pluginVersion.get(),
            requiresResources = requiresResources.get()
        )

        outputFile.asFile.get().writeText(
            JsonBuilder(
                manifest,
                JsonGenerator.Options().excludeNulls().build()
            ).toString()
        )
    }
}
