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
            val cloudstream = subproject.extensions.findCloudstream() ?: continue

            /*
                bruh why does gradle ignore task order
                forcing me to do jank like this
            */
            var timeout = 10000 // 10 s timeout
            while (cloudstream.fileSize == null) {
                Thread.sleep(100)
                timeout -= 100
                if (timeout <= 0)
                    throw RuntimeException("Timeout while fetching fileSize for ${subproject.name}")
            }

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