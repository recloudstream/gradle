package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CompilePluginJarTask : DefaultTask() {

    @get:Input
    abstract val hasCrossPlatformSupport: Property<Boolean>

    @get:InputFile
    abstract val pluginClassFile: RegularFileProperty

    @get:Internal
    abstract val pluginClassName: Property<String?>

    @get:Internal
	abstract val jarFileSize: Property<Long?>

    @get:InputFile
    abstract val jarInputFile: RegularFileProperty

    @get:OutputFile
    abstract val targetJarFile: RegularFileProperty

    @TaskAction
    fun compileJar() {
        if (pluginClassName.orNull == null) {
            val file = pluginClassFile.get().asFile
            if (file.exists()) {
                pluginClassName.set(file.readText())
            }
        }

        if (!hasCrossPlatformSupport.get()) return

        val jarFile = jarInputFile.get().asFile
        val targetFile = targetJarFile.get().asFile

        jarFile.copyTo(targetFile, overwrite = true)
        jarFileSize.set(jarFile.length())
        logger.lifecycle("Made Cloudstream cross-platform package at ${targetFile.absolutePath}")
    }
}
