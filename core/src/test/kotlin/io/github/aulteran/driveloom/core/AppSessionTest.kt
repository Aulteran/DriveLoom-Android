package io.github.aulteran.driveloom.core

import io.github.aulteran.driveloom.core.api.ApiClient
import io.github.aulteran.driveloom.core.api.DriveLoomApi
import io.github.aulteran.driveloom.core.session.AppSession
import io.github.aulteran.driveloom.core.session.AppState
import io.github.aulteran.driveloom.core.session.InMemoryKeyValueStore
import io.github.aulteran.driveloom.core.session.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A tiny stand-in for the server's sign-in behaviour, driven by flags. */
private class FakeServer : Dispatcher() {
    var apiVersion = "0.6.0"
    var mfa: String? = null
    var validCookie: String? = null
    val paths = ArrayList<String>()

    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)
    private fun unauthenticated() = MockResponse().setResponseCode(401).setHeader("Content-Type", "application/problem+json")
        .setBody("""{"title":"Authentication required","status":401,"code":"unauthenticated"}""")

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path!!.substringBefore('?')
        paths += "${request.method} $path"
        val cookie = request.getHeader("Cookie")?.removePrefix("dl_session=")
        val signedIn = validCookie != null && cookie == validCookie
        return when ("${request.method} $path") {
            "GET /v1/version" -> json("""{"server":"1.2.3","api":"$apiVersion","schema":8}""")
            "GET /v1/auth/instance" -> json("""{"registration":"closed","password_login":true,"oidc":false,"email_recovery":false,"needs_setup":false,"setup_token_required":false}""")
            "POST /v1/auth/login" -> {
                if (mfa != null) {
                    json("""{"state":"mfa_required","csrf_token":"c0","mfa_method":"$mfa"}""").addHeader("Set-Cookie", "dl_session=pending; Path=/; Max-Age=600")
                } else {
                    validCookie = "s1"
                    json("""{"state":"signed_in","csrf_token":"c1"}""").addHeader("Set-Cookie", "dl_session=s1; Path=/; Max-Age=600")
                }
            }
            "POST /v1/auth/mfa/totp" -> {
                validCookie = "s2"
                json("""{"state":"signed_in","csrf_token":"c2"}""").addHeader("Set-Cookie", "dl_session=s2; Path=/; Max-Age=600")
            }
            "GET /v1/auth/session" -> if (signedIn) json("""{"csrf_token":"c9","session_id":"sid","account":{"id":"u1","email":"me@example.com","displayName":"Me","instanceRole":"admin","emailVerified":true,"credentials":{"password":true,"totp":false,"recoveryCodesRemaining":0,"passkeys":[],"oidc":[]}}}""") else unauthenticated()
            "GET /v1/preferences" -> if (signedIn) json("""{"distanceUnit":"mi","volumeUnit":"gal_us","economyUnit":"mpg_us","currency":"USD"}""") else unauthenticated()
            "GET /v1/friends" -> if (signedIn) json("""{"friends":[],"incoming":[],"outgoing":[]}""") else unauthenticated()
            "POST /v1/auth/logout" -> {
                validCookie = null
                MockResponse().setResponseCode(204).addHeader("Set-Cookie", "dl_session=; Path=/; Max-Age=0")
            }
            else -> MockResponse().setResponseCode(404)
        }
    }
}

class AppSessionTest {
    private val server = MockWebServer()
    private val fake = FakeServer()
    private lateinit var store: SessionStore
    private lateinit var api: DriveLoomApi
    private lateinit var session: AppSession
    private lateinit var origin: String

    @Before fun setUp() {
        server.dispatcher = fake
        server.start()
        origin = server.url("/").toString().trimEnd('/')
        store = SessionStore(InMemoryKeyValueStore())
        api = DriveLoomApi(ApiClient(store), store)
        session = AppSession(api, store)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `first launch asks for a server`() = runBlocking {
        session.start()
        assertEquals(AppState.NeedsServer, session.state.value)
    }

    @Test fun `plain http is not saved without consent`() = runBlocking {
        val result = session.connect(origin, allowInsecure = false)
        assertEquals(AppSession.ConnectResult.NeedsInsecureConsent(origin), result)
        assertNull(store.serverUrl)
        assertTrue(fake.paths.isEmpty())
    }

    @Test fun `an old server is refused and nothing is saved`() = runBlocking {
        fake.apiVersion = "0.4.0"
        assertTrue(session.connect(origin, true) is AppSession.ConnectResult.Failed)
        assertNull(store.serverUrl)
    }

    @Test fun `connect then password sign-in reaches signed in`() = runBlocking {
        assertEquals(AppSession.ConnectResult.Connected, session.connect(origin, true))
        assertTrue(session.state.value is AppState.SignedOut)
        session.login("me@example.com", "pw")
        val s = session.state.value as AppState.SignedIn
        assertEquals("Me", s.account.label)
        assertTrue(s.account.isAdmin)
        assertEquals("mpg_us", s.preferences.economyUnit)
        assertEquals("c9", store.csrfToken)
        assertEquals("s1", store.sessionCookie)
    }

    @Test fun `a second factor is asked for and completes`() = runBlocking {
        fake.mfa = "totp"
        session.connect(origin, true)
        session.login("me@example.com", "pw")
        assertEquals("totp", (session.state.value as AppState.MfaRequired).method)
        session.completeTotp("123456", recovery = false)
        assertTrue(session.state.value is AppState.SignedIn)
        assertEquals("s2", store.sessionCookie)
    }

    @Test fun `cancelling the second step drops the pending session`() = runBlocking {
        fake.mfa = "email"
        session.connect(origin, true)
        session.login("me@example.com", "pw")
        session.cancelMfa()
        assertTrue(session.state.value is AppState.SignedOut)
        assertNull(store.sessionCookie)
    }

    @Test fun `a stored session is restored on launch`() = runBlocking {
        store.serverUrl = origin
        store.sessionCookie = "s1"
        fake.validCookie = "s1"
        session.start()
        assertTrue(session.state.value is AppState.SignedIn)
        assertEquals("1.2.3", session.serverVersion?.server)
    }

    @Test fun `a stale stored session falls back to sign-in`() = runBlocking {
        store.serverUrl = origin
        store.sessionCookie = "old"
        session.start()
        assertTrue(session.state.value is AppState.SignedOut)
        assertNull(store.sessionCookie)
    }

    @Test fun `an unreachable server keeps the address and offers a retry`() = runBlocking {
        store.serverUrl = "http://127.0.0.1:1"
        store.sessionCookie = "s1"
        session.start()
        assertTrue(session.state.value is AppState.Unreachable)
        assertEquals("s1", store.sessionCookie)
    }

    @Test fun `a session that ends mid-use returns to sign-in with a notice`() = runBlocking {
        session.connect(origin, true)
        session.login("me@example.com", "pw")
        fake.validCookie = null
        runCatching { api.friends() }
        val s = session.state.value as AppState.SignedOut
        assertTrue(s.notice!!.contains("session ended"))
    }

    @Test fun `sign out and forget server clear what is stored`() = runBlocking {
        session.connect(origin, true)
        session.login("me@example.com", "pw")
        session.logout()
        assertTrue(session.state.value is AppState.SignedOut)
        assertNull(store.sessionCookie)
        assertEquals(origin, store.serverUrl)
        session.forgetServer()
        assertEquals(AppState.NeedsServer, session.state.value)
        assertNull(store.serverUrl)
    }
}
