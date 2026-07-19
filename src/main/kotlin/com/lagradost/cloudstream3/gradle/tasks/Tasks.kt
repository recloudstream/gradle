package com.lagradost.cloudstream3.gradle.tasks

import com.android.build.gradle.tasks.ProcessLibraryManifest
import com.lagradost.cloudstream3.gradle.LibraryExtensionCompat
import com.lagradost.cloudstream3.gradle.getCloudstream
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
            task.nuvioOutputFile.set(task.project.layout.buildDirectory.file("nuvio.json"))
            task.repositoryName.set(task.project.name)
        }
    }

    project.tasks.register("generateSources", GenerateSourcesTask::class.java) { task ->
        task.group = TASK_GROUP
        val apkinfoProvider = project.provider {
            extension.apkinfo ?: error(
                "Task 'generateSources' requires APK info to be configured, " +
                "but none was found. If this project does not use cloudstream.jar, " +
                "this task does not apply."
            )
        }

        task.urlPrefix.set(apkinfoProvider.map { it.urlPrefix })
        task.sourcesJarFile.set(project.layout.file(
            project.provider {
                apkinfoProvider.get().cache.resolve("cloudstream-sources.jar")
            })
        )
    }

    val pluginClassFile = intermediatesDir.map { it.file("pluginClass") }

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) { task ->
        task.group = TASK_GROUP

        task.pluginClassFile.set(pluginClassFile)
        task.outputFile.set(intermediatesDir.map { dir -> dir.file("classes.dex") })

        val android = LibraryExtensionCompat(project)
        task.minSdk.set(android.minSdk)
        task.bootClasspath.from(android.bootClasspath)

        val kotlinTask = (project.tasks.findByName("compileDebugKotlin")
            ?: project.tasks.findByName("compileAndroidMain")) as? KotlinCompile
        if (kotlinTask != null) {
            task.dependsOn(kotlinTask)
            task.input.from(kotlinTask.destinationDirectory)
        }
    }

    // resApkFile resolved as a provider at configuration time so it can be
    // referenced in the make task without capturing project at execution time.
    val resApkFile = intermediatesDir.map { it.file("res.apk") }

    val compileResources =
        project.tasks.register("compileResources", CompileResourcesTask::class.java) { task ->
            task.group = TASK_GROUP

            val processManifestTask =
                (project.tasks.findByName("processDebugManifest")
                    ?: project.tasks.findByName("processAndroidMainManifest")) as? ProcessLibraryManifest
                    ?: error("No manifest process task.")

            task.dependsOn(processManifestTask)
            task.manifestFile.set(processManifestTask.manifestOutputFile)
            val android = LibraryExtensionCompat(project)

            task.outputFile.set(resApkFile)
            if (extension.requiresResources) {
                task.input.set(android.mainResSrcDir)
            }

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

    val compilePluginJs =
        project.tasks.register("compilePluginJs", CompilePluginJsTask::class.java) { task ->
            task.group = TASK_GROUP
            task.onlyIf("isCrossPlatform must be turned on to compile to JS.") {
                extension.isCrossPlatform
            }
            // Prevent dependencies from being executed
            if (!extension.isCrossPlatform) {
                return@register
            }

            task.dependsOn(compileDex)
            task.pluginClassFile.set(pluginClassFile)
            task.nuvioEnabled.set(extension.isNuvioEnabled && extension.isCrossPlatform)
            task.targetJsFile.set(project.layout.buildDirectory.file("${project.name}.js"))

            val compileJsTaskName = "jsProductionExecutableCompileSync"
            // No source if there is no JS compilation task
            val compileJsTask = project.tasks.findByName(compileJsTaskName)

            val source = compileJsTask?.let { compileTask ->
                task.dependsOn(compileTask) // Ensure JS is compiled before copy
                compileTask.outputs.files.singleFile
            }

            task.onlyIf("There must be a JS output from $compileJsTaskName.") {
                source != null
            }

            task.pluginName.set(project.name)
            task.sourceDir.set(source)
        }

    val compilePluginJar =
        project.tasks.register("compilePluginJar", CompilePluginJarTask::class.java) { task ->
            task.group = TASK_GROUP
            task.onlyIf("isCrossPlatform must be turned on to compile to a JAR.") {
                extension.isCrossPlatform
            }
            // Prevent dependencies from being executed
            if (!extension.isCrossPlatform) {
                return@register
            }

            val jarTask = project.tasks.findByName("createFullJarDebug")
                ?: project.tasks.findByName("jvmJar") ?: error("No JAR task available.")

            if (task.enabled) {
                task.dependsOn(jarTask) // Ensure JAR is built before copying
                task.finalizedBy("ensureJarCompatibility") // Ensure compiled JAR is valid
            }

            task.jarInputFile.fileValue(jarTask.outputs.files.singleFile)
            task.targetJarFile.set(project.layout.buildDirectory.file("${project.name}.jar"))
        }

    val ensureJarCompatibility =
        project.tasks.register(
            "ensureJarCompatibility",
            EnsureJarCompatibilityTask::class.java
        ) { task ->
            task.onlyIf("isCrossPlatform must be turned on to check the JAR file.") {
                extension.isCrossPlatform
            }
            // Prevent dependencies from being executed
            if (!extension.isCrossPlatform) {
                return@register
            }

            task.dependsOn(compilePluginJar)
            task.jarFile.set(project.layout.buildDirectory.file("${project.name}.jar"))
            task.doLast {
                task.checkOutput()
            }
        }

    val manifestFile = intermediatesDir.map { it.file("manifest.json") }

    val generateManifest =
        project.tasks.register("generateManifest", GenerateManifestTask::class.java) { task ->
            task.group = TASK_GROUP
            task.dependsOn(compileDex)

            task.pluginClassFile.set(pluginClassFile)
            task.outputFile.set(manifestFile)

            task.pluginName.set(project.name)
            task.pluginVersion.set(
                project.provider {
                    project.version.toString().toIntOrNull(10).also { v ->
                        if (v == null) project.logger.warn(
                            "'${project.version}' is not a valid version in ${project.name}. Use an integer."
                        )
                    } ?: -1
                }
            )
            task.requiresResources.set(extension.requiresResources)
        }

    val make = project.tasks.register("make", Zip::class.java) { task ->
        task.group = TASK_GROUP
        task.dependsOn(compileDex)

        if (extension.isCrossPlatform) {
            task.dependsOn(compilePluginJar)
            task.dependsOn(compilePluginJs)
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
            task.logger.lifecycle("Made CloudStream package at ${task.outputs.files.singleFile}")
        }
    }

    val pluginEntryFile = project.layout.buildDirectory.file("plugin-entry.json")
    val nuvioPluginEntryFile = project.layout.buildDirectory.file("nuvio-plugin-entry.json")

    val writeCacheEntry =
        project.tasks.register("writeCacheEntry", WriteCacheEntryTask::class.java) { task ->
            task.group = TASK_GROUP
            task.dependsOn(make)
            if (extension.isCrossPlatform) {
                task.dependsOn(compilePluginJar)
                task.dependsOn(compilePluginJs)
            }

            task.nuvioEnabled.set(extension.isNuvioEnabled && extension.isCrossPlatform)
            task.pluginName.set(project.name)
            task.pluginVersion.set(project.provider {
                project.version.toString().toIntOrNull(10) ?: -1
            })
            task.repoUrl.set(project.provider { extension.repository?.url })
            task.repoRawLink.set(project.provider {
                extension.repository?.getRawLink(
                    "{file}",
                    extension.buildBranch
                )
            })
            task.buildBranch.set(project.provider { extension.buildBranch })
            task.status.set(project.provider { extension.status })
            task.authors.set(project.provider { extension.authors })
            task.pluginDescription.set(project.provider { extension.description })
            task.language.set(project.provider { extension.language })
            task.iconUrl.set(project.provider { extension.iconUrl })
            task.apiVersion.set(project.provider { extension.apiVersion })
            task.tvTypes.set(project.provider { extension.tvTypes })

            task.cs3File.set(make.flatMap { zip ->
                zip.outputs.files.let { project.layout.buildDirectory.file("${project.name}.cs3") }
            })
            if (extension.isCrossPlatform) {
                task.jarFile.set(project.layout.buildDirectory.file("${project.name}.jar"))
                val jsFile = project.layout.buildDirectory.file("${project.name}.js").get()
                if (jsFile.asFile.exists()) {
                    task.jsFile.set(jsFile)
                }
            }

            task.outputFile.set(pluginEntryFile)
            task.nuvioOutputFile.set(nuvioPluginEntryFile)
        }

    project.rootProject.tasks.named("makePluginsJson", MakePluginsJsonTask::class.java)
        .configure { task ->
            task.dependsOn(writeCacheEntry)
            task.mustRunAfter(ensureJarCompatibility)
            task.pluginEntryFiles.from(pluginEntryFile)
            task.nuvioEntryFiles.from(nuvioPluginEntryFile)
        }

    project.tasks.register("cleanCache", CleanCacheTask::class.java) { task ->
        task.group = TASK_GROUP
        val apkinfoProvider = project.provider {
            extension.apkinfo ?: error(
                "Cannot clean cache: no cached APK info found. " +
                "This task only applies to projects that depend on cloudstream.jar."
            )
        }

        task.jarFile.set(project.layout.file(
            apkinfoProvider.map { it.jarFile }
        ))
    }

    project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) { task ->
        task.group = TASK_GROUP
        task.dependsOn(make)

        val android = LibraryExtensionCompat(project)
        task.adbPath.set(android.adb.absolutePath)
        task.pluginFile.set(project.layout.file(
            make.map { it.outputs.files.singleFile }
        ))
    }
}
