package com.lagradost.cloudstream3.gradle.tasks

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.makeManifest
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

const val TASK_GROUP = "cloudstream"

fun registerTasks(project: Project) {
    val extension = project.extensions.getCloudstream()
    val intermediatesDir = project.layout.buildDirectory.dir("intermediates")

    if (project.rootProject.tasks.findByName("makePluginsJson") == null) {
        project.rootProject.tasks.register("makePluginsJson", MakePluginsJsonTask::class.java) {
            it.group = TASK_GROUP

            it.outputs.upToDateWhen { false }

            it.outputFile.set(it.project.layout.buildDirectory.file("plugins.json"))
        }
    }

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        it.group = TASK_GROUP
    }

    val pluginClassFile = intermediatesDir.map { it.file("pluginClass") }

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) {
        it.group = TASK_GROUP

        it.pluginClassFile.set(pluginClassFile)

        val kotlinTask = project.tasks.findByName("compileDebugKotlin") as KotlinCompile?
        if (kotlinTask != null) {
            it.dependsOn(kotlinTask)
            it.input.from(kotlinTask.destinationDirectory)
        }

        // This task does not seem to be required for a successful cs3 file

//        val javacTask = project.tasks.findByName("compileDebugJavaWithJavac") as AbstractCompile?
//        if (javacTask != null) {
//            it.dependsOn(javacTask)
//            it.input.from(javacTask.destinationDirectory)
//        }

        it.outputFile.set(intermediatesDir.map { it.file("classes.dex") })
    }

    val compileResources =
        project.tasks.register("compileResources", CompileResourcesTask::class.java) {
            it.group = TASK_GROUP

            val processManifestTask =
                project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
            it.dependsOn(processManifestTask)

            val android = project.extensions.getByName("android") as BaseExtension
            it.input.set(android.sourceSets.getByName("main").res.srcDirs.single())
            it.manifestFile.set(processManifestTask.manifestOutputFile)

            it.outputFile.set(intermediatesDir.map { it.file("res.apk") })

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

    val compilePluginJar = project.tasks.register("compilePluginJar") {
        it.group = TASK_GROUP
        it.dependsOn("createFullJarDebug") // Ensure JAR is built before copying

        it.doFirst {
            if (extension.pluginClassName == null) {
                if (pluginClassFile.get().asFile.exists()) {
                    extension.pluginClassName = pluginClassFile.get().asFile.readText()
                }
            }
        }

        it.doLast {
            if (!extension.isCrossPlatform) {
                return@doLast
            }

            val jarTask = project.tasks.findByName("createFullJarDebug") ?: return@doLast
            val jarFile =
                jarTask.outputs.files.singleFile // Output directory of createFullJarDebug
            if (jarFile != null) {
                val targetFile = project.layout.buildDirectory.file("${project.name}.jar").get().asFile
                jarFile.copyTo(targetFile, overwrite = true)
                extension.jarFileSize = jarFile.length()
                it.logger.lifecycle("Made Cloudstream cross-platform package at ${targetFile.absolutePath}")
            } else {
                it.logger.warn("Could not find JAR file!")
            }
        }
    }

    project.tasks.register("ensureJarCompatibility", EnsureJarCompatibilityTask::class.java) { task ->
        task.dependsOn("compilePluginJar")
        task.hasCrossPlatformSupport.set(extension.isCrossPlatform)
        if (extension.isCrossPlatform) {
            task.jarFile.set(project.layout.buildDirectory.file("${project.name}.jar"))
            task.doLast {
                task.checkOutput()
            }
        }
    }

    project.afterEvaluate {
        val make = project.tasks.register("make", Zip::class.java) {
            val compileDexTask = compileDex.get()
            it.dependsOn(compileDexTask)
            if (extension.isCrossPlatform) {
                it.dependsOn(compilePluginJar)
            }

            val manifestFile = intermediatesDir.map { it.file("manifest.json") }.get()
            it.from(manifestFile)
            it.doFirst {
                if (extension.pluginClassName == null) {
                    if (pluginClassFile.get().asFile.exists()) {
                        extension.pluginClassName = pluginClassFile.get().asFile.readText()
                    }
                }

                manifestFile.asFile.writeText(
                    JsonBuilder(
                        project.makeManifest(),
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
            zip.destinationDirectory.set(project.layout.buildDirectory)

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
