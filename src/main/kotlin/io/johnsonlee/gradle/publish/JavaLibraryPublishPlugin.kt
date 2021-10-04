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
                val sourcesJar = tasks.register("sourcesJar${name.capitalize()}", Jar::class.java) {
                    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
                    archiveClassifier.set("sources")
                    from(sourceSets["main"].allSource)
                }
                val javadocJar = tasks.register("javadocJar${name.capitalize()}", Jar::class.java) {
                    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
                    archiveClassifier.set("javadoc")
                    from(tasks["javadoc"])
                }

                configure(project)
                artifact(javadocJar)
                artifact(sourcesJar)
                from(components["java"])
                config.invoke(this)
            }
        }
    }

}