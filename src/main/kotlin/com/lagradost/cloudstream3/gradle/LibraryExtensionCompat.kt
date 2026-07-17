// BaseExtension is deprecated and will be removed in AGP 10.0
@file:Suppress("DEPRECATION")

package com.lagradost.cloudstream3.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

/**
 * Compatibility layer for AGP 9, maintaining backward compatibility with AGP 8
 * for Android library modules. Provides access to necessary properties,
 * in a way that works across both versions.
 *
 * Support for BaseExtension can be removed once support for AGP 8 is no longer required.
 */
internal class LibraryExtensionCompat(private val project: Project) {

    private val kmpExtension = project.extensions.findByName("kotlin") as? KotlinMultiplatformExtension?
    private val android = project.extensions.findByName("android") ?: kmpExtension?.targets?.findByName("android") ?: error("Android plugin not found")
    private val androidComponents =
        project.extensions.getByType(
            com.android.build.api.variant.AndroidComponentsExtension::class.java
        )

    val compileSdk: String
        get() = when (android) {
            is BaseExtension -> android.compileSdkVersion ?: error("compileSdkVersion not found")
            is LibraryExtension -> "android-${android.compileSdk}"
            is KotlinMultiplatformAndroidLibraryTarget -> "android-${android.compileSdk}"
            else -> error("Android plugin found, but it's not a library module")
        }

    val minSdk: Int
        get() = when (android) {
            is BaseExtension -> android.defaultConfig.minSdk ?: 21
            is LibraryExtension -> android.defaultConfig.minSdk ?: 21
            is KotlinMultiplatformAndroidLibraryTarget -> android.minSdk ?: 21
            else -> error("Android plugin found, but it's not a library module")
        }

    val buildToolsVersion: String
        get() = when (android) {
            is BaseExtension -> android.buildToolsVersion
            is LibraryExtension -> android.buildToolsVersion
            is KotlinMultiplatformAndroidLibraryTarget -> android.buildToolsVersion
            else -> error("Android plugin found, but it's not a library module")
        }

    val adb: File
        get() = when (android) {
            is BaseExtension -> android.adbExecutable
            is LibraryExtension -> project.extensions
                .findByType(LibraryAndroidComponentsExtension::class.java)
                ?.sdkComponents
                ?.adb?.get()?.asFile ?: error("LibraryAndroidComponentsExtension not found")
            is KotlinMultiplatformAndroidLibraryTarget -> androidComponents.sdkComponents.adb.get().asFile
            else -> error("Unknown Android extension type")
        }

    val bootClasspath: Any
        get() = when (android) {
            is BaseExtension -> android.bootClasspath
            is LibraryExtension -> project.extensions
                .findByType(LibraryAndroidComponentsExtension::class.java)
                ?.sdkComponents
                ?.bootClasspath ?: error("LibraryAndroidComponentsExtension not found")
            is KotlinMultiplatformAndroidLibraryTarget ->  androidComponents.sdkComponents.bootClasspath

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
            is KotlinMultiplatformAndroidLibraryTarget -> androidComponents.sdkComponents.sdkDirectory.get().asFile

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
            is KotlinMultiplatformAndroidLibraryTarget ->  {
                val dir = project.layout.projectDirectory.dir("src/androidMain/resources").asFile
                if (!dir.exists()) {
                    error(
                        "Resource directory not found at ${dir.path}. " +
                        "Resources are only supported in src/androidMain/resources. " +
                        "If this extension has no resources, remove requiresResources = true."
                    )
                }
                dir
            }
            else -> error("Unknown Android extension type")
        }
}
