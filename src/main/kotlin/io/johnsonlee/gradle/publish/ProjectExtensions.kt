package io.johnsonlee.gradle.publish

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
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

fun Project.publishing(
        config: PublishingExtension.() -> Unit
) = extensions.configure(PublishingExtension::class.java, config)

fun Project.signing(
        config: SigningExtension.() -> Unit
) = extensions.configure(SigningExtension::class.java, config)

val Project.git: Repository
    get() = FileRepositoryBuilder().setGitDir(File(rootDir, ".git")).findGitDir().build()

val Project.license: License?
    get() = rootDir.listFiles()?.asSequence()?.mapNotNull(License.Companion::of)?.firstOrNull()
