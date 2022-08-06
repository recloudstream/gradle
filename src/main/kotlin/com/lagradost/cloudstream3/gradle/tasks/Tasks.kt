package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.entities.PluginManifest
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import groovy.json.JsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile 

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

        val kotlinTask = project.tasks.findByName("compileDebugKotlin") as KotlinCompile?
        if (kotlinTask != null) {
            it.dependsOn(kotlinTask)
            it.input.from(kotlinTask.destinationDirectory)
        }

        val javacTask = project.tasks.findByName("compileDebugJavaWithJavac") as AbstractCompile?
        if (javacTask != null) {
            it.dependsOn(javacTask)
            it.input.from(javacTask.destinationDirectory)
        }

        it.outputFile.set(intermediates.resolve("classes.dex"))
    }

    project.afterEvaluate {
        project.tasks.register("make", Zip::class.java) {
            val compileDexTask = compileDex.get()
            it.dependsOn(compileDexTask)

            val manifestFile = intermediates.resolve("manifest.json")
            it.from(manifestFile)
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

                manifestFile.writeText(
                    JsonBuilder(
                        PluginManifest(
                            pluginClassName = extension.pluginClassName!!,
                            name = project.name
                        )
                    ).toPrettyString()
                )
            }

            it.from(compileDexTask.outputFile)
            
            val zip = it as Zip
            //zip.dependsOn(compileResources.get())
            zip.isPreserveFileTimestamps = false
            zip.archiveBaseName.set(project.name)
            zip.archiveVersion.set("")
            zip.destinationDirectory.set(project.buildDir)

            it.doLast { task ->
                task.logger.lifecycle("Made Cloudstream package at ${task.outputs.files.singleFile}")
            }
        }
    }

    project.tasks.register("cleanCache", CleanCacheTask::class.java) {
        it.group = TASK_GROUP
    }

    project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
        it.group = TASK_GROUP
        it.dependsOn("make")
    }
}