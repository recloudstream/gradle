package com.lagradost.gradle

import com.aliucord.gradle.configuration.registerConfigurations
import com.aliucord.gradle.task.registerTasks
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class CloudstreamPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("cloudstream", CloudstreamExtension::class.java, project)
    }
}