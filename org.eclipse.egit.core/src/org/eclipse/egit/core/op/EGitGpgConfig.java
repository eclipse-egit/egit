/*******************************************************************************
 * Copyright (C) 2024, Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;

import org.eclipse.egit.core.internal.signing.SigningSetup;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.util.StringUtils;

/**
 * An EGit-specific {@link GpgConfig} that ensures that the executable defined
 * in Eclipse is taken if one is specified.
 */
public class EGitGpgConfig extends GpgConfig {

	private String exe;

	/**
	 * Creates a new instance.
	 *
	 * @param config
	 *            git config to use
	 */
	public EGitGpgConfig(Config config) {
		super(config);
	}

	@Override
	public String getProgram() {
		// The EGit setting is an override in case whatever is defined in the
		// git config doesn't work in Eclipse. (Or the user, for whatever
		// reason, prefers to use different programs in C git and in EGit.)
		GpgFormat format = getKeyFormat();
		switch (format) {
		case OPENPGP:
		case X509:
			if (exe == null) {
				exe = determineProgram(GpgFormat.X509.equals(format));
			}
			if (!StringUtils.isEmptyOrNull(exe)) {
				return exe;
			}
			break;
		default:
			break;
		}
		// No EGit setting: just use whatever is configured in the git config.
		String fromConfig = super.getProgram();
		if (!StringUtils.isEmptyOrNull(fromConfig)) {
			return fromConfig;
		}
		return null;
	}

	private String determineProgram(boolean x509) {
		if (SigningSetup.Signer.GPG.equals(SigningSetup.getSigner())) {
			File f = GitSettings.getGpgExecutable();
			if (f != null) {
				if (!x509) {
					return f.getAbsolutePath();
				}
				File parent = f.getParentFile();
				String name = f.getName().replace("gpg", "gpgsm"); //$NON-NLS-1$ //$NON-NLS-2$
				File gpgsm = new File(parent, name);
				if (gpgsm.isFile() && gpgsm.canExecute()) {
					return gpgsm.getAbsolutePath();
				}
			}
		}
		return ""; //$NON-NLS-1$
	}
}
