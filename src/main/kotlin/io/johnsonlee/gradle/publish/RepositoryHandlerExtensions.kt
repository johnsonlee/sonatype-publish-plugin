package io.johnsonlee.gradle.publish

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

internal val SONATYPE_SERVER = URI("https://oss.sonatype.org/")

fun RepositoryHandler.configureNexus(project: Project, repository: String = "/content/repositories/public") {
    if (project.NEXUS_URL != null && project.NEXUS_USERNAME != null && project.NEXUS_PASSWORD != null) {
        val nexusUrl = project.NEXUS_URL!!.resolve(repository)
        maven {
            url = nexusUrl
            credentials {
                username = project.NEXUS_USERNAME
                password = project.NEXUS_PASSWORD
            }
        }
        project.logger.info("[${project}] Configuring nexus {url=`${nexusUrl}`} completed")
    } else {
        project.logger.warn("[${project}] Configuring nexus skipped: `NEXUS_URL` or `NEXUS_USERNAME` or `NEXUS_PASSWORD` not found")
    }
}

fun RepositoryHandler.configureSonatype(project: Project) {
    val sonatype = maven {
        url = project.OSSRH_SERVER_URL.resolve("/service/local/staging/deploy/maven2/")
    }
    project.logger.info("[${project}] Configuring sonatype repository {url=`${sonatype.url}`} completed")
}
