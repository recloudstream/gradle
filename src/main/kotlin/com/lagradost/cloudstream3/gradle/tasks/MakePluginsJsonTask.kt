package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.findCloudstream
import com.lagradost.cloudstream3.gradle.makePluginEntry
import com.lagradost.cloudstream3.gradle.entities.PluginEntry
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.LinkedList
import java.lang.Thread

abstract class MakePluginsJsonTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun makePluginsJson() {
        val lst = LinkedList<PluginEntry>()

        for (subproject in project.allprojects) {
            subproject.extensions.findCloudstream() ?: continue

            lst.add(subproject.makePluginEntry())
        }

        outputFile.asFile.get().writeText(
            JsonBuilder(
                lst,
                JsonGenerator.Options()
                    .excludeNulls()
                    .build()
            ).toPrettyString()
        )

        logger.lifecycle("Created ${outputFile.asFile.get()}")
    }
}