package io.johnsonlee.gradle.publish

import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

class GradlePluginPublishPlugin : AbstractLibraryPublishPlugin() {

    override fun Project.createMavenPublications(
            publications: PublicationContainer,
            config: MavenPublication.() -> Unit
    ) {
        publications.run {
            val sourceSets = the<SourceSetContainer>()
            val javadocsJar = tasks.register("packageJavadoc${project.name.capitalize()}", Jar::class.java) {
                archiveClassifier.set("javadoc")
                from(tasks["javadoc"])
            }
            val sourcesJar = tasks.register("packageSources${project.name.capitalize()}", Jar::class.java) {
                archiveClassifier.set("sources")
                from(sourceSets["main"].allSource)
            }
            withType<MavenPublication>().configureEach {
                artifact(javadocsJar)
                artifact(sourcesJar)
                config.invoke(this)
            }
        }

    }

}