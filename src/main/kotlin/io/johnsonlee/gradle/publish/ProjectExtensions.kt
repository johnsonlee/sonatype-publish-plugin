package io.johnsonlee.gradle.publish

import de.marcphilipp.gradle.nexus.NexusPublishExtension
import io.codearte.gradle.nexus.NexusStagingExtension
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Project.DEFAULT_VERSION
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.repositories
import org.gradle.plugins.signing.SigningExtension
import java.io.File

val Project.hasKotlinPlugin
    get() = plugins.hasPlugin("org.jetbrains.kotlin.jvm")

val Project.hasJavaLibraryPlugin
    get() = plugins.hasPlugin("java-library")

val Project.hasJavaGradlePlugin
    get() = plugins.hasPlugin("java-gradle-plugin")

val Project.hasAndroidLibraryPlugin
    get() = plugins.hasPlugin("com.android.library")

val Project.hasSigningProperties
    get() = arrayOf("signing.keyId", "signing.password", "signing.secretKeyRingFile").none {
        findProperty(it) != null
    }

val Project.useSonatype: Boolean
    get() {
        val OSSRH_USERNAME: String? by vars
        val OSSRH_PASSWORD: String? by vars
        val OSSRH_PACKAGE_GROUP: String? by vars
        return OSSRH_USERNAME != null
                && OSSRH_PASSWORD != null
                && OSSRH_PACKAGE_GROUP != null
                && hasSigningProperties
    }

val Project.git: Repository
    get() = FileRepositoryBuilder().setGitDir(File(rootDir, ".git")).findGitDir().build()

val Project.license: License?
    get() = rootDir.listFiles()?.asSequence()?.mapNotNull(License.Companion::of)?.firstOrNull()

val Project.versionString: String
    get() = version.toString().takeIf { it != DEFAULT_VERSION && it.isNotBlank() }
            ?: parent?.versionString
            ?: throw GradleException("unspecified project version")

val Project.groupString: String
    get() = group.toString().takeIf(String::isNotBlank)?.takeUnless {
        it == rootProject.name || it.startsWith("${rootProject.name}.")
    } ?: parent?.groupString ?: throw GradleException("unspecified project group")

fun Project.publishing(
        config: PublishingExtension.() -> Unit
) = extensions.configure(PublishingExtension::class.java, config)

fun Project.signing(
        config: SigningExtension.() -> Unit
) = extensions.configure(SigningExtension::class.java, config)

fun Project.configureDokka() {
    extensions.findByName("kotlin")?.let {
        plugins.run {
            apply("org.jetbrains.dokka")
        }
    }
}

fun Project.configureSigning() {
    if (!useSonatype) return

    if (!hasSigningProperties) {
        throw GradleException("signing properties are required")
    }

    plugins.apply("signing")

    afterEvaluate {
        publishing {
            signing {
                sign(publications)
            }
        }
    }
}

fun Project.configureNexusStaging() {
    val OSSRH_USERNAME: String? by vars
    val OSSRH_PASSWORD: String? by vars
    val OSSRH_PACKAGE_GROUP: String? by vars
    val OSSRH_SERVER_URL: String? by vars

    if (OSSRH_USERNAME == null || OSSRH_PASSWORD == null || OSSRH_PACKAGE_GROUP == null) {
        return
    }

    plugins.apply("io.codearte.nexus-staging")

    extensions.configure<NexusStagingExtension>("nexusStaging") {
        serverUrl = OSSRH_SERVER_URL?.takeIf(String::isNotBlank) ?: "https://s01.oss.sonatype.org/service/local/"
        packageGroup = OSSRH_PACKAGE_GROUP
        username = OSSRH_USERNAME
        password = OSSRH_PASSWORD
        numberOfRetries = 50
        delayBetweenRetriesInMillis = 3000
    }
}

fun Project.configureNexusPublish() {
    val OSSRH_USERNAME: String? by vars
    val OSSRH_PASSWORD: String? by vars

    if (OSSRH_USERNAME == null || OSSRH_PASSWORD == null) {
        return
    }

    plugins.apply("de.marcphilipp.nexus-publish")

    extensions.configure<NexusPublishExtension>("nexusPublishing") {
        repositories {
            sonatype {
                username.set(OSSRH_USERNAME)
                password.set(OSSRH_PASSWORD)
            }
        }
    }
}

fun Project.configureMavenRepository() {
    repositories {
        configureNexus(project)
    }
}

fun Project.configurePublishRepository() {
    afterEvaluate {
        publishing {
            repositories {
                if (useSonatype) {
                    configureSonatype(project)
                } else {
                    configureNexus(project, if (versionString.endsWith("SNAPSHOT")) {
                        "/content/repositories/snapshot"
                    } else {
                        "/content/repositories/releases"
                    })
                }
            }
        }
    }
}

inline val Project.vars
    get() = ProjectVariableDelegate.of(this)
