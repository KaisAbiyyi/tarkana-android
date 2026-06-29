package com.kaisabiyyistudio.tarkana_android;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class ApiClient {
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;

    private ApiClient() {}

    static ApiResponse getFunction(Context context, String path) throws Exception {
        return requestFunction(context, "GET", path, null);
    }

    static ApiResponse postFunction(Context context, String path, JSONObject body) throws Exception {
        return requestFunction(context, "POST", path, body == null ? new JSONObject() : body);
    }

    static ApiResponse requestFunction(
            Context context,
            String method,
            String path,
            JSONObject body
    ) throws Exception {
        String token = AuthSession.accessToken(context);
        if (token == null || token.isEmpty()) {
            token = AuthSession.refreshAccessToken(context);
        }
        if (token == null || token.isEmpty()) {
            throw new AuthException("No active session token. Please log out and log in again.");
        }

        ApiResponse response = execute(method, functionUrl(path), token, body);
        if (response.code != 401) return response;

        String refreshedToken = AuthSession.refreshAccessToken(context);
        if (refreshedToken == null || refreshedToken.isEmpty()) {
            throw new AuthException("Function returned 401 and token refresh failed: " + response.body);
        }

        response = execute(method, functionUrl(path), refreshedToken, body);
        if (response.code == 401) {
            throw new AuthException("Function returned 401 after token refresh: " + response.body);
        }
        return response;
    }

    private static URL functionUrl(String path) throws Exception {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return new URL(BuildConfig.SUPABASE_URL + "/functions/v1" + normalizedPath);
    }

    private static ApiResponse execute(String method, URL url, String token, JSONObject body)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setUseCaches(false);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Encoding", "identity");

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
        }

        int code = conn.getResponseCode();
        String responseBody = readStream(code < 400 ? conn.getInputStream() : conn.getErrorStream());
        conn.disconnect();
        return new ApiResponse(code, responseBody);
    }

    static String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        try (InputStream input = stream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static boolean isCancellation(Throwable error) {
        return Thread.currentThread().isInterrupted() || error instanceof InterruptedIOException;
    }

    static final class ApiResponse {
        final int code;
        final String body;

        ApiResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }

        JSONObject json() throws Exception {
            return body == null || body.isEmpty() ? new JSONObject() : new JSONObject(body);
        }

        void requireSuccess() throws ApiException {
            if (code < 200 || code >= 300) throw new ApiException(code, body);
        }
    }

    static class ApiException extends Exception {
        final int code;

        ApiException(int code, String body) {
            super("Request failed (" + code + "): " + body);
            this.code = code;
        }
    }

    static final class AuthException extends ApiException {
        AuthException(String message) {
            super(401, message);
        }
    }
}
