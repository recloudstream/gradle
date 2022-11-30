plugins {
    kotlin("jvm") version "1.7.10"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.lagradost.cloudstream3"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", kotlin.coreLibrariesVersion))
    compileOnly(gradleApi())

    compileOnly("com.google.guava:guava:30.1.1-jre")
    compileOnly("com.android.tools:sdk-common:30.0.0")
    compileOnly("com.android.tools.build:gradle:7.2.2")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-tree:9.4")
    implementation("com.github.js6pak:jadb:fix-modified-time-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("com.lagradost.cloudstream3.gradle") {
            id = "com.lagradost.cloudstream3.gradle"
            implementationClass = "com.lagradost.cloudstream3.gradle.CloudstreamPlugin"
        }
    }
}

publishing {
    repositories {
        val token = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")

        if (token != null) {
            maven {
                credentials {
                    username = "recloudstream"
                    password = token
                }
                setUrl("https://maven.pkg.github.com/recloudstream/gradle")
            }
        } else {
            mavenLocal()
        }
    }
}