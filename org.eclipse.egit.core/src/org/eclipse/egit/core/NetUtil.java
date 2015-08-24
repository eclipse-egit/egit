/*******************************************************************************
 * Copyright (C) 2015, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jgit.lib.Repository;

/**
 * Networking utilities
 */
@SuppressWarnings("restriction")
public class NetUtil {

	private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// no check
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// no check
		}
	} };

	private static HostnameVerifier trustAllHostNames = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			// always accept
			return true;
		}
	};

	/**
	 * Configures a {@link HttpURLConnection} according to the value of the
	 * repositories configuration parameter "http.sslVerify". When this value is
	 * false and when the URL is for the "https" protocol then all hostnames are
	 * accepted and certificates are also accepted when they can't be validated
	 *
	 * @param repo
	 *            the repository to be asked for the configuration parameter
	 *            http.sslVerify
	 * @param conn
	 *            the connection to be configured
	 * @throws IOException
	 */
	public static void setSslVerification(Repository repo,
			HttpURLConnection conn) throws IOException {
		if ("https".equals(conn.getURL().getProtocol())) { //$NON-NLS-1$
			HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
			if (!repo.getConfig().getBoolean("http", "sslVerify", true)) { //$NON-NLS-1$ //$NON-NLS-2$
				try {
					SSLContext ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
					ctx.init(null, trustAllCerts, null);
					httpsConn.setSSLSocketFactory(ctx.getSocketFactory());
					httpsConn.setHostnameVerifier(trustAllHostNames);
				} catch (KeyManagementException e) {
					throw new IOException(e.getMessage());
				} catch (NoSuchAlgorithmException e) {
					throw new IOException(e.getMessage());
				}
			}
		}
	}

	/**
	 * Creates and configures a {@link HttpClient} according to the value of the
	 * repositories configuration parameter "http.sslVerify". When this value is
	 * false and when the URL is for the "https" protocol then all hostnames are
	 * accepted and certificates are also accepted when they can't be validated
	 *
	 * @param repo
	 *            the repository to be asked for the configuration parameter
	 *            http.sslVerify
	 * @return the configured HttpClient
	 * @throws IOException
	 */
	public static CloseableHttpClient createSSLConfiguredHttpClient(
			Repository repo)
			throws IOException {
		CloseableHttpClient httpclient = null;
		X509HostnameVerifier verifier = null;
		try {
			SSLContext sslcontext = null;
			if (!repo.getConfig().getBoolean("http", "sslVerify", true)) { //$NON-NLS-1$ //$NON-NLS-2$
				sslcontext = SSLContext
						.getInstance(SSLConnectionSocketFactory.TLS);
				sslcontext.init(null, trustAllCerts, null);
				verifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
			} else {
				sslcontext = SSLContext.getDefault();
				verifier = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
			}
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslcontext, new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" }, //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					null, verifier);
			httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
					.build();
		} catch (KeyManagementException e) {
			throw new IOException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage());
		}
		return httpclient;
	}
}
