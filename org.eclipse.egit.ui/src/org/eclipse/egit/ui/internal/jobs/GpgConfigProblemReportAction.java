/*******************************************************************************
 *  Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.ErrorDialogWithHelp;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * An action to show a dedicated status dialog for problems relating to the GPG
 * configuration.
 */
public class GpgConfigProblemReportAction extends Action {

	private IStatus status;

	private String message;

	/**
	 * Create a new instance.
	 *
	 * @param status
	 *            to report
	 * @param message
	 *            for the dialog
	 */
	public GpgConfigProblemReportAction(IStatus status, String message) {
		super(UIText.GpgConfigProblemReportAction_Title);
		this.status = status;
		this.message = message;
	}

	@Override
	public void run() {
		ErrorDialogWithHelp dialog = new ErrorDialogWithHelp(
				PlatformUI.getWorkbench().getModalDialogShellProvider()
						.getShell(),
				UIText.GpgConfigProblemReportAction_Title, message, status,
				"org.eclipse.egit.ui.gpgSigning"); //$NON-NLS-1$
		dialog.open();
	}
}
