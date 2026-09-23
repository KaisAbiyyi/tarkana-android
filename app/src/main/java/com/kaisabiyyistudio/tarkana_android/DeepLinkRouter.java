package com.kaisabiyyistudio.tarkana_android;

import android.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Parses and routes incoming deep link URIs for Tarkana.
 * In P1.7, unsupported native flows (/share/* and /duel/*) resolve to explicit safe web fallback.
 */
public final class DeepLinkRouter {

    public enum Destination {
        SAFE_WEB_FALLBACK_SHARE,
        SAFE_WEB_FALLBACK_DUEL,
        MAIN_APP,
        UNKNOWN
    }

    public static final class Resolution {
        public final Destination destination;
        public final String publicId;
        public final String canonicalWebUrl;

        public Resolution(Destination destination, String publicId, String canonicalWebUrl) {
            this.destination = destination;
            this.publicId = publicId;
            this.canonicalWebUrl = canonicalWebUrl;
        }
    }

    private DeepLinkRouter() {}

    public static Resolution resolve(Uri androidUri, String configuredHost) {
        if (androidUri == null) {
            return new Resolution(Destination.UNKNOWN, null, null);
        }
        return resolve(androidUri.toString(), configuredHost);
    }

    public static Resolution resolve(String rawUrl, String configuredHost) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return new Resolution(Destination.UNKNOWN, null, null);
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            return new Resolution(Destination.UNKNOWN, null, null);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        if (path == null) {
            path = "";
        }

        if (scheme == null) {
            return new Resolution(Destination.UNKNOWN, null, null);
        }

        boolean isHttps = "https".equalsIgnoreCase(scheme);
        boolean isCustomScheme = "tarkana".equalsIgnoreCase(scheme);

        if (!isHttps && !isCustomScheme) {
            return new Resolution(Destination.UNKNOWN, null, null);
        }

        String fallbackHost = (configuredHost != null && !configuredHost.trim().isEmpty())
                ? configuredHost.trim()
                : "tarkana.vercel.app";

        if (isCustomScheme) {
            String shareId = null;
            String duelId = null;

            if ("share".equalsIgnoreCase(host)) {
                shareId = (path.startsWith("/")) ? path.substring(1).trim() : path.trim();
            } else if ("duel".equalsIgnoreCase(host)) {
                duelId = (path.startsWith("/")) ? path.substring(1).trim() : path.trim();
            } else if (path.startsWith("/share/")) {
                shareId = path.substring("/share/".length()).trim();
            } else if (path.startsWith("/duel/")) {
                duelId = path.substring("/duel/".length()).trim();
            }

            if (shareId != null && isValidPublicId(shareId)) {
                String webUrl = "https://" + fallbackHost + "/share/" + shareId;
                return new Resolution(Destination.SAFE_WEB_FALLBACK_SHARE, shareId, webUrl);
            }
            if (duelId != null && isValidPublicId(duelId)) {
                String webUrl = "https://" + fallbackHost + "/duel/" + duelId;
                return new Resolution(Destination.SAFE_WEB_FALLBACK_DUEL, duelId, webUrl);
            }
            if ((host == null || host.isEmpty()) && (path.isEmpty() || path.equals("/"))) {
                return new Resolution(Destination.MAIN_APP, null, null);
            }
            return new Resolution(Destination.UNKNOWN, null, null);
        }

        // For https, verify host matches configured canonical domain
        if (configuredHost != null && !configuredHost.trim().isEmpty()) {
            if (host == null || !configuredHost.equalsIgnoreCase(host)) {
                return new Resolution(Destination.UNKNOWN, null, null);
            }
        }

        String targetHost = (host != null && !host.isEmpty()) ? host : fallbackHost;

        // Clean path and extract public ID
        if (path.startsWith("/share/")) {
            String publicId = path.substring("/share/".length()).trim();
            if (isValidPublicId(publicId)) {
                String webUrl = "https://" + targetHost + "/share/" + publicId;
                return new Resolution(Destination.SAFE_WEB_FALLBACK_SHARE, publicId, webUrl);
            }
        } else if (path.startsWith("/duel/")) {
            String publicId = path.substring("/duel/".length()).trim();
            if (isValidPublicId(publicId)) {
                String webUrl = "https://" + targetHost + "/duel/" + publicId;
                return new Resolution(Destination.SAFE_WEB_FALLBACK_DUEL, publicId, webUrl);
            }
        } else if (path.isEmpty() || path.equals("/")) {
            return new Resolution(Destination.MAIN_APP, null, null);
        }

        return new Resolution(Destination.UNKNOWN, null, null);
    }

    private static boolean isValidPublicId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9_-]{1,64}$");
    }
}
