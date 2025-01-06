package com.lagradost.cloudstream3.gradle.configuration

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

// Deprecated, use CloudstreamConfigurationProvider
class ApkConfigurationProvider : IConfigurationProvider {

    override val name: String
        get() = "apk"

    override fun provide(project: Project, dependency: Dependency) {
        CloudstreamConfigurationProvider().provide(project, dependency)
    }
}