package com.example.app.util;

import com.example.app.common.Constants;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Utility class to create HTTP clients with mTLS support
 */
public class HttpClientUtil {

    /**
     * Creates an HTTP client configured with mTLS using the client certificate
     * from the keystore specified in environment variables.
     * 
     * @return
     *         CloseableHttpClient instance configured with mTLS
     * 
     * @throws Exception
     */
    public static CloseableHttpClient createMTLSHttpClient() throws Exception {

        // Check if mTLS is configured
        if (Constants.MTLS_KEYSTORE_PATH == null || Constants.MTLS_KEYSTORE_PATH.isEmpty()) {
            System.out.println("mTLS not configured, using default HTTP client");
            return HttpClients.createDefault();
        }

        // Load the client keystore
        KeyStore keyStore = KeyStore.getInstance(Constants.MTLS_KEYSTORE_TYPE);
        try (FileInputStream keystoreStream = new FileInputStream(Constants.MTLS_KEYSTORE_PATH)) {
            keyStore.load(keystoreStream, Constants.MTLS_KEYSTORE_PASSWORD.toCharArray());
        }

        // Build SSL context with the client certificate
        SSLContext sslContext =

                SSLContextBuilder

                        .create()

                        .loadKeyMaterial(keyStore, Constants.MTLS_KEYSTORE_PASSWORD.toCharArray())

                        .loadTrustMaterial(new TrustSelfSignedStrategy())

                        .build();

        // Create SSL socket factory with NoopHostnameVerifier for development
        // In production, use proper certificate validation
        SSLConnectionSocketFactory sslSocketFactory =

                new SSLConnectionSocketFactory(

                        sslContext,

                        NoopHostnameVerifier.INSTANCE

                );

        // Build and return HTTP client with mTLS support
        return HttpClients

                .custom()

                .setSSLSocketFactory(sslSocketFactory)

                .build();

    }

    /**
     * Creates a default HTTP client without mTLS.
     * Useful for backward compatibility or when mTLS is not required.
     * 
     * @return CloseableHttpClient without mTLS
     */
    public static CloseableHttpClient createDefaultHttpClient() {
        return HttpClients.createDefault();
    }
}

