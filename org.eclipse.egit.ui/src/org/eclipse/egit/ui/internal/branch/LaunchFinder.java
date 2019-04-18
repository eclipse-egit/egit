/*******************************************************************************
 * Copyright (c) 2013, 2016 Robin Stocker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf <thomas.wolf@paranor.ch>
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class for finding launch configurations.
 */
public final class LaunchFinder {
	private static final IDebugUIPluginFacade debugPluginFacade;

	static {
		if (hasDebugUiBundle()) {
			debugPluginFacade = new DebugUIPluginFacade();
		} else {
			debugPluginFacade = new NoopDebugUIPluginFacade();
		}
	}

	private static final boolean hasDebugUiBundle() {
		try {
			return Class.forName(
					"org.eclipse.debug.core.ILaunchConfiguration") != null; //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private LaunchFinder() {
		// Utility class shall not be instantiated
	}

	/**
	 * If there is a running launch covering at least one project from the given
	 * repositories, return the name of the first such launch configuration.
	 *
	 * @param repositories
	 *            to determine projects to be checked whether they are used in
	 *            running launches
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @return the launch name, or {@code null} if none found.
	 */
	@Nullable
	public static String getRunningLaunchConfiguration(
			final Collection<Repository> repositories,
			IProgressMonitor monitor) {
		return debugPluginFacade.getRunningLaunchConfigurationName(repositories,
				monitor);
	}

	/**
	 * Checks whether there are any running launches based on projects belonging
	 * to the given repository. If so, asks the user whether to cancel, and
	 * returns the user's choice. The user has the possibility to suppress the
	 * dialog, in which case this method returns {@code false} without checking
	 * for running launches.
	 *
	 * @param repository
	 *            to determine projects to be checked whether they are used in
	 *            running launches
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @return {@code true} if the operation should be canceled, {@code false}
	 *         otherwise
	 */
	public static boolean shouldCancelBecauseOfRunningLaunches(
			Repository repository, IProgressMonitor monitor) {
		return shouldCancelBecauseOfRunningLaunches(
				Collections.singleton(repository), monitor);
	}

	/**
	 * Checks whether there are any running launches based on projects belonging
	 * to the given repositories. If so, asks the user whether to cancel, and
	 * returns the user's choice. The user has the possibility to suppress the
	 * dialog, in which case this method returns {@code false} without checking
	 * for running launches.
	 *
	 * @param repositories
	 *            to determine projects to be checked whether they are used in
	 *            running launches
	 * @param monitor
	 *            for progress reporting and cancellation
	 * @return {@code true} if the operation should be canceled, {@code false}
	 *         otherwise
	 */
	public static boolean shouldCancelBecauseOfRunningLaunches(
			Collection<Repository> repositories, IProgressMonitor monitor) {
		final IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		if (!store.getBoolean(
				UIPreferences.SHOW_RUNNING_LAUNCH_ON_CHECKOUT_WARNING)) {
			return false;
		}
		SubMonitor progress = SubMonitor.convert(monitor);
		final String launchConfiguration = getRunningLaunchConfiguration(
				repositories,
				progress);
		if (launchConfiguration != null) {
			final boolean[] dialogResult = new boolean[1];
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					dialogResult[0] = showContinueDialogInUI(store,
							launchConfiguration);
				}
			});
			return dialogResult[0];
		}
		return false;
	}

	private static boolean showContinueDialogInUI(final IPreferenceStore store,
			final String launchConfiguration) {
		String[] buttons = new String[] { UIText.BranchOperationUI_Continue,
				IDialogConstants.CANCEL_LABEL };
		String message = NLS.bind(UIText.LaunchFinder_RunningLaunchMessage,
				launchConfiguration) + ' '
				+ UIText.LaunchFinder_ContinueQuestion;
		MessageDialogWithToggle continueDialog = new MessageDialogWithToggle(
				PlatformUI.getWorkbench().getModalDialogShellProvider()
						.getShell(),
				UIText.LaunchFinder_RunningLaunchTitle, null,
				message, MessageDialog.NONE, buttons, 0,
				UIText.LaunchFinder_RunningLaunchDontShowAgain, false);
		int result = continueDialog.open();
		// cancel
		if (result == IDialogConstants.CANCEL_ID || result == SWT.DEFAULT)
			return true;
		boolean dontWarnAgain = continueDialog.getToggleState();
		if (dontWarnAgain)
			store.setValue(
					UIPreferences.SHOW_RUNNING_LAUNCH_ON_CHECKOUT_WARNING,
					false);
		return false;
	}

}
