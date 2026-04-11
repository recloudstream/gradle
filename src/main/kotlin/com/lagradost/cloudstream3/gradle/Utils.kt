package com.lagradost.cloudstream3.gradle

import org.gradle.api.Project
import com.lagradost.cloudstream3.gradle.entities.*
import java.io.File
import java.security.MessageDigest

fun sha256(file: File): String {
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

fun Project.makePluginEntry(): PluginEntry {
    val extension = this.extensions.getCloudstream()

    val version = this.version.toString().toIntOrNull(10)
    if (version == null) {
        logger.warn("'${project.version}' is not a valid version. Use an integer.")
    }

    val repo = extension.repository

    return PluginEntry(
        url = (if (repo == null) "" else repo.getRawLink("${this.name}.cs3", extension.buildBranch)),
        status = extension.status,
        version = version ?: -1,
        name = this.name,
        internalName = this.name,
        authors = extension.authors,
        description = extension.description,
        repositoryUrl = repo?.url,
        language = extension.language,
        iconUrl = extension.iconUrl,
        apiVersion = extension.apiVersion,
        tvTypes = extension.tvTypes,
        fileSize = extension.fileSize,
        jarFileSize = extension.jarFileSize,
        jarUrl = (
                if (repo == null || extension.jarFileSize == null) null else repo.getRawLink("${this.name}.jar", extension.buildBranch)
        ),
        jarHash = extension.jarHash,
        fileHash = extension.fileHash
    )
}
