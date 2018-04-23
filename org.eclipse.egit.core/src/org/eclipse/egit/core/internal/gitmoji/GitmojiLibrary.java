/*******************************************************************************
 * Copyright (C) 2018, Romain WALLON <romain.wallon@orange.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core.internal.gitmoji;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jgit.util.HttpSupport;

/**
 * A utility class to lazily download the Gitmojis from the
 * <a href="https://github.com/carloscuesta/gitmoji/">GitHub repository</a>, and
 * to keep a reference to them.
 *
 * @since 5.0
 */
public final class GitmojiLibrary {

	/**
	 * The URL of the JSON file in which the Gitmojis are stored.
	 */
	private static final String GITMOJIS_URL = "https://raw.githubusercontent.com/carloscuesta/gitmoji/master/src/data/gitmojis.json"; //$NON-NLS-1$

	/**
	 * The collections of all the existing Gitmojis. They are lazily downloaded
	 * from the GiHub repository.
	 */
	private static Collection<Gitmoji> allGitmojis;

	/**
	 * Disables instantiation.
	 */
	private GitmojiLibrary() {
		throw new AssertionError("No GitmojiLibrary instances for you!"); //$NON-NLS-1$
	}

	/**
	 * Gives the collections of all the existing Gitmojis.
	 *
	 * @return The collections of the Gitmojis. An empty list is returned when
	 *         they could not be downloaded.
	 */
	public static Collection<Gitmoji> gitmojis() {
		if (allGitmojis == null) {
			// The gitmojis have to be loaded.
			download();
		}

		return allGitmojis;
	}

	/**
	 * Downloads the Gitmojis from the GitHub project.
	 */
	private static final void download() {
		try (GitmojiJsonReader reader = new GitmojiJsonReader(openStream())) {
			allGitmojis = Collections.unmodifiableList(reader.read());

		} catch (IOException e) {
			allGitmojis = Collections.emptyList();
		}
	}

	/**
	 * Opens a stream to read the remote file containing the description of the
	 * Gitmoijis. If a proxy is defined, it is used to get the connection.
	 *
	 * @return The stream to use to read the Gitmoji file.
	 *
	 * @throws IOException
	 *             If the connection made to get the file fails.
	 */
	private static InputStream openStream() throws IOException {
		// Finding the proxy to apply.
		URL url = new URL(GITMOJIS_URL);
		Proxy proxy = HttpSupport.proxyFor(ProxySelector.getDefault(), url);

		// Getting the connection to the URL.
		URLConnection connection = url.openConnection(proxy);
		return connection.getInputStream();
	}

}
