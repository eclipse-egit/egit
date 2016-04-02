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
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jsch.core.IJSchService;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

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
		JSch jsch = provider.getJSch();
		File home = fs.userHome();
		if (home == null) {
			home = new File(".").getAbsoluteFile(); //$NON-NLS-1$
		}
		File config = new File(new File(home, ".ssh"), //$NON-NLS-1$
				Constants.CONFIG);
		if (config.canRead()) {
			try {
				jsch.setConfigRepository(
						OpenSSHConfig.parseFile(config.getPath()));
			} catch (IOException e) {
				throw new JSchException(e.getLocalizedMessage(), e);
			}
		}
		return jsch;
	}

	@Override
	protected Session createSession(final OpenSshConfig.Host hc,
			final String user, final String host, final int port, FS fs)
			throws JSchException {
		final JSch jsch = getJSch(hc, fs);
		if (jsch == provider.getJSch()) {
			// If it's the default JSch desired, let the provider
			// manage the session creation for us.
			//
			return provider.createSession(host, port, user);
		} else {
			// This host configuration is using a different IdentityFile,
			// one that is not available through the default JSch.
			//
			jsch.setConfigRepository(provider.getJSch().getConfigRepository());
			return jsch.getSession(user, host, port);
		}
	}

	@Override
	protected void configure(final OpenSshConfig.Host hc, final Session session) {
		UserInfo userInfo = session.getUserInfo();
		if (!hc.isBatchMode() && userInfo == null) {
			final CredentialsProvider cp = CredentialsProvider.getDefault();
			session.setUserInfo(new CredentialsProviderUserInfo(session, cp));
		}
	}
}
