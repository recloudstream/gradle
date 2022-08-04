package com.lagradost.cloudstream3.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.lagradost.cloudstream3.gradle.tasks.registerTasks
import com.lagradost.cloudstream3.gradle.configuration.registerConfigurations

abstract class CloudstreamPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("cloudstream", CloudstreamExtension::class.java, project)

        registerTasks(project)
        registerConfigurations(project)
    }
}