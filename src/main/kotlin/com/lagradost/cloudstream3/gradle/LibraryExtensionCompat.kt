package com.lagradost.cloudstream3.gradle

import com.android.build.gradle.BaseExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import java.io.File

/**
 * Compatibility layer for AGP 9, maintaining backward compatibility with AGP 8
 * for Android library modules. Provides access to necessary properties,
 * in a way that works across both versions.
 *
 * Support for BaseExtension can be removed once support for AGP 8 is no longer required.
 */
internal class LibraryExtensionCompat(private val project: Project) {

    private val android = project.extensions.findByName("android")
        ?: error("Android plugin not found")

    val compileSdk: String
        get() = when (android) {
            is BaseExtension -> android.compileSdkVersion ?: error("compileSdkVersion not found")
            is LibraryExtension -> "android-${android.compileSdk}"
            else -> error("Android plugin found, but it's not a library module")
        }

    val minSdk: Int
        get() = when (android) {
            is BaseExtension -> android.defaultConfig.minSdk ?: 21
            is LibraryExtension -> android.defaultConfig.minSdk ?: 21
            else -> error("Android plugin found, but it's not a library module")
        }

    val buildToolsVersion: String
        get() = when (android) {
            is BaseExtension -> android.buildToolsVersion
            is LibraryExtension -> android.buildToolsVersion
            else -> error("Android plugin found, but it's not a library module")
        }

    val adb: File
        get() = when (android) {
            is BaseExtension -> android.adbExecutable
            is LibraryExtension -> project.extensions
                .findByType(LibraryAndroidComponentsExtension::class.java)
                ?.sdkComponents
                ?.adb?.get()?.asFile ?: error("LibraryAndroidComponentsExtension not found")
            else -> error("Unknown Android extension type")
        }

    val bootClasspath: Any
        get() = when (android) {
            is BaseExtension -> android.bootClasspath
            is LibraryExtension -> project.extensions
                .findByType(LibraryAndroidComponentsExtension::class.java)
                ?.sdkComponents
                ?.bootClasspath ?: error("LibraryAndroidComponentsExtension not found")
            else -> error("Unknown Android extension type")
        }

    val sdkDirectory: File
        get() = when (android) {
            is BaseExtension -> android.sdkDirectory
            is LibraryExtension -> project.extensions
                .findByType(LibraryAndroidComponentsExtension::class.java)
                ?.sdkComponents
                ?.sdkDirectory
                ?.get()?.asFile ?: error("LibraryAndroidComponentsExtension not found")
            else -> error("Unknown Android extension type")
        }

    val mainResSrcDir: File
        get() = when (android) {
            is BaseExtension -> android.sourceSets.getByName("main").res.srcDirs.single()
            is LibraryExtension -> {
                val dir = project.layout.projectDirectory.dir("src/main/res").asFile
                if (!dir.exists()) {
                    error(
                        "Resource directory not found at ${dir.path}. " +
                        "Resources are only supported in src/main/res. " +
                        "If this extension has no resources, remove requiresResources = true."
                    )
                }
                dir
            }
            else -> error("Unknown Android extension type")
        }
}
