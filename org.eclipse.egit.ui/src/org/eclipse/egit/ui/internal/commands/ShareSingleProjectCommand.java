/*******************************************************************************
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.internal.sharing.SharingWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Provides a handler for the Share Project command. This can then be bound to
 * whatever keybinding the user prefers.
 *
 * @since 0.6.0
 */
public class ShareSingleProjectCommand extends AbstractHandler {

	private static final String PROJECT_NAME_PARAMETER = "org.eclipse.egit.ui.command.projectNameParameter"; //$NON-NLS-1$

	/**
	 * Invokes 'Configure Git Repository' dialog to share given project.
	 *
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final String projectName = event.getParameter(PROJECT_NAME_PARAMETER);
		final IProject projectToShare = ResourcesPlugin.getWorkspace()
				.getRoot().getProject(projectName);
		IWorkbench workbench = HandlerUtil.getActiveWorkbenchWindow(event)
				.getWorkbench();

		final SharingWizard wizard = new SharingWizard();
		wizard.init(workbench, projectToShare);
		final Shell shell = HandlerUtil.getActiveShell(event);
		WizardDialog wizardDialog = new WizardDialog(shell, wizard);
		wizardDialog.setHelpAvailable(false);
		wizardDialog.open();
		return null;
	}

}
