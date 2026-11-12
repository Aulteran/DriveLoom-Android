package io.github.aulteran.driveloom.core

import io.github.aulteran.driveloom.core.api.ApiCompat
import io.github.aulteran.driveloom.core.api.ServerUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlTest {
    private fun ok(input: String, insecure: Boolean = false) = (ServerUrl.parse(input, insecure) as ServerUrl.Result.Ok).origin

    @Test fun `a bare host gets https`() = assertEquals("https://fuel.example.com", ok("fuel.example.com"))

    @Test fun `path query and fragment are dropped`() =
        assertEquals("https://fuel.example.com", ok("  https://Fuel.Example.com/#/signin?x=1  "))

    @Test fun `a non-default port is kept and a default one dropped`() {
        assertEquals("https://fuel.example.com:8443", ok("fuel.example.com:8443"))
        assertEquals("https://fuel.example.com", ok("https://fuel.example.com:443/"))
    }

    @Test fun `plain http needs explicit consent`() {
        val refused = ServerUrl.parse("http://192.168.1.20:8080", allowInsecure = false)
        assertEquals(ServerUrl.Result.InsecureNotAllowed("http://192.168.1.20:8080"), refused)
        assertEquals("http://192.168.1.20:8080", ok("http://192.168.1.20:8080", insecure = true))
    }

    @Test fun `credentials in the address are refused`() =
        assertTrue(ServerUrl.parse("https://me:secret@fuel.example.com", false) is ServerUrl.Result.Invalid)

    @Test fun `other schemes and junk are refused`() {
        for (bad in listOf("", "   ", "ftp://fuel.example.com", "javascript:alert(1)", "https://", "fuel example.com", "file:///etc/passwd")) {
            assertTrue(bad, ServerUrl.parse(bad, true) is ServerUrl.Result.Invalid)
        }
    }

    @Test fun `ipv6 hosts keep their brackets`() = assertEquals("https://[::1]:8443", ok("https://[::1]:8443/"))

    @Test fun `api compatibility`() {
        assertEquals(ApiCompat.Level.SUPPORTED, ApiCompat.check("0.6.0"))
        assertEquals(ApiCompat.Level.SUPPORTED, ApiCompat.check("0.12.3"))
        assertEquals(ApiCompat.Level.TOO_OLD, ApiCompat.check("0.5.9"))
        assertEquals(ApiCompat.Level.NEWER_MAJOR, ApiCompat.check("1.0.0"))
        assertEquals(ApiCompat.Level.UNKNOWN, ApiCompat.check("banana"))
    }
}

class InvitesTest {
    @Test fun `a bare code and a whole link give the same code`() {
        val invites = io.github.aulteran.driveloom.core.session.Invites
        assertEquals("abc-123", invites.codeFrom("  abc-123 "))
        assertEquals("a+b/c=", invites.codeFrom("https://fuel.example.com/#/register?invite=a%2Bb%2Fc%3D"))
        assertEquals("tok", invites.codeFrom("https://fuel.example.com/#/register?invite=tok&x=1"))
        assertEquals("a+b/c=", invites.codeFrom(invites.link("https://fuel.example.com", "a+b/c=")))
        assertEquals("%zz", invites.codeFrom("invite=%zz"))
    }
}
