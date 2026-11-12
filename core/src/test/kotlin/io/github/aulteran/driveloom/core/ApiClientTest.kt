package io.github.aulteran.driveloom.core

import io.github.aulteran.driveloom.core.api.ApiClient
import io.github.aulteran.driveloom.core.api.ApiException
import io.github.aulteran.driveloom.core.api.DriveLoomApi
import io.github.aulteran.driveloom.core.api.VehicleInput
import io.github.aulteran.driveloom.core.session.InMemoryKeyValueStore
import io.github.aulteran.driveloom.core.session.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private val server = MockWebServer()
    private val other = MockWebServer()
    private lateinit var store: SessionStore
    private lateinit var api: DriveLoomApi

    @Before fun setUp() {
        server.start()
        other.start()
        store = SessionStore(InMemoryKeyValueStore())
        store.serverUrl = server.url("/").toString().trimEnd('/')
        api = DriveLoomApi(ApiClient(store, "DriveLoom-Android/test"), store)
    }

    @After fun tearDown() {
        server.shutdown()
        other.shutdown()
    }

    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)
    private fun problem(code: Int, body: String) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/problem+json").setBody(body)

    @Test fun `sign-in stores the cookie and csrf token and later requests carry them`() = runBlocking {
        server.enqueue(json("""{"state":"signed_in","csrf_token":"csrf-1"}""").addHeader("Set-Cookie", "dl_session=tok%2Fen; HttpOnly; SameSite=Lax; Path=/; Max-Age=2592000; Secure"))
        server.enqueue(MockResponse().setResponseCode(204))
        api.login(" me@example.com ", "correct horse battery")
        val login = server.takeRequest()
        assertEquals("/v1/auth/login", login.path)
        assertNull(login.getHeader("Cookie"))
        assertNull(login.getHeader("Origin"))
        assertEquals("""{"email":"me@example.com","password":"correct horse battery"}""", login.body.readUtf8())
        assertEquals("tok%2Fen", store.sessionCookie)
        assertEquals("csrf-1", store.csrfToken)

        api.acceptFriend("018f3b1e-7c4a-7d2e-9a10-5b6c7d8e9f01")
        val next = server.takeRequest()
        assertEquals("dl_session=tok%2Fen", next.getHeader("Cookie"))
        assertEquals("csrf-1", next.getHeader("X-CSRF-Token"))
        assertEquals("DriveLoom-Android/test", next.getHeader("User-Agent"))
    }

    @Test fun `safe requests do not send the csrf token`() = runBlocking {
        store.sessionCookie = "abc"
        store.csrfToken = "csrf"
        server.enqueue(json("""{"items":[]}"""))
        api.vehicles(includeArchived = true)
        val req = server.takeRequest()
        assertEquals("/v1/vehicles?includeArchived=true", req.path)
        assertNull(req.getHeader("X-CSRF-Token"))
        assertEquals("dl_session=abc", req.getHeader("Cookie"))
    }

    @Test fun `a cleared cookie signs the client out`() = runBlocking {
        store.sessionCookie = "abc"
        server.enqueue(MockResponse().setResponseCode(204).addHeader("Set-Cookie", "dl_session=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0"))
        api.logout()
        assertNull(store.sessionCookie)
    }

    @Test fun `an expired session clears the store and notifies once`() = runBlocking {
        store.sessionCookie = "abc"
        store.csrfToken = "csrf"
        var notified = 0
        api.http.onSignedOut = { notified++ }
        server.enqueue(problem(401, """{"type":"about:blank","title":"Authentication required","status":401,"code":"unauthenticated"}"""))
        try {
            api.friends(); fail()
        } catch (e: ApiException) {
            assertTrue(e.isUnauthenticated)
        }
        assertNull(store.sessionCookie)
        assertNull(store.csrfToken)
        assertEquals(1, notified)
    }

    @Test fun `a wrong password is an error but not a sign-out`() = runBlocking {
        store.sessionCookie = "abc"
        var notified = 0
        api.http.onSignedOut = { notified++ }
        server.enqueue(problem(401, """{"title":"Invalid credentials","status":401,"code":"invalid_credentials"}"""))
        try {
            api.reauth("wrong", null); fail()
        } catch (e: ApiException) {
            assertEquals("invalid_credentials", e.code)
            assertFalse(e.isUnauthenticated)
        }
        assertEquals("abc", store.sessionCookie)
        assertEquals(0, notified)
    }

    @Test fun `redirects are never followed so the cookie cannot leave the server`() = runBlocking {
        store.sessionCookie = "abc"
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", other.url("/v1/friends").toString()))
        try {
            api.friends(); fail()
        } catch (e: ApiException) {
            assertEquals(ApiException.UNEXPECTED, e.code)
        }
        assertEquals(0, other.requestCount)
    }

    @Test fun `probing another address sends no session and keeps none`() = runBlocking {
        store.sessionCookie = "abc"
        store.csrfToken = "csrf"
        other.enqueue(json("""{"server":"1.0.0","api":"0.6.0","schema":8}""").addHeader("Set-Cookie", "dl_session=evil; Path=/"))
        val v = api.version(other.url("/").toString().trimEnd('/'))
        assertEquals("0.6.0", v.api)
        assertNull(other.takeRequest().getHeader("Cookie"))
        assertEquals("abc", store.sessionCookie)
    }

    @Test fun `a web page instead of an api is reported as unexpected`() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<html>router login</html>"))
        try {
            api.version(); fail()
        } catch (e: ApiException) {
            assertEquals(ApiException.UNEXPECTED, e.code)
        }
        server.enqueue(MockResponse().setResponseCode(404).setHeader("Content-Type", "text/html").setBody("nope"))
        try {
            api.instance(); fail()
        } catch (e: ApiException) {
            assertEquals(ApiException.UNEXPECTED, e.code)
        }
    }

    @Test fun `an unreachable server is a network error`() = runBlocking {
        store.serverUrl = "http://127.0.0.1:1"
        try {
            api.version(); fail()
        } catch (e: ApiException) {
            assertEquals(ApiException.NETWORK, e.code)
        }
    }

    @Test fun `problems keep their code and the import report`() = runBlocking {
        server.enqueue(problem(422, """{"title":"Validation failed","status":422,"code":"import_invalid","detail":"Nothing was imported because some rows are invalid.","report":{"dryRun":false,"committed":false,"summary":{"total":2,"created":1,"duplicates":0,"invalid":1},"errors":[{"row":2,"code":"odometer_out_of_sequence","message":"Odometer runs backwards"}]}}"""))
        try {
            api.importIntoVehicle("v", "csv", kotlinx.serialization.json.JsonPrimitive("a,b"), dryRun = false); fail()
        } catch (e: ApiException) {
            assertEquals("import_invalid", e.code)
            assertEquals(1, e.report!!.summary.invalid)
            assertEquals(2, e.report!!.errors.single().row)
        }
        assertEquals("""{"format":"csv","data":"a,b","dryRun":false}""", server.takeRequest().body.readUtf8())
    }

    @Test fun `unknown fields and enum values from a newer server are tolerated`() = runBlocking {
        server.enqueue(json("""{"items":[{"id":"1","name":"Golf","make":null,"model":null,"year":null,"archived":false,"odometerUnit":"km","currency":"EUR","role":"steward","createdAt":"x","updatedAt":"y","imageVersion":null,"colour":"blue"}]}"""))
        val v = api.vehicles(false).single()
        assertEquals("steward", v.role)
        assertNull(v.imageVersion)
        assertEquals("", v.description)
    }

    @Test fun `clearing vehicle details sends explicit nulls and decimals are strings`() = runBlocking {
        server.enqueue(json("""{"id":"1","name":"Golf","archived":false,"odometerUnit":"km","currency":"EUR","role":"owner","createdAt":"x","updatedAt":"y"}"""))
        api.updateVehicle("1", VehicleInput(" Golf ", make = "", model = " GTI ", year = null, odometerUnit = "km", currency = "EUR"))
        val sent = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals(JsonNull, sent["make"])
        assertEquals(JsonNull, sent["year"])
        assertEquals("\"GTI\"", sent["model"].toString())
        assertEquals("\"Golf\"", sent["name"].toString())
    }

    @Test fun `image upload sends the raw bytes with their type`() = runBlocking {
        store.sessionCookie = "abc"
        store.csrfToken = "csrf"
        server.enqueue(MockResponse().setResponseCode(204))
        api.setVehicleImage("v1", "image/jpeg", byteArrayOf(1, 2, 3))
        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("image/jpeg", req.getHeader("Content-Type"))
        assertEquals(3, req.bodySize)
        assertEquals("csrf", req.getHeader("X-CSRF-Token"))
    }

    @Test fun `export uses the server file name safely`() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/csv").setHeader("Content-Disposition", "attachment; filename=\"../golf.csv\"").setBody("a,b"))
        val file = api.export("v1", "csv")
        assertEquals("golf.csv", file.fileName)
        assertEquals("a,b", file.bytes.toString(Charsets.UTF_8))
        assertEquals("/v1/vehicles/v1/export?format=csv", server.takeRequest().path)
    }

    @Test fun `an oversized answer is refused whether or not its length is declared`() = runBlocking {
        val huge = okio.Buffer().write(ByteArray((ApiClient.MAX_RESPONSE_BYTES + 1).toInt()))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(huge.clone()))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setChunkedBody(huge, 1024 * 1024))
        repeat(2) {
            val e = try { api.vehicles(false); null } catch (e: ApiException) { e }
            assertEquals(ApiException.UNEXPECTED, e?.code)
        }
    }

    @Test fun `unsafe cookie values are never sent`() = runBlocking {
        store.sessionCookie = "abc\r\nX-Injected: 1"
        server.enqueue(json("""{"items":[]}"""))
        api.vehicles(false)
        assertNull(server.takeRequest().getHeader("Cookie"))
        assertNotNull(store.sessionCookie)
    }
}
