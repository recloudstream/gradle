package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

@CacheableTask
// Source task, since it skips when no input JS file
abstract class CompilePluginJsTask : SourceTask() {
    @get:Input
    abstract val hasCrossPlatformSupport: Property<Boolean>

    @get:OutputFile
    abstract val targetJsFile: RegularFileProperty

    @TaskAction
    fun compileJs() {
        if (!hasCrossPlatformSupport.get()) return

        val jsFile = this.source.singleFile
        val targetFile = targetJsFile.asFile.get()

        jsFile.copyTo(targetFile, overwrite = true)
        logger.lifecycle("Made CloudStream js package at ${targetFile.absolutePath}")
    }
}
