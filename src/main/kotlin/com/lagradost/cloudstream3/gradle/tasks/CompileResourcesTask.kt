package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
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

    @get:InputFile
    abstract val aaptExecutable: RegularFileProperty

    @get:InputFile
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
