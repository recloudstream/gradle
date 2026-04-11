package com.lagradost.cloudstream3.gradle.tasks

import com.android.build.gradle.tasks.ProcessLibraryManifest
import com.lagradost.cloudstream3.gradle.LibraryExtensionCompat
import com.lagradost.cloudstream3.gradle.findCloudstream
import com.lagradost.cloudstream3.gradle.getCloudstream
import com.lagradost.cloudstream3.gradle.makePluginEntry
import com.lagradost.cloudstream3.gradle.sha256
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

const val TASK_GROUP = "cloudstream"

fun registerTasks(project: Project) {
    val extension = project.extensions.getCloudstream()
    val intermediatesDir = project.layout.buildDirectory.dir("intermediates")

    if (project.rootProject.tasks.findByName("makePluginsJson") == null) {
        project.rootProject.tasks.register("makePluginsJson", MakePluginsJsonTask::class.java) { task ->
            task.group = TASK_GROUP
            task.outputs.upToDateWhen { false }
            task.outputFile.set(task.project.layout.buildDirectory.file("plugins.json"))
            task.pluginEntriesJson.set(
                task.project.provider {
                    val lst = task.project.allprojects.mapNotNull { sub ->
                        sub.extensions.findCloudstream()?.let { sub.makePluginEntry() }
                    }
                    JsonBuilder(lst, JsonGenerator.Options().excludeNulls().build()).toPrettyString()
                }
            )
        }
    }

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        it.group = TASK_GROUP
    }

    val pluginClassFile = intermediatesDir.map { it.file("pluginClass") }

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) { task ->
        task.group = TASK_GROUP

        task.pluginClassFile.set(pluginClassFile)
        task.outputFile.set(intermediatesDir.map { dir -> dir.file("classes.dex") })

        val android = LibraryExtensionCompat(project)
        task.minSdk.set(android.minSdk)
        task.bootClasspath.from(android.bootClasspath)

        val extension = project.extensions.getCloudstream()
        task.pluginClassName.set(extension.pluginClassName)

        val kotlinTask = project.tasks.findByName("compileDebugKotlin") as KotlinCompile?
        if (kotlinTask != null) {
            task.dependsOn(kotlinTask)
            task.input.from(kotlinTask.destinationDirectory)
        }

        task.doLast {
            extension.pluginClassName = task.pluginClassName.orNull
        }
    }

    // resApkFile resolved as a provider at configuration time so it can be
    // referenced in the make task without capturing project at execution time.
    val resApkFile = intermediatesDir.map { it.file("res.apk") }

    val compileResources =
        project.tasks.register("compileResources", CompileResourcesTask::class.java) { task ->
            task.group = TASK_GROUP

            val processManifestTask =
                project.tasks.named("processDebugManifest", ProcessLibraryManifest::class.java)
            task.dependsOn(processManifestTask)

            val android = LibraryExtensionCompat(project)
            task.input.set(android.mainResSrcDir)

            task.manifestFile.set(processManifestTask.flatMap { it.manifestOutputFile })
            task.outputFile.set(resApkFile)

            task.aaptExecutable.set(project.layout.file(project.provider {
                android.sdkDirectory
                    .resolve("build-tools")
                    .resolve(android.buildToolsVersion)
                    .resolve(if (OperatingSystem.current().isWindows) "aapt2.exe" else "aapt2")
            }))

            task.androidJar.set(project.layout.file(project.provider {
                android.sdkDirectory
                    .resolve("platforms")
                    .resolve(android.compileSdk)
                    .resolve("android.jar")
            }))
        }

    val compilePluginJar = project.tasks.register("compilePluginJar", CompilePluginJarTask::class.java) { task ->
        task.group = TASK_GROUP
        task.dependsOn("createFullJarDebug") // Ensure JAR is built before copying
        task.dependsOn("compileDex") // compileDex creates pluginClass
        val jarTask = project.tasks.named("createFullJarDebug")

        task.hasCrossPlatformSupport.set(extension.isCrossPlatform)
        task.pluginClassFile.set(pluginClassFile)
        task.pluginClassName.set(extension.pluginClassName)
        task.jarInputFile.fileProvider(jarTask.map { it.outputs.files.singleFile })
        task.targetJarFile.set(project.layout.buildDirectory.file("${project.name}.jar"))
        task.jarFileSize.set(extension.jarFileSize)
        task.jarHash.set(extension.jarHash)

        task.doLast {
            extension.pluginClassName = task.pluginClassName.orNull
            extension.jarFileSize = task.jarFileSize.orNull
            extension.jarHash = task.jarHash.orNull
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

    val manifestFile = intermediatesDir.map { it.file("manifest.json") }

    val generateManifest = project.tasks.register("generateManifest", GenerateManifestTask::class.java) { task ->
        task.group = TASK_GROUP
        task.dependsOn(compileDex)

        task.pluginClassFile.set(pluginClassFile)
        task.outputFile.set(manifestFile)

        task.pluginName.set(project.name)
        task.pluginVersion.set(
            project.provider {
                project.version.toString().toIntOrNull(10).also { v ->
                    if (v == null) project.logger.warn(
                        "'${project.version}' is not a valid version. Use an integer."
                    )
                } ?: -1
            }
        )
        task.requiresResources.set(extension.requiresResources)
    }

    project.afterEvaluate {
        val make = project.tasks.register("make", Zip::class.java) { task ->
            task.group = TASK_GROUP
            task.dependsOn(compileDex)
            if (extension.isCrossPlatform) {
                task.dependsOn(compilePluginJar)
            }

            task.dependsOn(generateManifest)

            task.from(manifestFile)
            task.from(compileDex.flatMap { it.outputFile })

            if (extension.requiresResources) {
                task.dependsOn(compileResources)
                task.from(project.zipTree(resApkFile)) { copySpec ->
                    copySpec.exclude("AndroidManifest.xml")
                }
            }

            task.isPreserveFileTimestamps = false
            task.archiveBaseName.set(project.name)
            task.archiveExtension.set("cs3")
            task.archiveVersion.set("")
            task.destinationDirectory.set(project.layout.buildDirectory)

            task.doLast {
                extension.fileSize = task.outputs.files.singleFile.length()
                extension.fileHash = sha256(task.outputs.files.singleFile)
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
