package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CompilePluginJarTask : DefaultTask() {

    @get:Input
    abstract val hasCrossPlatformSupport: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginClassFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jarInputFile: RegularFileProperty

    @get:OutputFile
    abstract val targetJarFile: RegularFileProperty

    @TaskAction
    fun compileJar() {
        if (!hasCrossPlatformSupport.get()) return

        val jarFile = jarInputFile.get().asFile
        val targetFile = targetJarFile.get().asFile

        jarFile.copyTo(targetFile, overwrite = true)
        logger.lifecycle("Made CloudStream cross-platform package at ${targetFile.absolutePath}")
    }
}
