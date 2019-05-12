/*******************************************************************************
 * Copyright (C) 2015, Christian Halstrick <christian.halstrick@sap.com>
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.HttpConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Networking utilities
 */
public class NetUtil {

	private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// no check
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// no check
		}
	} };

	private static HostnameVerifier trustAllHostNames = (hostname,
			session) -> true; // always accept

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
			try {
				HttpConfig http = new HttpConfig(repo.getConfig(),
						new URIish(conn.getURL().toString()));
				if (!http.isSslVerify()) {
					SSLContext ctx = SSLContext.getInstance("TLS"); //$NON-NLS-1$
					ctx.init(null, trustAllCerts, null);
					httpsConn.setSSLSocketFactory(ctx.getSocketFactory());
					httpsConn.setHostnameVerifier(trustAllHostNames);
				}
			} catch (KeyManagementException | NoSuchAlgorithmException
					| URISyntaxException e) {
				throw new IOException(e.getMessage(), e);
			}
		}
	}
}
