/*******************************************************************************
 * Copyright (c) 2016, Matthias Sohn <matthias.sohn@sap.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Mathias Kinzler (SAP AG) - use the abstract super class
 *    Thomas Wolf <thomas.wolf@paranor.ch> - Bug 477248
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;
import java.util.Collections;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.branch.LaunchFinder;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to confirm a potentially destructive command
 */
public class CommandConfirmation {
	/**
	 * Ask the user to confirm hard reset. Warns the user if a running launch
	 * could be affected by the reset.
	 *
	 * @param shell
	 * @param repo
	 * @return {@code true} if the user confirmed hard reset
	 */
	public static boolean confirmHardReset(Shell shell, final Repository repo) {
		String question = UIText.ResetTargetSelectionDialog_ResetConfirmQuestion;
		ILaunchConfiguration launch = LaunchFinder
				.getRunningLaunchConfiguration(Collections.singleton(repo),
						null);
		if (launch != null) {
			question = MessageFormat.format(question,
					"\n\n" + MessageFormat.format( //$NON-NLS-1$
							UIText.LaunchFinder_RunningLaunchMessage,
							launch.getName()));
		} else {
			question = MessageFormat.format(question, ""); //$NON-NLS-1$
		}

		MessageDialog messageDialog = new MessageDialog(shell,
				UIText.ResetTargetSelectionDialog_ResetQuestion, null, question,
				MessageDialog.QUESTION,
				new String[] {
						UIText.CommandConfirmationHardResetDialog_resetButtonLabel,
						IDialogConstants.CANCEL_LABEL },
				0);

		return messageDialog.open() == Window.OK;
	}
}
