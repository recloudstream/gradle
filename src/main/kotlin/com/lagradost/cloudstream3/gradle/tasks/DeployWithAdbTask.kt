package com.lagradost.cloudstream3.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import se.vidstige.jadb.AdbServerLauncher
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.JadbException
import se.vidstige.jadb.RemoteFile
import se.vidstige.jadb.Subprocess
import java.io.File
import java.nio.charset.StandardCharsets

@CacheableTask
abstract class DeployWithAdbTask : DefaultTask() {

    @get:Input
    @set:Option(option = "wait-for-debugger", description = "Enables debugging flag when starting the discord activity")
    var waitForDebugger: Boolean = false

    @get:Input abstract val adbPath: Property<String>
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pluginFile: RegularFileProperty

    @TaskAction
    fun deployWithAdb() {
        AdbServerLauncher(Subprocess(), adbPath.get()).launch()
        val jadbConnection = JadbConnection()
        val devices = jadbConnection.devices.filter {
            try {
                it.state == JadbDevice.State.Device
            } catch (_: JadbException) {
                false
            }
        }

        require(devices.size == 1) {
            "Only one ADB device should be connected, but ${devices.size} were!"
        }

        val file: File = pluginFile.get().asFile
        val path = "/storage/emulated/0/Cloudstream3/plugins/"
        val device = devices[0]
        device.push(file, RemoteFile(path + file.name))

        // Make the file readonly to work on newer android versions, this does not impact adb push.
        // https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
        device.executeShell("chmod", "-w", path + file.name)
        val args = arrayListOf("start", "-a", "android.intent.action.VIEW", "-d", "cloudstreamapp:")
        if (waitForDebugger) args.add("-D")
        val response = String(
            device.executeShell("am", *args.toTypedArray()).readAllBytes(), StandardCharsets.UTF_8
        )

        if (response.contains("Error")) logger.error(response)
        logger.lifecycle("Deployed $file to ${device.serial}")
    }
}
