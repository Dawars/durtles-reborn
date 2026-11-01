package com.example.durtlesreborn.settings

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TokenStoreTest {
    /** Reverses the text; enough to prove the store never writes the plain token. */
    private class FakeCipher(var broken: Boolean = false) : TokenCipher {
        override fun encrypt(plain: String) = "enc:" + plain.reversed()
        override fun decrypt(encoded: String) = if (broken) null else encoded.removePrefix("enc:").reversed()
    }

    private fun dataStore() = InMemoryDataStore()

    @Test
    fun savedTokenComesBackTrimmedAndIsStoredEncrypted() = runBlocking {
        val ds = dataStore()
        val store = TokenStore(ds, FakeCipher())
        assertNull(store.current())
        store.save("  abc-123  ")
        assertEquals("abc-123", store.current())
        val raw = ds.data.first()[stringPreferencesKey("api_token")]!!
        assertFalse(raw.contains("abc-123"))
    }

    @Test
    fun clearRemovesTheToken() = runBlocking {
        val store = TokenStore(dataStore(), FakeCipher())
        store.save("abc")
        store.clear()
        assertNull(store.current())
    }

    @Test
    fun anUndecryptableTokenReadsAsMissing() = runBlocking {
        val cipher = FakeCipher()
        val store = TokenStore(dataStore(), cipher)
        store.save("abc")
        cipher.broken = true
        assertNull(store.current())
    }
}
