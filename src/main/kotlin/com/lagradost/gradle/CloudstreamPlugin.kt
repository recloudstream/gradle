package com.lagradost.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class CloudstreamPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("cloudstream", CloudstreamExtension::class.java, project)
    }
}