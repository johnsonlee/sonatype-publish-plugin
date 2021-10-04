package io.johnsonlee.gradle.publish

import de.marcphilipp.gradle.nexus.NexusPublishExtension
import io.codearte.gradle.nexus.NexusStagingExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for publishing artifacts to [Sonatype](https://oss.sonatype.org/)
 *
 * @author johnsonlee
 */
class SonatypePublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.rootProject.run(Project::configureNexusStaging)
        project.run(Project::configureNexusPublish)

        project.run {
            afterEvaluate {
                when {
                    hasAndroidLibraryPlugin -> plugins.apply(AndroidLibraryPublishPlugin::class.java)
                    hasJavaGradlePlugin -> plugins.apply(GradlePluginPublishPlugin::class.java)
                    hasKotlinPlugin -> plugins.apply(KotlinLibraryPublishPlugin::class.java)
                    hasJavaLibraryPlugin -> plugins.apply(JavaLibraryPublishPlugin::class.java)
                }

                publishing {
                    signing {
                        sign(publications)
                    }
                }
            }
        }
    }

}

private fun Project.configureNexusStaging() {
    plugins.run {
        apply("io.codearte.nexus-staging")
    }

    extensions.configure<NexusStagingExtension>("nexusStaging") {
        packageGroup = OSSRH_PACKAGE_GROUP
        username = OSSRH_USERNAME
        password = OSSRH_PASSWORD
        numberOfRetries = 50
        delayBetweenRetriesInMillis = 3000
    }
}

private fun Project.configureNexusPublish() {
    plugins.run {
        apply("maven-publish")
        apply("signing")
        apply("de.marcphilipp.nexus-publish")

    }

    extensions.configure<NexusPublishExtension>("nexusPublishing") {
        repositories {
            sonatype {
                username.set(OSSRH_USERNAME)
                password.set(OSSRH_PASSWORD)
            }
        }
    }
}

private val Project.OSSRH_USERNAME
    get() = "${findProperty("OSSRH_USERNAME") ?: System.getenv("OSSRH_USERNAME")}"

private val Project.OSSRH_PASSWORD
    get() = "${findProperty("OSSRH_PASSWORD") ?: System.getenv("OSSRH_PASSWORD")}"

private val Project.OSSRH_PACKAGE_GROUP
    get() = "${findProperty("OSSRH_PACKAGE_GROUP") ?: System.getenv("OSSRH_PACKAGE_GROUP")}"
