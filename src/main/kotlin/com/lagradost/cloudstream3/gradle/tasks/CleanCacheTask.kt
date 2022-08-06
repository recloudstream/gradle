package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.nio.charset.StandardCharsets

abstract class CleanCacheTask : DefaultTask() {
    @TaskAction
    fun cleanCache() {
        val extension = project.extensions.getCloudstream()
        val apkinfo = extension.apkinfo
        if (apkinfo == null) return;

        if (apkinfo.jarFile.exists()) {
            apkinfo.jarFile.delete()
        }
    }
}