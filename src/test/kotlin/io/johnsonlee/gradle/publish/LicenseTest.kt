package io.johnsonlee.gradle.publish

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class LicenseTest {

    @Test
    fun `check license`() {
        val license = License.of(File(System.getProperty("user.dir"), "LICENSE"))
        assertEquals(License.APACHE_2_0, license)
    }

}