package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.entities.PluginEntry
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

@CacheableTask
abstract class WriteCacheEntryTask : DefaultTask() {

    @get:Input abstract val pluginName: Property<String>
    @get:Input abstract val pluginVersion: Property<Int>
    @get:Input @get:Optional abstract val repoUrl: Property<String>
    @get:Input @get:Optional abstract val repoRawLink: Property<String> // template: "{file}"
    @get:Input abstract val buildBranch: Property<String>
    @get:Input abstract val status: Property<Int>
    @get:Input abstract val authors: ListProperty<String>
    @get:Input @get:Optional abstract val pluginDescription: Property<String>
    @get:Input @get:Optional abstract val language: Property<String>
    @get:Input @get:Optional abstract val iconUrl: Property<String>
    @get:Input abstract val apiVersion: Property<Int>
    @get:Input @get:Optional abstract val tvTypes: ListProperty<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val cs3File: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val jarFile: RegularFileProperty

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val cs3 = cs3File.asFile.get()
        val jar = jarFile.asFile.orNull?.takeIf { it.exists() }

        val name = pluginName.get()
        val rawTemplate = repoRawLink.orNull
        fun rawLink(file: String): String? = rawTemplate?.replace("{file}", file)

        val entry = PluginEntry(
            url = rawLink("${name}.cs3") ?: "",
            status = status.get(),
            version = pluginVersion.get(),
            name = name,
            internalName = name,
            authors = authors.get(),
            description = pluginDescription.orNull,
            repositoryUrl = repoUrl.orNull,
            language = language.orNull,
            iconUrl = iconUrl.orNull,
            apiVersion = apiVersion.get(),
            tvTypes = tvTypes.orNull,
            fileSize = cs3.length(),
            fileHash = sha256(cs3),
            jarFileSize = jar?.length(),
            jarUrl = jar?.let { rawLink("${name}.jar") },
            jarHash = jar?.let { sha256(it) },
        )

        outputFile.asFile.get().writeText(
            JsonBuilder(
                entry,
                JsonGenerator.Options().excludeNulls().build()
            ).toPrettyString()
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read = fis.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = fis.read(buffer)
            }
        }

        return "sha256-" + digest.digest().joinToString("") { "%02x".format(it) }
    }
}
