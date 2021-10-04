package io.johnsonlee.gradle.publish

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class NexusPublishPluginFunctionalTest {

    @Test
    fun `can run task`() {
        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('io.johnsonlee.sonatype-publish-plugin')
            }
           
            tasks.register("greeting") {
                println("Hello from plugin 'io.johnsonlee.sonatype-publish-plugin'")
            }
        """)

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("greeting")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertTrue(result.output.contains("Hello from plugin 'io.johnsonlee.sonatype-publish-plugin'"))
    }

}
