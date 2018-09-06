package me.urielsalis.projectpipeline

import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

fun String.runCommand(): String? = try {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    proc.waitFor(60, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText()
} catch (e: IOException) {
    e.printStackTrace()
    null
}

fun createJenkinsJob(name: String, toWrite: String): Int {
    with(URL("$jenkinsHost/createItem?name=$name").openConnection() as HttpURLConnection) {
        requestMethod = "POST"
        doOutput = true
        val auth = Base64.getEncoder().encode("$jenkinsUser:$jenkinsAccessToken".toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)

        addRequestProperty("Content-Type", "application/xml")
        addRequestProperty("Authorization", "Basic $auth")
        val wr = OutputStreamWriter(outputStream)
        wr.write(toWrite)
        wr.flush()

        return responseCode
    }
}