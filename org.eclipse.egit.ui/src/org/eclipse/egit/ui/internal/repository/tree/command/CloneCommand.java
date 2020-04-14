/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
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
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Clones a Repository by calling the clone wizard.
 */
public class CloneCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	private String presetURI;

	/**
	 * Default constructor
	 */
	public CloneCommand() {
		this(null);
	}

	/**
	 * Constructor support presetURI
	 *
	 * @param presetURI
	 */
	public CloneCommand(String presetURI) {
		this.presetURI = presetURI;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		GitCloneWizard wizard;
		if (presetURI == null) {
			wizard = new GitCloneWizard();
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
