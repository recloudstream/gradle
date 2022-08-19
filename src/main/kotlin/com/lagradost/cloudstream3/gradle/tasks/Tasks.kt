package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.entities.PluginManifest
import com.lagradost.cloudstream3.gradle.makeManifest
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.lagradost.cloudstream3.gradle.findCloudstream

const val TASK_GROUP = "cloudstream"

fun registerTasks(project: Project) {
    val extension = project.extensions.getCloudstream()
    val intermediates = project.buildDir.resolve("intermediates")

    if (project.rootProject.tasks.findByName("makePluginsJson") == null) {
        project.rootProject.tasks.register("makePluginsJson", MakePluginsJsonTask::class.java) {
            it.group = TASK_GROUP
            
            it.outputs.upToDateWhen { false }

            it.outputFile.set(it.project.buildDir.resolve("plugins.json"))
        }
    }

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

    val compileResources = project.tasks.register("compileResources", CompileResourcesTask::class.java) {
        it.group = TASK_GROUP

        val processManifestTask = project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
        it.dependsOn(processManifestTask)

        val android = project.extensions.getByName("android") as BaseExtension
        it.input.set(android.sourceSets.getByName("main").res.srcDirs.single())
        it.manifestFile.set(processManifestTask.manifestOutputFile)

        it.outputFile.set(intermediates.resolve("res.apk"))

        it.doLast { _ ->
            val resApkFile = it.outputFile.asFile.get()

            if (resApkFile.exists()) {
                project.tasks.named("make", AbstractCopyTask::class.java) {
                    it.from(project.zipTree(resApkFile)) { copySpec ->
                        copySpec.exclude("AndroidManifest.xml")
                    }
                }
            }
        }
    }

    project.afterEvaluate {
        val make = project.tasks.register("make", Zip::class.java) {
            val compileDexTask = compileDex.get()
            it.dependsOn(compileDexTask)

            val manifestFile = intermediates.resolve("manifest.json")
            it.from(manifestFile)
            it.doFirst {
                if (extension.pluginClassName == null) {
                    if (pluginClassFile.exists()) {
                        extension.pluginClassName = pluginClassFile.readText()
                    }
                }

                manifestFile.writeText(
                    JsonBuilder(project.makeManifest(),
                        JsonGenerator.Options()
                        .excludeNulls()
                        .build()
                    ).toString()
                )
            }

            it.from(compileDexTask.outputFile)
            
            val zip = it as Zip
            if (extension.requiresResources) {
                zip.dependsOn(compileResources.get())
            }
            zip.isPreserveFileTimestamps = false
            zip.archiveBaseName.set(project.name)
            zip.archiveExtension.set("cs3")
            zip.archiveVersion.set("")
            zip.destinationDirectory.set(project.buildDir)

            it.doLast { task ->
                extension.fileSize = task.outputs.files.singleFile.length()
                task.logger.lifecycle("Made Cloudstream package at ${task.outputs.files.singleFile}")
            }
        }
        project.rootProject.tasks.getByName("makePluginsJson").dependsOn(make)  
    }

    project.tasks.register("cleanCache", CleanCacheTask::class.java) {
        it.group = TASK_GROUP
    }

    project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
        it.group = TASK_GROUP
        it.dependsOn("make")
    }
}
