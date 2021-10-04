package io.johnsonlee.gradle.publish

import org.gradle.internal.impldep.org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'io.johnsonlee.gradle.publish.greeting' plugin.
 */
class NexusPublishPluginTest {

    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.johnsonlee.sonatype-publish-plugin")
    }

    @Test
    fun `read git remote origin url`() {
        val git = File(System.getProperty("user.dir"), ".git")
        val url = FileRepositoryBuilder().setGitDir(git).findGitDir().build().config.getString("remote", "origin", "url")
        assertNotNull(url)
    }

}
