package io.github.heisiar.composewizard.shared.services

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparisonTest {

    @Test
    fun `version 1-9-0 is less than 1-10-0-beta01`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.0", "1.10.0-beta01"))
    }

    @Test
    fun `version 1-10-0-beta01 is not less than 1-10-0-beta01`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0-beta01", "1.10.0-beta01"))
    }

    @Test
    fun `version 1-10-0 is not less than 1-10-0-beta01`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0", "1.10.0-beta01"))
    }

    @Test
    fun `version 1-10-0-alpha01 is less than 1-10-0-beta01`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-alpha01", "1.10.0-beta01"))
    }

    @Test
    fun `version 1-9-1 is less than 1-10-0`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.1", "1.10.0"))
    }

    @Test
    fun `version 1-10-0 is not less than 1-9-1`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0", "1.9.1"))
    }

    @Test
    fun `version 1-9-0 is less than 1-9-1`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.0", "1.9.1"))
    }

    @Test
    fun `version 1-8-0 is less than 1-9-0`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.8.0", "1.9.0"))
    }

    @Test
    fun `version 2-0-0 is not less than 1-10-0`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("2.0.0", "1.10.0"))
    }

    @Test
    fun `version with dev suffix is handled correctly`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.0+dev1234", "1.10.0-beta01"))
    }

    @Test
    fun `version with dev suffix in both is compared by base version`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.0+dev1234", "1.10.0+dev5678"))
    }

    @Test
    fun `stable version is not less than pre-release of same version`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0", "1.10.0-beta01"))
    }

    @Test
    fun `pre-release version is less than stable of same version`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-beta01", "1.10.0"))
    }

    @Test
    fun `rc is less than stable`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-rc01", "1.10.0"))
    }

    @Test
    fun `beta is less than rc`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-beta01", "1.10.0-rc01"))
    }

    @Test
    fun `alpha is less than beta`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-alpha01", "1.10.0-beta01"))
    }

    @Test
    fun `same version with different patch numbers`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.0", "1.9.1"))
        assertFalse(VersionComparison.isComposeVersionLessThan("1.9.1", "1.9.0"))
    }

    @Test
    fun `version comparison handles missing patch number`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9", "1.10.0"))
    }

    @Test
    fun `version comparison handles missing minor number`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1", "2.0.0"))
    }

    @Test
    fun `equal versions return false`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("1.9.0", "1.9.0"))
    }

    @Test
    fun `version with higher qualifier number is not less`() {
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0-beta02", "1.10.0-beta01"))
    }

    @Test
    fun `version with lower qualifier number is less`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-beta01", "1.10.0-beta02"))
    }

    @Test
    fun `complex version with multiple suffixes is handled`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.10.0-beta01+dev3194", "1.10.0"))
    }

    @Test
    fun `version 1-10-0-beta01 is the threshold for bundled hot reload`() {
        assertTrue(VersionComparison.isComposeVersionLessThan("1.9.1", "1.10.0-beta01"))
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0-beta01", "1.10.0-beta01"))
        assertFalse(VersionComparison.isComposeVersionLessThan("1.10.0", "1.10.0-beta01"))
    }
}

