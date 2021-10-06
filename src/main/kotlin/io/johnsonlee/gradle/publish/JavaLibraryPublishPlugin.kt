package io.johnsonlee.gradle.publish

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

open class JavaLibraryPublishPlugin : AbstractLibraryPublishPlugin() {

    override fun Project.createMavenPublications(
            publications: PublicationContainer,
            config: MavenPublication.() -> Unit
    ) {
        publications.run {
            register("mavenJava", MavenPublication::class) {
                val sourceSets = the<SourceSetContainer>()
                val javadocJar = tasks.register("packageJavadocFor${name.capitalize()}", Jar::class.java) {
                    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
                    archiveClassifier.set("javadoc")
                    from(tasks["javadoc"])
                }
                val sourcesJar = tasks.register("packageSourcesFor${name.capitalize()}", Jar::class.java) {
                    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
                    archiveClassifier.set("sources")
                    from(sourceSets["main"].allSource)
                }

                configure(project)
                from(components["java"])
                artifact(javadocJar)
                artifact(sourcesJar)
                config.invoke(this)
            }
        }
    }

}