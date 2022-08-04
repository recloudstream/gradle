package com.lagradost.gradle.tasks

import com.cloudstream.gradle.getCloudstream
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import groovy.json.JsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.AbstractCompile

const val TASK_GROUP = "cloudstream"

fun registerTasks(project: Project) {
    val extension = project.extensions.getCloudstream()
    val intermediates = project.buildDir.resolve("intermediates")

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        it.group = TASK_GROUP
    }

    val pluginClassFile = intermediates.resolve("pluginClass")

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) {
        it.group = TASK_GROUP

        it.pluginClassFile.set(pluginClassFile)

        for (name in arrayOf("compileDebugJavaWithJavac", "compileDebugKotlin")) {
            val task = project.tasks.findByName(name) as AbstractCompile?
            if (task != null) {
                it.dependsOn(task)
                it.input.from(task.destinationDirectory)
            }
        }

        it.outputFile.set(intermediates.resolve("classes.dex"))
    }

    project.afterEvaluate {
        project.tasks.register("make", Copy::class.java)
        {
            val compileDexTask = compileDex.get()
            it.dependsOn(compileDexTask)

            it.doFirst {
                require(project.version != "unspecified") {
                    "No version is set"
                }

                if (extension.pluginClassName == null) {
                    if (pluginClassFile.exists()) {
                        extension.pluginClassName = pluginClassFile.readText()
                    }
                }

                require(extension.pluginClassName != null) {
                    "No plugin class found, make sure your plugin class is annotated with @CloudstreamPlugin"
                }
            }

            it.from(compileDexTask.outputFile)
            it.into(project.buildDir)
            it.rename { return@rename project.name }
        }
    }
}