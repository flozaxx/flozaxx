package com.flozaxx.applock.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinVaultTest {

    // The Keystore does not exist on the JVM; the stretching and comparison are what is under test.
    private val vault = PinVault(seal = { it })

    @Test
    fun `only the original pin matches`() {
        val credential = vault.create("482913")
        assertTrue(vault.matches("482913", credential))
        assertFalse(vault.matches("482914", credential))
        assertEquals(6, credential.length)
    }

    @Test
    fun `same pin is stored differently every time`() {
        assertNotEquals(vault.create("1234").hash, vault.create("1234").hash)
    }

    @Test
    fun `recovery code is accepted however it is typed`() {
        val code = PinVault.newRecoveryCode()
        assertTrue(Regex("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}").matches(code))

        val credential = vault.create(PinVault.normalizeRecoveryCode(code))
        val typed = code.lowercase().replace("-", " ")
        assertTrue(vault.matches(PinVault.normalizeRecoveryCode(typed), credential))
    }
}
