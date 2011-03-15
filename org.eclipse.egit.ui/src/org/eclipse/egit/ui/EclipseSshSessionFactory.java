/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2009, Google, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.ui.UserInfoPrompter;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

class EclipseSshSessionFactory extends JschConfigSessionFactory {
	private final IJSchService provider;

	EclipseSshSessionFactory(final IJSchService p) {
		provider = p;
	}

	@Override
	protected JSch createDefaultJSch(FS fs) throws JSchException {
		// Forcing a dummy session to be created will cause the known hosts
		// and configured private keys to be initialized. This is needed by
		// our parent class in case non-default JSch instances need to be made.
		//
		provider.createSession("127.0.0.1", 0, "eclipse"); //$NON-NLS-1$ //$NON-NLS-2$
		return provider.getJSch();
	}

	@Override
	protected Session createSession(final OpenSshConfig.Host hc,
			final String user, final String host, final int port, FS fs)
			throws JSchException {
		final JSch jsch = getJSch(hc, FS.DETECTED);
		if (jsch == provider.getJSch()) {
			// If its the default JSch desired, let the provider
			// manage the session creation for us.
			//
			return provider.createSession(host, port, user);
		} else {
			// This host configuration is using a different IdentityFile,
			// one that is not available through the default JSch.
			//
			return jsch.getSession(user, host, port);
		}
	}

	@Override
	protected void configure(final OpenSshConfig.Host hc, final Session session) {
		if (!hc.isBatchMode() && session.getUserInfo() == null) {
			new UserInfoPrompter(session);
		}
	}
}
