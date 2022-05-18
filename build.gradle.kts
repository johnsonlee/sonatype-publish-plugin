import org.gradle.api.Project.DEFAULT_VERSION
import org.gradle.kotlin.dsl.*

plugins {
    `java-gradle-plugin`
    `maven-publish`
    `kotlin-dsl`
    `signing`
    kotlin("jvm") version embeddedKotlinVersion
    id("org.jetbrains.dokka") version "1.4.32"
    id("io.codearte.nexus-staging") version "0.22.0"
    id("de.marcphilipp.nexus-publish") version "0.4.0"
}

group = "io.johnsonlee"
version = project.properties["version"]?.takeIf { it != DEFAULT_VERSION } ?: "1.0.0-SNAPSHOT"
description = "Gradle plugin for publishing artifacts to Sonatype"

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))
    implementation("de.marcphilipp.gradle:nexus-publish-plugin:0.4.0")
    implementation("io.codearte.nexus-staging:io.codearte.nexus-staging.gradle.plugin:0.22.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    compileOnly("com.android.tools.build:gradle:4.0.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("com.didiglobal.booster:booster-kotlinx:4.8.0")
}

gradlePlugin {
    plugins.create("io.johnsonlee.sonatype-publish-plugin") {
        id = name
        implementationClass = "io.johnsonlee.gradle.publish.SonatypePublishPlugin"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn("dokkaHtml")
    archiveClassifier.set("javadoc")
    from(tasks["dokkaHtml"])
}

val OSSRH_USERNAME = "${project.properties["OSSRH_USERNAME"] ?: System.getenv("OSSRH_USERNAME")}"
val OSSRH_PASSWORD = "${project.properties["OSSRH_PASSWORD"] ?: System.getenv("OSSRH_PASSWORD")}"

nexusPublishing {
    repositories {
        sonatype {
            username.set(OSSRH_USERNAME)
            password.set(OSSRH_PASSWORD)
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        }
    }
    publications {
        withType(MavenPublication::class.java).configureEach {
            val publication = this

            groupId = "${project.group}"
            artifactId = project.name
            version = "${project.version}"

            if ("mavenJava" == publication.name) {
                from(components["java"])
            }

            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom.withXml {
                asNode().apply {
                    appendNode("name", project.name)
                    appendNode("url", "https://github.com/johnsonlee/${rootProject.name}")
                    appendNode("description", project.description ?: project.name)
                    appendNode("scm").apply {
                        appendNode("connection", "scm:git:git://github.com/johnsonlee/${rootProject.name}.git")
                        appendNode("developerConnection", "scm:git:git@github.com:johnsonlee/${rootProject.name}.git")
                        appendNode("url", "https://github.com/johnsonlee/${rootProject.name}")
                    }
                    appendNode("licenses").apply {
                        appendNode("license").apply {
                            appendNode("name", "Apache License")
                            appendNode("url", "https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    appendNode("developers").apply {
                        appendNode("developer").apply {
                            appendNode("id", "johnsonlee")
                            appendNode("name", "Johnson Lee")
                            appendNode("email", "g.johnsonlee@gmail.com")
                        }
                    }
                }
            }

            signing {
                sign(publication)
            }
        }
    }
}


nexusStaging {
    packageGroup = "io.johnsonlee"
    username = OSSRH_USERNAME
    password = OSSRH_PASSWORD
    numberOfRetries = 50
    delayBetweenRetriesInMillis = 3000
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.check {
    dependsOn(functionalTest)
}
