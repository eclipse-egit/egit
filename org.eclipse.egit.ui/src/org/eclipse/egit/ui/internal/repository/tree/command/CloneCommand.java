/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.clone.GitCloneWizard;
import org.eclipse.egit.ui.internal.clone.GitUrlChecker;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Clones a Repository by calling the clone wizard.
 */
public class CloneCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	/** The command id. */
	public static final String COMMAND_ID = "org.eclipse.egit.ui.RepositoriesViewClone"; //$NON-NLS-1$

	/** Id of the optional command parameter for the repository URI. */
	public static final String REPOSITORY_URI_PARAMETER_ID = "repositoryUri"; //$NON-NLS-1$

	private String presetURI;

	/**
	 * Creates a command that will open the clone wizard pre-filled from either
	 * the {@link #REPOSITORY_URI_PARAMETER_ID} command parameter or from the
	 * clipboard contents.
	 */
	public CloneCommand() {
		this(null);
	}

	/**
	 * Creates a command that will open the clone wizard and pre-fill it with
	 * the given URI, which is assumed to be valid.
	 *
	 * @param presetURI
	 *            to fill the dialog with.
	 */
	public CloneCommand(String presetURI) {
		this.presetURI = presetURI;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		GitCloneWizard wizard;

		if (presetURI == null) {
			String uriParameter = event
					.getParameter(REPOSITORY_URI_PARAMETER_ID);
			if (uriParameter != null) {
				String sanitizedURI = GitUrlChecker
						.sanitizeAsGitUrl(uriParameter);

				wizard = GitUrlChecker.isValidGitUrl(sanitizedURI)
						? new GitCloneWizard(sanitizedURI)
						: new GitCloneWizard();
			} else {
				wizard = new GitCloneWizard();
			}
		} else {
			wizard = new GitCloneWizard(presetURI);
		}

		RepositoryGroup group = getSelectedRepositoryGroup(event);
		wizard.setRepositoryGroup(group);
		wizard.setShowProjectImport(true);
		WizardDialog dlg = new WizardDialog(getShell(event), wizard);
		dlg.setHelpAvailable(true);
		if (dlg.open() == Window.OK) {
			expandRepositoryGroup(event, group);
		}
		return null;
	}
}
