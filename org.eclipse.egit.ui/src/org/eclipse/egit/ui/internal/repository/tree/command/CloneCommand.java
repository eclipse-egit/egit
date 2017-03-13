/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.clone.GitCloneWizard;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
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
		if (presetURI == null)
			wizard = new GitCloneWizard();
		else
			wizard = new GitCloneWizard(presetURI);
		wizard.setShowProjectImport(true);
		WizardDialog dlg = new WizardDialog(getShell(event), wizard);
		dlg.setHelpAvailable(true);
		dlg.open();
		return null;
	}
}
