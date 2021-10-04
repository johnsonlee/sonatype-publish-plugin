package io.johnsonlee.gradle.publish

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.get

class AndroidLibraryPublishPlugin : AbstractLibraryPublishPlugin() {

    override fun Project.createMavenPublications(
            publications: PublicationContainer,
            config: MavenPublication.() -> Unit
    ) {
        val android = extensions.getByName("android") as LibraryExtension

        publications.run {
            android.libraryVariants.forEach { variant ->
                val javadocs = tasks.register("androidJavadocs${variant.name.capitalize()}", Javadoc::class.java) {
                    dependsOn("dokkaHtml")
                    source(android.sourceSets["main"].java.srcDirs)
                    classpath += files(android.bootClasspath + variant.javaCompileProvider.get().classpath)
                    exclude("**/R.html", "**/R.*.html", "**/index.html")
                }

                val javadocsJar = tasks.register("androidJavadocsJar${variant.name.capitalize()}", Jar::class.java) {
                    dependsOn(javadocs)
                    archiveClassifier.set("javadoc")
                    from(tasks["dokkaHtml"])
                }

                val sourcesJar = tasks.register("androidSourcesJar${variant.name.capitalize()}", Jar::class.java) {
                    archiveClassifier.set("sources")
                    from(android.sourceSets["main"].java.srcDirs)
                }

                create(variant.name, MavenPublication::class.java) {
                    configure(project)
                    from(components[variant.name])
                    artifact(javadocsJar)
                    artifact(sourcesJar)
                    config.invoke(this)
                }
            }
        }
    }

}