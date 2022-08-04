package com.lagradost.cloudstream3.gradle.configuration

import org.gradle.api.Project

fun registerConfigurations(project: Project) {
    val providers = arrayOf(ApkConfigurationProvider())

    for (provider in providers) {
        project.configurations.register(provider.name) {
            it.isTransitive = false
        }
    }

    project.afterEvaluate {
        for (provider in providers) {
            val configuration = project.configurations.getByName(provider.name)
            val dependencies = configuration.dependencies

            require(dependencies.size <= 1) {
                "Only one '${provider.name}' dependency should be specified, but ${dependencies.size} were!"
            }

            for (dependency in dependencies) {
                provider.provide(project, dependency)
            }
        }
    }
}