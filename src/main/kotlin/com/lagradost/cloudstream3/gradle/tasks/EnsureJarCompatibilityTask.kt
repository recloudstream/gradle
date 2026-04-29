package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.provider.Property
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.GradleException

@CacheableTask
abstract class EnsureJarCompatibilityTask : Exec() {

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract val hasCrossPlatformSupport: Property<Boolean>

    @get:OutputFile
    val outputFile = project.layout.buildDirectory.file("jdeps-output.txt")

    override fun exec() {
        if (!hasCrossPlatformSupport.get()) return

        val jar = jarFile.get().asFile
        if (!jar.exists()) throw GradleException("JAR file does not exist: ${jar.absolutePath}")

        commandLine("jdeps", "--print-module-deps", jar.absolutePath)
        standardOutput = outputFile.get().asFile.outputStream()
        errorOutput = System.err
        isIgnoreExitValue = true

        super.exec() // actually runs the exec
    }

    fun checkOutput() {
        val output = outputFile.get().asFile.readText().trim()
        when {
            output.isEmpty() -> logger.warn("No output from jdeps! Cannot analyze JAR file for Android imports!")
            "android." in output -> throw GradleException(
                "The cross-platform JAR file contains Android imports! " +
                    "This will cause compatibility issues.\nRemove 'isCrossPlatform = true' or remove the Android imports."
            )
            else -> logger.lifecycle("SUCCESS: The cross-platform JAR file does not contain Android imports.")
        }
    }
}
