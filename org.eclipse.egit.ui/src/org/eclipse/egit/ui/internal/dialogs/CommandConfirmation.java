/*******************************************************************************
 * Copyright (c) 2016, Matthias Sohn <matthias.sohn@sap.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jface.dialogs.MessageDialog;
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
		return MessageDialog.openQuestion(shell,
				UIText.ResetTargetSelectionDialog_ResetQuestion, question);
	}
}
