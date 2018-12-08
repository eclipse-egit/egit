/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/** Initializes plugin preferences with default values. */
public class GitCorePreferenceInitializer extends AbstractPreferenceInitializer {
	private static final int MB = 1024 * 1024;

	@Override
	public void initializeDefaultPreferences() {
		final IEclipsePreferences p  = DefaultScope.INSTANCE.getNode(Activator.getPluginId());

		p.putInt(GitCorePreferences.core_packedGitWindowSize, 8 * 1024);
		p.putInt(GitCorePreferences.core_packedGitLimit, 10 * MB);
		p.putBoolean(GitCorePreferences.core_packedGitMMAP, false);
		p.putInt(GitCorePreferences.core_deltaBaseCacheLimit, 10 * MB);
		p.putInt(GitCorePreferences.core_streamFileThreshold, 50 * MB);
		p.putBoolean(GitCorePreferences.core_autoShareProjects, true);
		p.putBoolean(GitCorePreferences.core_autoIgnoreDerivedResources, true);
		p.putBoolean(GitCorePreferences.core_autoStageDeletion, false);
		p.putBoolean(GitCorePreferences.core_autoStageMoves, true);

		String defaultRepoDir = RepositoryUtil.getDefaultDefaultRepositoryDir();
		p.put(GitCorePreferences.core_defaultRepositoryDir, defaultRepoDir);
		p.putInt(GitCorePreferences.core_maxPullThreadsCount, 1);
		p.put(GitCorePreferences.core_sshClient, "apache"); //$NON-NLS-1$
	}

}
