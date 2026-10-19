package io.johnsonlee.gradle.publish

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Project.DEFAULT_VERSION
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.repositories
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import java.time.Duration

internal val SIGNING_PROPERTIES = arrayOf("signing.keyId", "signing.password", "signing.secretKeyRingFile")

/**
 * The task generating HTML documentation in Dokka Gradle Plugin v2, replacing `dokkaHtml` of v1
 */
internal const val DOKKA_HTML_TASK_NAME = "dokkaGeneratePublicationHtml"

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

val Project.OSSRH_SNAPSHOT_URL: URI
    get() = ProjectVariableDelegate.of(this)["OSSRH_SNAPSHOT_URL"]?.let(::URI) ?: SONATYPE_SNAPSHOT_SERVER

val Project.NEXUS_URL: URI?
    get() = ProjectVariableDelegate.of(this)["NEXUS_URL"]?.let(::URI)

val Project.NEXUS_USERNAME: String?
    get() = ProjectVariableDelegate.of(this)["NEXUS_USERNAME"]

val Project.NEXUS_PASSWORD: String?
    get() = ProjectVariableDelegate.of(this)["NEXUS_PASSWORD"]

val Project.nexusPublishingClientTimeout: Duration
    get() = (try {
        Duration.parse(ProjectVariableDelegate.of(this)["nexusPublishing.clientTimeout"])
    } catch (_: Throwable) {
        null
    }) ?: Duration.ofMinutes(3)

val Project.nexusPublishingConnectTimeout: Duration
    get() = (try {
        Duration.parse(ProjectVariableDelegate.of(this)["nexusPublishing.connectTimeout"])
    } catch (_: Throwable) {
        null
    }) ?: Duration.ofMinutes(3)

val Project.useSonatype: Boolean
    get() = OSSRH_USERNAME != null && OSSRH_PASSWORD != null && OSSRH_PACKAGE_GROUP != null && hasSigningProperties

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

    val configure: Project.() -> Unit = {
        publishing {
            signing {
                sign(publications)
            }
        }
    }

    if (state.executed) {
        configure()
    } else {
        afterEvaluate {
            configure()
        }
    }

    logger.info("[${this}] Configuring `signing` completed")
}

fun Project.configureNexusPublish() {
    if (OSSRH_USERNAME == null || OSSRH_PASSWORD == null) {
        logger.warn("[${this}] Configuring `nexusPublishing` skipped: `OSSRH_USERNAME` or `OSSRH_PASSWORD` not found")
        return
    }

    if (plugins.hasPlugin("io.github.gradle-nexus.publish-plugin")) {
        logger.info("[${this}] Configuring `nexusPublishing` skipped: already configured")
        return
    }

    plugins.apply("io.github.gradle-nexus.publish-plugin")

    val resolvedNexusUrl = OSSRH_SERVER_URL.resolve("/service/local/")
    val resolvedSnapshotUrl = OSSRH_SNAPSHOT_URL

    extensions.configure<NexusPublishExtension>("nexusPublishing") {
        this.repositories {
            sonatype {
                nexusUrl.set(resolvedNexusUrl)
                snapshotRepositoryUrl.set(resolvedSnapshotUrl)
                username.set(OSSRH_USERNAME)
                password.set(OSSRH_PASSWORD)
            }
        }
        OSSRH_PACKAGE_GROUP?.let {
            this.packageGroup.set(it)
        }
        this.transitionCheckOptions {
            maxRetries.set(50)
            delayBetween.set(Duration.ofSeconds(3))
        }
        this.clientTimeout.set(nexusPublishingClientTimeout)
        this.connectTimeout.set(nexusPublishingConnectTimeout)
    }

    logger.info("[${this}] Configuring `nexusPublishing` {nexusUrl=`${resolvedNexusUrl}`, snapshotUrl=`${resolvedSnapshotUrl}`, packageGroup=`${OSSRH_PACKAGE_GROUP}`} completed")
}

fun Project.configureMavenRepository() {
    repositories {
        configureNexus(project)
    }
}

fun Project.configurePublishRepository() {
    val configure: Project.() -> Unit = {
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

    if (state.executed) {
        configure()
    } else {
        afterEvaluate {
            configure()
        }
    }
}
