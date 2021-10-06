package io.johnsonlee.gradle.publish

import org.eclipse.jgit.api.Git
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Project.DEFAULT_VERSION
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.maven

abstract class AbstractLibraryPublishPlugin : Plugin<Project> {

    final override fun apply(project: Project) {
        val repo = project.git
        val git = Git(repo)
        val localName: String = System.getProperty("user.name")
        val devName: String? = repo.config.getString("user", null, "name")
        val devEmail: String? = repo.config.getString("user", null, "email")
        val devAccount = devEmail?.let { email ->
            setOf(email to (devName ?: email.substringBefore("@")))
        }
        val url = repo.config.getString("remote", "origin", "url")
        val license = project.license
        val developers = (try {
            git.log().call().map {
                val author = it.authorIdent
                author.emailAddress to author.name
            }.toSet()
        } catch (e: Throwable) {
            emptySet()
        }).takeIf(Collection<Pair<String, String>>::isNotEmpty)
                ?: devAccount
                ?: setOf("${localName}@local" to localName)

        project.run {
            afterEvaluate {
                configureDependencies()

                publishing {
                    repositories {
                        maven("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    }

                    publications {
                        createMavenPublications(this) {
                            pom.withXml {
                                asNode().apply {
                                    appendNode("name", project.name)
                                    appendNode("url", url)
                                    appendNode("description", project.description ?: project.name)
                                    appendNode("scm").apply {
                                        appendNode("connection", "scm:git:${url}")
                                        appendNode("developerConnection", "scm:git:${url}")
                                        appendNode("url", url)
                                    }
                                    appendNode("licenses").apply {
                                        license?.let {
                                            appendNode("license").apply {
                                                appendNode("name", license.name)
                                                appendNode("url", license.url)
                                            }
                                        }
                                    }
                                    appendNode("developers").apply {
                                        developers.forEach { (email, name) ->
                                            appendNode("developer").apply {
                                                appendNode("id", name)
                                                appendNode("name", name)
                                                appendNode("email", email)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    abstract fun Project.createMavenPublications(
            publications: PublicationContainer,
            config: MavenPublication.() -> Unit
    )

    open fun Project.configureDependencies() {
        configureDokka()
    }

    fun MavenPublication.configure(project: Project) {
        artifactId = project.name
        groupId = listOf(project.group, project.rootProject.group)
                .map(Any::toString)
                .filterNot { it == project.rootProject.name || it.startsWith("${project.rootProject.name}.") }
                .firstOrNull(String::isNotBlank)
                ?: throw GradleException("group id of $project has not been configured")
        version = listOf(project.version, project.rootProject.version)
                .map(Any::toString)
                .firstOrNull { it != DEFAULT_VERSION }
                ?: throw GradleException("version of $project has not been configured")
    }

    fun Project.configureDokka() {
        extensions.findByName("kotlin")?.let {
            plugins.run {
                apply("org.jetbrains.dokka")
            }
        }
    }

}