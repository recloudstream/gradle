package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

@CacheableTask
// Source task, since it skips when no input JS file
abstract class CompilePluginJsTask : DefaultTask() {
    @get:Input
    abstract val nuvioEnabled: Property<Boolean>

    @get:Input
    abstract val pluginName: Property<String>

    @get:InputFile
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginClassFile: RegularFileProperty

    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputFile
    abstract val targetJsFile: RegularFileProperty

    @TaskAction
    fun compileJs() {
        val jsFile = this.sourceDir.asFileTree.find { file ->
            file.name.endsWith("${pluginName.get()}.js")
        } ?: return

        val targetFile = targetJsFile.asFile.get()
        jsFile.copyTo(targetFile, overwrite = true)

        val pluginClassName = pluginClassFile.map { it.asFile.readText() }.get()

        // Always load the plugin when JS file is loaded.
        val initText = """
            this.${pluginClassName}.prototype.load();
        """.trimIndent()
        targetFile.appendText(initText)

        // Load a Nuvio bridge to export the necessary Nuvio methods
        /*
        if (this.nuvioEnabled.get()) {
            val bridgeText = """
            this.NuvioBridge.init("$pluginClassName");
            """.trimIndent()
            targetFile.appendText(bridgeText)
            logger.lifecycle("Appended CloudStream Nuvio bridge")
        }
        */

        logger.lifecycle("Made CloudStream js package at ${targetFile.absolutePath}")
    }
}
