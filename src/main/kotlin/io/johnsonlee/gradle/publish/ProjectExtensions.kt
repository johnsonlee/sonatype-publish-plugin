package io.johnsonlee.gradle.publish

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Project.DEFAULT_VERSION
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import java.time.Duration

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
            ?: findProperty("sonatypeUsername")?.toString()?.takeIf(String::isNotBlank)

val Project.OSSRH_PASSWORD: String?
    get() = ProjectVariableDelegate.of(this)["OSSRH_PASSWORD"]
            ?: findProperty("sonatypePassword")?.toString()?.takeIf(String::isNotBlank)

val Project.OSSRH_PACKAGE_GROUP: String?
    get() = ProjectVariableDelegate.of(this)["OSSRH_PACKAGE_GROUP"]
            ?: findProperty("sonatypePackageGroup")?.toString()?.takeIf(String::isNotBlank)

val Project.OSSRH_STAGING_PROFILE_ID: String?
    get() = ProjectVariableDelegate.of(this)["OSSRH_STAGING_PROFILE_ID"]
            ?: findProperty("sonatypeStagingProfileId")?.toString()?.takeIf(String::isNotBlank)

val Project.OSSRH_URL: URI?
    get() = (ProjectVariableDelegate.of(this)["OSSRH_URL"]
            ?: findProperty("sonatypeUrl")?.toString()?.takeIf(String::isNotBlank))?.let(::URI)

val Project.OSSRH_SNAPSHOT_REPOSITORY_URL: URI?
    get() = (ProjectVariableDelegate.of(this)["OSSRH_SNAPSHOT_REPOSITORY_URL"]
            ?: findProperty("sonatypeSnapshotRepositoryUrl")?.toString()?.takeIf(String::isNotBlank))?.let(::URI)

val Project.NEXUS_URL: URI?
    get() = (ProjectVariableDelegate.of(this)["NEXUS_URL"]
            ?: findProperty("nexusUrl")?.toString()?.takeIf(String::isNotBlank))?.let(::URI)

val Project.NEXUS_SNAPSHOT_REPOSITORY_URL: URI?
    get() = (ProjectVariableDelegate.of(this)["NEXUS_SNAPSHOT_REPOSITORY_URL"]
            ?: findProperty("nexusSnapshotRepositoryUrl")?.toString()?.takeIf(String::isNotBlank))?.let(::URI)

val Project.NEXUS_USERNAME: String?
    get() = ProjectVariableDelegate.of(this)["NEXUS_USERNAME"]
            ?: findProperty("nexusUsername")?.toString()?.takeIf(String::isNotBlank)

val Project.NEXUS_PASSWORD: String?
    get() = ProjectVariableDelegate.of(this)["NEXUS_PASSWORD"]
            ?: findProperty("nexusPassword")?.toString()?.takeIf(String::isNotBlank)

val Project.useSonatype: Boolean
    get() = OSSRH_USERNAME != null
            && OSSRH_PASSWORD != null
            && OSSRH_PACKAGE_GROUP != null
            && OSSRH_STAGING_PROFILE_ID != null
            && hasSigningProperties

val Project.useNexus: Boolean
    get() = NEXUS_URL != null
            && NEXUS_USERNAME != null
            && NEXUS_PASSWORD != null

val Project.git: Repository
    get() = FileRepositoryBuilder().findGitDir(rootDir).setMustExist(useSonatype).build()

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
    logger.info("[${this}] Configuring `dokka` completed")
}

fun Project.configureSigning() {
    if (!hasSigningProperties) {
        logger.warn("[${this}] Configuring `signing` skipped: ${SIGNING_PROPERTIES.joinToString(" or ") { "`${it}`" }} not found")
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

    logger.info("[${this}] Configuring `signing` completed")
}

fun Project.configureNexusPublishing() {
    if (!plugins.hasPlugin("io.github.gradle-nexus.publish-plugin")) {
        plugins.apply("io.github.gradle-nexus.publish-plugin")
    }

    extensions.configure<NexusPublishExtension> {
        clientTimeout.set(Duration.ofSeconds(300))
        connectTimeout.set(Duration.ofSeconds(60))

        transitionCheckOptions {
            maxRetries.set(3000)
            delayBetween.set(Duration.ofMillis(3000))
        }

        repositories {
            if (useSonatype) {
                packageGroup.set(OSSRH_PACKAGE_GROUP)
                try {
                    named("sonatype")
                } catch (e: UnknownDomainObjectException) {
                    sonatype {
                        username.set(OSSRH_USERNAME)
                        password.set(OSSRH_PASSWORD)
                        stagingProfileId.set(OSSRH_STAGING_PROFILE_ID)
                        OSSRH_URL?.let(nexusUrl::set)
                        OSSRH_SNAPSHOT_REPOSITORY_URL?.let(snapshotRepositoryUrl::set)
                    }
                    logger.info("[${this}] Configuring `nexusPublishing` with Sonatype complete")
                }
            } else if (useNexus) {
                try {
                    named("nexus")
                } catch (e: UnknownDomainObjectException) {
                    create("nexus") {
                        username.set(NEXUS_USERNAME)
                        password.set(NEXUS_PASSWORD)
                        NEXUS_URL?.let(nexusUrl::set)
                        NEXUS_SNAPSHOT_REPOSITORY_URL?.let(snapshotRepositoryUrl::set) ?: NEXUS_URL?.let { baseUrl ->
                            snapshotRepositoryUrl.set(baseUrl.resolve("/content/repositories/snapshots/"))
                        }
                        logger.info("[${this}] Configuring `nexusPublishing` with Nexus {nexusUrl=${nexusUrl}, snapshotRepositoryUrl=${snapshotRepositoryUrl.get()}} complete")
                    }
                }
            } else {
                logger.warn("[${this}] Configuring `nexusPublishing` skipped, neither Sonatype nor Nexus environment variables are configured")
            }
        }
    }
}
