package com.lagradost.gradle.tasks

import com.cloudstream.gradle.getCloudstream
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import jadx.api.impl.SimpleCodeWriter
import jadx.plugins.input.dex.DexInputPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.function.Function

abstract class GenSourcesTask : DefaultTask() {
    @TaskAction
    fun genSources() {
        val extension = project.extensions.getAliucord()
        val apkinfo = extension.apkinfo!!

        val sourcesJarFile = apkinfo.cache.resolve("cloudstream-${apkinfo.version}-sources.jar")

        val args = JadxArgs()
        args.setInputFile(apkinfo.apkFile)
        args.outDirSrc = sourcesJarFile
        args.isSkipResources = true
        args.isShowInconsistentCode = true
        args.isRespectBytecodeAccModifiers = true
        args.isFsCaseSensitive = true
        args.isGenerateKotlinMetadata = false
        args.isDebugInfo = false
        args.isInlineAnonymousClasses = false
        args.isInlineMethods = false
        args.isReplaceConsts = false

        args.codeCache = NoOpCodeCache()
        args.codeWriterProvider = Function { SimpleCodeWriter(it) }

        JadxDecompiler(args).use { decompiler ->
            decompiler.registerPlugin(DexInputPlugin())
            decompiler.load()
            decompiler.save()
        }
    }
}