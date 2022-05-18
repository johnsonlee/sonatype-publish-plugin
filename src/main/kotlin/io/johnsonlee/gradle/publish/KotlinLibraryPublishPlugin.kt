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
import org.gradle.util.GradleVersion

open class KotlinLibraryPublishPlugin : AbstractLibraryPublishPlugin() {

    override fun Project.createMavenPublications(
            publications: PublicationContainer,
            config: MavenPublication.() -> Unit
    ) {
        publications.run {
            register("mavenJava", MavenPublication::class) {
                val sourceSets = the<SourceSetContainer>()
                val javadocJar = tasks.register("packageJavadocFor${name.capitalize()}", Jar::class.java) {
                    archiveClassifier.set("javadoc")
                    from(tasks["dokkaHtml"])
                }
                val sourcesJar = tasks.register("packageSourcesFor${name.capitalize()}", Jar::class.java) {
                    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
                    archiveClassifier.set("sources")
                    from(sourceSets["main"].allSource)
                }

                configure(project)
                from(components["java"])
                if (GradleVersion.current() >= GRADLE_6_6) {
                    artifact(javadocJar)
                    artifact(sourcesJar)
                } else {
                    artifact(javadocJar.get())
                    artifact(sourcesJar.get())
                }
                config.invoke(this)
            }
        }
    }

}