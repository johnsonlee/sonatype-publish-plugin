package io.johnsonlee.gradle.publish

import com.didiglobal.booster.kotlinx.file
import com.didiglobal.booster.kotlinx.touch
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class KotlinLibraryFunctionalTest {

    private val tmp = File("build", "tmp").also(File::mkdirs)

    @Test
    fun `publish kotlin library to maven local with gradle 7_4`() {
        val result = publishToMavenLocal("7.4")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 7_4_1`() {
        val result = publishToMavenLocal("7.4.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 7_4_2`() {
        val result = publishToMavenLocal("7.4.2")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 7_5`() {
        val result = publishToMavenLocal("7.5")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 7_5_1`() {
        val result = publishToMavenLocal("7.5.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 7_6`() {
        val result = publishToMavenLocal("7.6")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 7_6_1`() {
        val result = publishToMavenLocal("7.6.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 8_0`() {
        val result = publishToMavenLocal("8.0")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 8_1`() {
        val result = publishToMavenLocal("8.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 8_2`() {
        val result = publishToMavenLocal("8.2")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 8_3`() {
        val result = publishToMavenLocal("8.3")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 8_4`() {
        val result = publishToMavenLocal("8.4")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 8_5`() {
        val result = publishToMavenLocal("8.5")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    private fun publishToMavenLocal(gradleVersion: String): BuildResult {
        val projectDir = createTempDir(directory = tmp).apply {
            file("settings.gradle.kts").writeText("")
            file("build.gradle.kts").writeText("""
                plugins {
                    kotlin("jvm") version embeddedKotlinVersion
                    id("io.johnsonlee.sonatype-publish-plugin")
                }

                repositories {
                    mavenCentral()
                    google()
                }

                group = "io.johnsonlee.functional-test"
                version = "1.0.0"
            """.trimIndent())
            file("src", "main", "kotlin", "Library.kt").touch().writeText("""
                object Library {
                    fun init() {
                    }
                }
            """.trimIndent())
        }

        try {
            return GradleRunner.create().apply {
                forwardOutput()
                withGradleVersion(gradleVersion)
                withPluginClasspath()
                withArguments("publishToMavenLocal", "-S")
                withProjectDir(projectDir)
            }.build()
        } finally {
            projectDir.deleteRecursively()
        }
    }

}
