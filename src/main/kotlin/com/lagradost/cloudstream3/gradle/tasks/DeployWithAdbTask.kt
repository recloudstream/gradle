package com.lagradost.cloudstream3.gradle.tasks

import com.lagradost.cloudstream3.gradle.LibraryExtensionCompat
import com.lagradost.cloudstream3.gradle.getCloudstream
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import se.vidstige.jadb.*
import java.nio.charset.StandardCharsets

abstract class DeployWithAdbTask : DefaultTask() {
    @get:Input
    @set:Option(option = "wait-for-debugger", description = "Enables debugging flag when starting the discord activity")
    var waitForDebugger: Boolean = false

    @TaskAction
    fun deployWithAdb() {
        val android = LibraryExtensionCompat(project)

        AdbServerLauncher(Subprocess(), android.adb.absolutePath).launch()
        val jadbConnection = JadbConnection()
        val devices = jadbConnection.devices.filter {
            try {
                it.state == JadbDevice.State.Device
            } catch (e: JadbException) {
                false
            }
        }

        require(devices.size == 1) {
            "Only one ADB device should be connected, but ${devices.size} were!"
        }

        val device = devices[0]

        val make = project.tasks.getByName("make") as AbstractCopyTask

        var file = make.outputs.files.singleFile

        var path = "/storage/emulated/0/Cloudstream3/plugins/"

        device.push(file, RemoteFile(path + file.name))

        // Make the file readonly to work on newer android versions, this does not impact adb push.
        // https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
        device.executeShell("chmod", "-w", path + file.name)

        val args = arrayListOf("start", "-a", "android.intent.action.VIEW", "-d", "cloudstreamapp:")

        if (waitForDebugger) {
            args.add("-D")
        }

        val response = String(
            device.executeShell("am", *args.toTypedArray()).readAllBytes(), StandardCharsets.UTF_8
        )

        if (response.contains("Error")) {
            logger.error(response)
        }

        logger.lifecycle("Deployed $file to ${device.serial}")
    }
}
