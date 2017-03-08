/*******************************************************************************
 * Copyright (c) 2011, 2017 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Ian Pun - factored out of RepositorySelectionPage
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.net.URISyntaxException;

import org.eclipse.egit.ui.internal.KnownHosts;
import org.eclipse.egit.ui.internal.components.RepositorySelectionPage.Protocol;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportProtocol;
import org.eclipse.jgit.transport.URIish;

/**
 * Utility class for checking strings for being valid git URLs, and for
 * sanitizing arbitrary string input.
 */
public abstract class GitUrlChecker {

	private static final String GIT_CLONE_COMMAND_PREFIX = "git clone "; //$NON-NLS-1$

	/**
	 * Checks if the incoming string is a valid git URL. It is recommended to
	 * {@link #sanitizeAsGitUrl(String) sanitize} the String first if coming
	 * from an untrustworthy source.
	 *
	 * @param url
	 *            to check
	 * @return {@code true} if the {@code url} is a valid git URL, {@code false}
	 *         otherwise
	 */
	public static boolean isValidGitUrl(String url) {
		try {
			if (url != null) {
				URIish u = new URIish(url);
				if (canHandleProtocol(u)) {
					if (Protocol.GIT.handles(u) || Protocol.SSH.handles(u)
							|| (Protocol.HTTP.handles(u)
									|| Protocol.HTTPS.handles(u))
									&& KnownHosts.isKnownHost(u.getHost())
							|| url.endsWith(Constants.DOT_GIT_EXT)) {
						return true;
					}
				}
			}
		} catch (URISyntaxException e) {
			// Ignore. This is used to check arbitrary input for being a
			// possibly valid git URL; we don't want to flood the log here.
		}
		return false;
	}

	/**
	 * Sanitize a string for use as a git URL. Strips the Git Clone commanded if
	 * needed, reduces the input to the first line if it has multiple lines, and
	 * trims any trailing whitespace.
	 *
	 * @param input
	 *            String to be sanitized
	 * @return sanitized string; if the input came from an untrustworthy source,
	 *         is should still be checked using {@link #isValidGitUrl(String)}
	 *         before being used for real as a git URL
	 */
	public static String sanitizeAsGitUrl(String input) {
		// Split on any whitespace character
		String sanitized = input.split("[\\h|\\v]", 2)[0]; //$NON-NLS-1$
		sanitized = sanitized.trim();
		if (sanitized.startsWith(GIT_CLONE_COMMAND_PREFIX)) {
			sanitized = sanitized.substring(GIT_CLONE_COMMAND_PREFIX.length())
					.trim();
		}
		return sanitized;
	}

	private static boolean canHandleProtocol(URIish u) {
		for (TransportProtocol proto : Transport.getTransportProtocols()) {
			if (proto.canHandle(u)) {
				return true;
			}
		}
		return false;
	}

}
