package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.findCloudstream
import com.lagradost.cloudstream3.gradle.makeManifest
import com.lagradost.cloudstream3.gradle.entities.PluginManifest
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.LinkedList

abstract class MakePluginsJsonTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun makePluginsJson() {
        val lst = LinkedList<PluginManifest>()

        for (subproject in project.allprojects) {
            val cloudstream = subproject.extensions.findCloudstream() ?: continue

            lst.add(subproject.makeManifest())
        }

        outputFile.asFile.get().writeText(
            JsonBuilder(
                lst,
                JsonGenerator.Options()
                    .excludeNulls()
                    .build()
            ).toString()
        )
    }
}