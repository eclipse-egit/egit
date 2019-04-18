/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.LfsFactory;
import org.eclipse.jgit.util.LfsFactory.LfsInstallCommand;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;

/**
 * Checks the system configuration
 *
 */
public class ConfigurationChecker {

	/**
	 * Checks the system configuration.
	 */
	public static void checkConfiguration() {
		// Schedule a job
		// This avoids that the check is executed too early
		// because in startup phase the JobManager is suspended
		// and scheduled Jobs are executed later
		Job job = new Job(UIText.ConfigurationChecker_checkConfiguration) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (PlatformUI.isWorkbenchRunning()) {
					PlatformUI.getWorkbench().getDisplay()
							.asyncExec(new Runnable() {
								@Override
								public void run() {
									check();
								}
							});
				} else {
					schedule(1000L);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private static void check() {
		checkHome();
		checkLfs();
	}

	private static void checkLfs() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		boolean auto = store.getBoolean(UIPreferences.LFS_AUTO_CONFIGURATION);
		if (auto && !isLfsConfigured()) {
			try {
				LfsInstallCommand cmd = LfsFactory.getInstance()
						.getInstallCommand();
				if (cmd != null) {
					cmd.call();
				}
			} catch (Exception e) {
				Activator.handleIssue(IStatus.WARNING,
						UIText.ConfigurationChecker_installLfsCannotInstall, e,
						true);
			}
		}
	}

	private static boolean isLfsConfigured() {
		try {
			StoredConfig cfg = SystemReader.getInstance().openUserConfig(null,
					FS.DETECTED);
			cfg.load();
			return cfg.getSubsections(ConfigConstants.CONFIG_FILTER_SECTION)
					.contains("lfs"); //$NON-NLS-1$
		} catch (Exception e) {
			Activator.handleIssue(IStatus.WARNING,
					UIText.ConfigurationChecker_installLfsCannotLoadConfig, e, false);
		}
		return false;
	}

	private static void checkHome() {
		String home = System.getenv("HOME"); //$NON-NLS-1$
		if (home != null)
			return; // home is set => ok
		home = calcHomeDir();
		String message = NLS.bind(UIText.ConfigurationChecker_homeNotSet, home);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		boolean hidden = !store.getBoolean(UIPreferences.SHOW_HOME_DIR_WARNING);
		if (!hidden)
			Activator.handleIssue(IStatus.WARNING, message, null, false);
	}

	private static String calcHomeDir() {
		if (runsOnWindows()) {
			String homeDrive = System.getenv("HOMEDRIVE"); //$NON-NLS-1$
			if (homeDrive != null) {
				String homePath = SystemReader.getInstance().getenv("HOMEPATH"); //$NON-NLS-1$
				return new File(homeDrive, homePath).getAbsolutePath();
			}
			return System.getenv("HOMESHARE"); //$NON-NLS-1$
		} else {
			// The user.home property is not compatible with Git for Windows
			return System.getProperty("user.home"); //$NON-NLS-1$
		}
	}

	private static boolean runsOnWindows() {
		String os;
		try {
			os = System.getProperty("os.name"); //$NON-NLS-1$
		} catch (RuntimeException e) {
			return false;
		}
		return os.contains("Windows"); //$NON-NLS-1$
	}

}
