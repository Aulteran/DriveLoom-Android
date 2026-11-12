package io.github.aulteran.driveloom.core.session

/** Small persistent string store. The Android implementation encrypts values at rest. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val map = HashMap<String, String>()
    @Synchronized override fun get(key: String): String? = map[key]
    @Synchronized override fun put(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
}

/**
 * What the client remembers between launches: the server origin, whether
 * plain HTTP was explicitly allowed for it, and the session cookie value.
 * The CSRF token is deliberately held in memory only, like the web client;
 * it is fetched again from `/v1/auth/session` on start.
 */
class SessionStore(private val kv: KeyValueStore) {
    var serverUrl: String?
        get() = kv.get(KEY_SERVER)
        set(value) = kv.put(KEY_SERVER, value)

    /** Raw `dl_session` cookie value, exactly as the server set it. */
    var sessionCookie: String?
        get() = kv.get(KEY_COOKIE)
        set(value) = kv.put(KEY_COOKIE, value)

    var themeMode: String
        get() = kv.get(KEY_THEME) ?: THEME_DARK
        set(value) = kv.put(KEY_THEME, value)

    @Volatile var csrfToken: String? = null

    fun clearSession() {
        sessionCookie = null
        csrfToken = null
    }

    fun clearAll() {
        clearSession()
        serverUrl = null
    }

    companion object {
        private const val KEY_SERVER = "server_url"
        private const val KEY_COOKIE = "session_cookie"
        private const val KEY_THEME = "theme_mode"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        const val THEME_SYSTEM = "system"
    }
}
