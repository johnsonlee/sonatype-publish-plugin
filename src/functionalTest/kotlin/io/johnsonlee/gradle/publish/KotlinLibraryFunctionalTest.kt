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
    fun `publish kotlin library to maven local with gradle 6_2`() {
        val result = publishToMavenLocal("6.2")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_2_1`() {
        val result = publishToMavenLocal("6.2.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_2_2`() {
        val result = publishToMavenLocal("6.2.2")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_3`() {
        val result = publishToMavenLocal("6.3")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_4`() {
        val result = publishToMavenLocal("6.4")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_4_1`() {
        val result = publishToMavenLocal("6.4.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_5`() {
        val result = publishToMavenLocal("6.5")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_5_1`() {
        val result = publishToMavenLocal("6.5.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_6`() {
        val result = publishToMavenLocal("6.6")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_6_1`() {
        val result = publishToMavenLocal("6.6.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_7`() {
        val result = publishToMavenLocal("6.7")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_7_1`() {
        val result = publishToMavenLocal("6.7.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_8`() {
        val result = publishToMavenLocal("6.8")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_8_1`() {
        val result = publishToMavenLocal("6.8.1")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_8_2`() {
        val result = publishToMavenLocal("6.8.2")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_8_3`() {
        val result = publishToMavenLocal("6.8.3")
        assertTrue(result.tasks.none { it.outcome == TaskOutcome.FAILED })
    }

    @Test
    fun `publish kotlin library to maven local with gradle 6_9`() {
        val result = publishToMavenLocal("6.9")
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
