package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.makeManifest
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.nio.charset.StandardCharsets
import groovy.json.JsonBuilder

abstract class DumpManifestTask : DefaultTask() {
    @TaskAction
    fun dumpManifest() {
        val manifestFile = project.buildDir.resolve("${project.name}.json")

        manifestFile.writeText(
            JsonBuilder(project.makeManifest()).toPrettyString()
        )
    }
}