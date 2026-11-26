package com.mediaswipe.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val MAIN_SOURCES = File("src/main")
private val BUILD_FILES = listOf(File("build.gradle.kts"), File("../gradle/libs.versions.toml"))
private val UPDATE_CHECK_FOLDER = File(MAIN_SOURCES, "java/com/mediaswipe/app/update")
private val SCANNED_EXTENSIONS = setOf("kt", "java", "xml")
private val ANDROID_XML_NAMESPACE = Regex("""http://schemas\.android\.com/\S*""")
private val NETWORK_USE = listOf(
    Regex("""\bjava\.net\b"""),
    Regex("""\bjavax\.net\b"""),
    Regex("""\bandroid\.net\.(http|wifi|ConnectivityManager|Network\b)"""),
    Regex("""\b(HttpURLConnection|HttpsURLConnection|URLConnection|openConnection|openStream)\b"""),
    Regex("""\b(Socket|DatagramSocket|ServerSocket|WebView|DownloadManager|HttpDataSource)\b"""),
    Regex("""(?i)\b(okhttp|retrofit|ktor|volley|cronet|grpc)\b"""),
    Regex("""(?i)\b(https?|wss?|ftp)://"""),
)
private val NETWORK_LIBRARIES = Regex("""(?i)(okhttp|retrofit|ktor|volley|cronet|grpc|coil-network|media3-datasource-(okhttp|cronet))""")

class NetworkUseTest {

    private fun networkLines(file: File): List<String> =
        file.readLines().mapIndexedNotNull { index, line ->
            val withoutNamespaces = line.replace(ANDROID_XML_NAMESPACE, "")
            if (NETWORK_USE.any { it.containsMatchIn(withoutNamespaces) }) "${file.path}:${index + 1}: ${line.trim()}" else null
        }

    @Test
    fun onlyTheUpdateCheckUsesTheNetwork() {
        val scanned = MAIN_SOURCES.walkTopDown().filter { it.isFile && it.extension in SCANNED_EXTENSIONS }.toList()
        assertTrue("no sources found under ${MAIN_SOURCES.absolutePath}", scanned.size > 10)
        val outsideTheUpdateCheck = scanned.filterNot { it.startsWith(UPDATE_CHECK_FOLDER) }.flatMap(::networkLines)
        assertEquals(emptyList<String>(), outsideTheUpdateCheck)
    }

    @Test
    fun theUpdateCheckOnlyTalksToGitHubsReleaseApi() {
        val addresses = UPDATE_CHECK_FOLDER.walkTopDown().filter { it.isFile }
            .flatMap { file -> Regex("""https?://[^"\s]+""").findAll(file.readText()).map { it.value } }
            .toSet()
        assertEquals(
            setOf(
                "https://api.github.com/repos/drew-codes-things/MediaSwipe/releases/latest",
                "https://github.com/drew-codes-things/MediaSwipe/releases/latest",
            ),
            addresses,
        )
    }

    @Test
    fun noNetworkLibraryIsADependency() {
        val found = BUILD_FILES.flatMap { file -> file.readLines().filter { NETWORK_LIBRARIES.containsMatchIn(it) } }
        assertEquals(emptyList<String>(), found)
    }
}
