package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import java.io.File

@CacheableTask
abstract class CompileResourcesTask : Exec() {

    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val input: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aaptExecutable: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val androidJar: RegularFileProperty

    override fun exec() {
        val tmpRes = File.createTempFile("res", ".zip")
        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.asFile.get().path
            args("compile")
            args("--dir", input.asFile.get().path)
            args("-v")
            args("-o", tmpRes.path)
            execute()
        }

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.asFile.get().path
            args("link")
            args( "-I", androidJar.asFile.get().path)
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
