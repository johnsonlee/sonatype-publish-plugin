package io.johnsonlee.gradle.publish

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.maven
import java.net.URI

internal val SONATYPE_SERVER = URI("https://s01.oss.sonatype.org/")

fun RepositoryHandler.configureNexus(project: Project, repository: String = "/content/repositories/public") {
    val NEXUS_URL: String? by project.vars
    val NEXUS_USERNAME: String? by project.vars
    val NEXUS_PASSWORD: String? by project.vars

    if (NEXUS_URL != null && NEXUS_USERNAME != null && NEXUS_PASSWORD != null) {
        maven {
            url = URI(NEXUS_URL).resolve(repository)
            credentials {
                username = NEXUS_USERNAME
                password = NEXUS_PASSWORD
            }
        }
    }
}

fun RepositoryHandler.configureSonatype(project: Project) {
    val OSSRH_SERVER_URL: String? by project.vars
    val serverUrl = OSSRH_SERVER_URL?.takeIf(String::isNotBlank)?.let(::URI) ?: SONATYPE_SERVER
    val mavenUrl = project.uri("${serverUrl.resolve("/service/local/staging/deploy/maven2/")}")

    project.publishing {
        repositories.withType(MavenArtifactRepository::class.java).find {
            it.name == "sonatype"
        }?.apply {
            url = mavenUrl
        } ?: maven {
            name = "sonatype"
            url = mavenUrl
        }
    }
}
