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
import java.net.URI

internal val SIGNING_PROPERTIES = arrayOf("signing.keyId", "signing.password", "signing.secretKeyRingFile")

val Project.hasKotlinPlugin
    get() = plugins.hasPlugin("org.jetbrains.kotlin.jvm")

val Project.hasJavaLibraryPlugin
    get() = plugins.hasPlugin("java-library")

val Project.hasJavaGradlePlugin
    get() = plugins.hasPlugin("java-gradle-plugin")

val Project.hasAndroidLibraryPlugin
    get() = plugins.hasPlugin("com.android.library")

val Project.hasSigningProperties
    get() = ProjectVariableDelegate.of(this).let { properties ->
        SIGNING_PROPERTIES.mapNotNull {
            properties[it]
        }.isNotEmpty()
    }

val Project.OSSRH_USERNAME: String?
    get() = ProjectVariableDelegate.of(this)["OSSRH_USERNAME"]

val Project.OSSRH_PASSWORD: String?
    get() = ProjectVariableDelegate.of(this)["OSSRH_PASSWORD"]

val Project.OSSRH_PACKAGE_GROUP: String?
    get() = ProjectVariableDelegate.of(this)["OSSRH_PACKAGE_GROUP"]

val Project.OSSRH_SERVER_URL: URI
    get() = ProjectVariableDelegate.of(this)["OSSRH_SERVER_URL"]?.let(::URI) ?: SONATYPE_SERVER

val Project.NEXUS_URL: URI?
    get() = ProjectVariableDelegate.of(this)["NEXUS_URL"]?.let(::URI)

val Project.NEXUS_USERNAME: String?
    get() = ProjectVariableDelegate.of(this)["NEXUS_USERNAME"]

val Project.NEXUS_PASSWORD: String?
    get() = ProjectVariableDelegate.of(this)["NEXUS_PASSWORD"]

val Project.useSonatype: Boolean
    get() = OSSRH_USERNAME != null && OSSRH_PASSWORD != null && OSSRH_PACKAGE_GROUP != null && hasSigningProperties

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
    logger.info("Configuring `dokka` completed")
}

fun Project.configureSigning() {
    if (!hasSigningProperties) {
        logger.warn("Configuring `signing` skipped: ${SIGNING_PROPERTIES.joinToString(" or ") { "`${it}`" }} not found")
        return
    }

    plugins.apply("signing")

    afterEvaluate {
        publishing {
            signing {
                sign(publications)
            }
        }
    }

    logger.info("Configuring `signing` completed")
}

fun Project.configureNexusStaging() {
    if (OSSRH_USERNAME == null || OSSRH_PASSWORD == null || OSSRH_PACKAGE_GROUP == null) {
        logger.warn("Configuring `nexusStaging` skipped: `OSSRH_USERNAME` or `OSSRH_PASSWORD` or `OSSRH_PACKAGE_GROUP` not found")
        return
    }

    plugins.apply("io.codearte.nexus-staging")

    val stagingUrl = OSSRH_SERVER_URL.resolve("/service/local/").toString()

    extensions.configure<NexusStagingExtension>("nexusStaging") {
        serverUrl = stagingUrl
        packageGroup = OSSRH_PACKAGE_GROUP
        username = OSSRH_USERNAME
        password = OSSRH_PASSWORD
        numberOfRetries = 50
        delayBetweenRetriesInMillis = 3000
    }

    logger.info("Configuring `nexusStaging` {serverUrl=`${stagingUrl}`, packageGroup=`${OSSRH_PACKAGE_GROUP}`} completed")
}

fun Project.configureNexusPublish() {
    if (OSSRH_USERNAME == null || OSSRH_PASSWORD == null) {
        logger.warn("Configuring `nexusPublishing` skipped: `OSSRH_USERNAME` or `OSSRH_PASSWORD` not found")
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

    logger.info("Configuring `nexusPublishing` completed")
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
