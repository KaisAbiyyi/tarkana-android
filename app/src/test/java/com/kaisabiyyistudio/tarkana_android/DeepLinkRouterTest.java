package com.kaisabiyyistudio.tarkana_android;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DeepLinkRouterTest {

    private static final String CONFIGURED_HOST = "tarkana.vercel.app";

    @Test
    public void testResolveValidShareLink() {
        String url = "https://tarkana.vercel.app/share/shr_a1b2c3d4";
        DeepLinkRouter.Resolution resolution = DeepLinkRouter.resolve(url, CONFIGURED_HOST);

        assertEquals(DeepLinkRouter.Destination.SAFE_WEB_FALLBACK_SHARE, resolution.destination);
        assertEquals("shr_a1b2c3d4", resolution.publicId);
        assertEquals("https://tarkana.vercel.app/share/shr_a1b2c3d4", resolution.canonicalWebUrl);
    }

    @Test
    public void testResolveValidDuelLink() {
        String url = "https://tarkana.vercel.app/duel/duel_987654321";
        DeepLinkRouter.Resolution resolution = DeepLinkRouter.resolve(url, CONFIGURED_HOST);

        assertEquals(DeepLinkRouter.Destination.SAFE_WEB_FALLBACK_DUEL, resolution.destination);
        assertEquals("duel_987654321", resolution.publicId);
        assertEquals("https://tarkana.vercel.app/duel/duel_987654321", resolution.canonicalWebUrl);
    }

    @Test
    public void testResolveCustomSchemeFallback() {
        String shareUrl = "tarkana://share/shr_test123";
        DeepLinkRouter.Resolution resShare = DeepLinkRouter.resolve(shareUrl, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.SAFE_WEB_FALLBACK_SHARE, resShare.destination);
        assertEquals("shr_test123", resShare.publicId);
        assertEquals("https://tarkana.vercel.app/share/shr_test123", resShare.canonicalWebUrl);

        String duelUrl = "tarkana://duel/duel_test456";
        DeepLinkRouter.Resolution resDuel = DeepLinkRouter.resolve(duelUrl, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.SAFE_WEB_FALLBACK_DUEL, resDuel.destination);
        assertEquals("duel_test456", resDuel.publicId);
        assertEquals("https://tarkana.vercel.app/duel/duel_test456", resDuel.canonicalWebUrl);
    }

    @Test
    public void testRejectMismatchedHost() {
        String url = "https://evil.com/share/shr_attacker123";
        DeepLinkRouter.Resolution resolution = DeepLinkRouter.resolve(url, CONFIGURED_HOST);

        assertEquals(DeepLinkRouter.Destination.UNKNOWN, resolution.destination);
        assertNull(resolution.publicId);
        assertNull(resolution.canonicalWebUrl);
    }

    @Test
    public void testRejectMalformedPublicId() {
        String traversalUrl = "https://tarkana.vercel.app/share/../../../etc/passwd";
        DeepLinkRouter.Resolution resTraversal = DeepLinkRouter.resolve(traversalUrl, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.UNKNOWN, resTraversal.destination);

        String emptyShare = "https://tarkana.vercel.app/share/";
        DeepLinkRouter.Resolution resEmpty = DeepLinkRouter.resolve(emptyShare, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.UNKNOWN, resEmpty.destination);

        String specialChars = "https://tarkana.vercel.app/duel/<script>alert(1)</script>";
        DeepLinkRouter.Resolution resSpecial = DeepLinkRouter.resolve(specialChars, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.UNKNOWN, resSpecial.destination);
    }

    @Test
    public void testRootNavigatesToMainApp() {
        String rootUrl = "https://tarkana.vercel.app/";
        DeepLinkRouter.Resolution res = DeepLinkRouter.resolve(rootUrl, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.MAIN_APP, res.destination);
    }

    @Test
    public void testNullOrEmptyUrl() {
        DeepLinkRouter.Resolution resNull = DeepLinkRouter.resolve((String) null, CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.UNKNOWN, resNull.destination);

        DeepLinkRouter.Resolution resEmpty = DeepLinkRouter.resolve("", CONFIGURED_HOST);
        assertEquals(DeepLinkRouter.Destination.UNKNOWN, resEmpty.destination);
    }
}
