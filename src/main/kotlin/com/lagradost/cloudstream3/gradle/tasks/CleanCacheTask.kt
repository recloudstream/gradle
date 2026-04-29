package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CleanCacheTask : DefaultTask() {

    @get:Internal abstract val jarFile: RegularFileProperty

    @TaskAction
    fun clean() {
        val file = jarFile.asFile.get()
        if (file.exists()) file.delete() else {
            logger.lifecycle("JAR file does not exist; nothing to clean.")
        }
    }
}
