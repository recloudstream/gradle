package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.getCloudstream
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.ClassFileInputs
import com.android.builder.dexing.DexArchiveBuilder
import com.android.builder.dexing.DexParameters
import com.android.builder.dexing.r8.ClassFileProviderFactory
import com.google.common.io.Closer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

abstract class CompileDexTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    val input: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val pluginClassFile: RegularFileProperty

    @get:Internal
	abstract val pluginClassName: Property<String?>

    @get:Input
	abstract val minSdk: Property<Int>

	@get:InputFiles
	abstract val bootClasspath: ConfigurableFileCollection

    @TaskAction
    fun compileDex() {
        val dexOutputDir = outputFile.get().asFile.parentFile

        Closer.create().use { closer ->
            val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
                DexParameters(
                    minSdkVersion = minSdk.get(),
                    debuggable = true,
                    dexPerClass = false,
                    withDesugaring = true, // Make all plugins work on lower android versions
                    desugarBootclasspath = ClassFileProviderFactory(bootClasspath.files.map(File::toPath))
                        .also { closer.register(it) },
                    desugarClasspath = ClassFileProviderFactory(listOf<Path>()).also {
                        closer.register(
                            it
                        )
                    },
                    coreLibDesugarConfig = null,
                    messageReceiver = MessageReceiverImpl(
                        ErrorFormatMode.HUMAN_READABLE,
                        LoggerFactory.getLogger(CompileDexTask::class.java)
                    ),
                    enableApiModeling = false // Unknown option, setting to false seems to work
                )
            )

            val fileStreams =
                input.map { input -> ClassFileInputs.fromPath(input.toPath()).use { it.entries { _, _ -> true } } }
                    .toTypedArray()

            Arrays.stream(fileStreams).flatMap { it }
                .use { classesInput ->
                    val files = classesInput.collect(Collectors.toList())

                    dexBuilder.convert(
                        files.stream(),
                        dexOutputDir.toPath(),
                        null,
                    )

                    for (file in files) {
                        val reader = ClassReader(file.readAllBytes())

                        val classNode = ClassNode()
                        reader.accept(classNode, 0)

                        for (annotation in classNode.visibleAnnotations.orEmpty() + classNode.invisibleAnnotations.orEmpty()) {
                            if (annotation.desc == "Lcom/lagradost/cloudstream3/plugins/CloudstreamPlugin;") {
                                require(pluginClassName.orNull == null) {
                                    "Only 1 active plugin class per project is supported"
                                }

                                val detectedName = classNode.name.replace('/', '.')
                                pluginClassFile.asFile.orNull?.writeText(detectedName)
                                pluginClassName.set(detectedName)
                            }
                        }
                    }
                }
        }

        logger.lifecycle("Compiled dex to ${outputFile.get()}")
    }
}
