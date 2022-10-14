package io.johnsonlee.gradle.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.repositories

/**
 * Gradle plugin for publishing artifacts to [Sonatype](https://oss.sonatype.org/) or Nexus
 *
 * @author johnsonlee
 */
@Suppress("LocalVariableName")
class SonatypePublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.rootProject.run {
            configureNexusStaging()
        }

        project.run {
            plugins.apply("maven-publish")

            // for sonatype
            configureNexusPublish()

            repositories {
                mavenCentral()
            }

            configurePublishRepository()

            applySubplugin()

            // for nexus
            configureMavenRepository()

            configureSigning()
        }
    }

    private fun Project.applySubplugin() {
        val apply: Project.() -> Unit = {
            when {
                hasAndroidLibraryPlugin -> plugins.apply(AndroidLibraryPublishPlugin::class.java)
                hasJavaGradlePlugin -> plugins.apply(GradlePluginPublishPlugin::class.java)
                hasKotlinPlugin -> plugins.apply(KotlinLibraryPublishPlugin::class.java)
                hasJavaLibraryPlugin -> plugins.apply(JavaLibraryPublishPlugin::class.java)
            }
        }
        if (state.executed) {
            apply()
        } else {
            afterEvaluate {
                apply()
            }
        }
    }

}
