package com.lagradost.cloudstream3.gradle.tasks

import com.android.build.gradle.BaseExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import java.io.File

abstract class CompileResourcesTask : Exec() {
    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    abstract val input: DirectoryProperty

    @get:InputFile
    abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    override fun exec() {
        val android = project.extensions.getByName("android") as BaseExtension

        val aaptExecutable = android.sdkDirectory.resolve("build-tools")
            .resolve(android.buildToolsVersion)
            .resolve(if (OperatingSystem.current().isWindows) "aapt2.exe" else "aapt2")

        val tmpRes = File.createTempFile("res", ".zip")

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.path
            args("compile")
            args("--dir", input.asFile.get().path)
            args("-v")
            args("-o", tmpRes.path)
            execute()
        }

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.path
            args("link")
            args(
                "-I",
                android.sdkDirectory
                    .resolve("platforms")
                    .resolve(android.compileSdkVersion!!)
                    .resolve("android.jar")
            )
            args("-R", tmpRes.path)
            args("--manifest", manifestFile.asFile.get().path)
            args("--auto-add-overlay")
            args("--warn-manifest-validation")
            args("-v")
            args("-o", outputFile.asFile.get().path)
            execute()
        }

        tmpRes.delete()
    }
}
