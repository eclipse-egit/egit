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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.CreateBranchPage;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;

/**
 * Creates a branch using a simple dialog.
 * <p>
 * This is context-sensitive as it suggests the currently selected branch or (if
 * not started from a branch) the currently checked-out branch as source branch.
 * The user can override this suggestion.
 */
public class CreateBranchCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final RepositoryTreeNode node = getSelectedNodes(event).get(0);
		final Ref baseBranch;
		if (node.getObject() instanceof Ref)
			baseBranch = (Ref) node.getObject();
		else {
			// we are on another node, so we have no Ref as context
			// -> try to determine the currently checked out branch
			Ref branch;
			try {
				if (node.getRepository().getFullBranch().startsWith(
						Constants.R_HEADS)) {
					// simple case: local branch checked out
					branch = node.getRepository().getRef(
							node.getRepository().getFullBranch());
				} else {
					// remote branch or tag checked out: resolve the commit
					String ref = Activator
							.getDefault()
							.getRepositoryUtil()
							.mapCommitToRef(node.getRepository(),
									node.getRepository().getFullBranch(), false);
					if (ref == null)
						branch = null;
					else {
						if (ref.startsWith(Constants.R_TAGS))
							// if a tag is checked out, we don't suggest
							// anything
							branch = null;
						else
							branch = node.getRepository().getRef(ref);
					}
				}
			} catch (IOException e) {
				branch = null;
			}
			baseBranch = branch;
		}

		Wizard wiz = new Wizard() {

			@Override
			public void addPages() {
				addPage(new CreateBranchPage(node.getRepository(), baseBranch));
				setWindowTitle(UIText.RepositoriesView_NewBranchTitle);
			}

			@Override
			public boolean performFinish() {
				try {
					getContainer().run(false, true,
							new IRunnableWithProgress() {

								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									CreateBranchPage cp = (CreateBranchPage) getPages()[0];
									try {
										cp.createBranch(monitor);
									} catch (CoreException ce) {
										throw new InvocationTargetException(ce);
									} catch (IOException ioe) {
										throw new InvocationTargetException(ioe);
									}

								}
							});
				} catch (InvocationTargetException ite) {
					Activator
							.handleError(
									UIText.RepositoriesView_BranchCreationFailureMessage,
									ite.getCause(), true);
					return false;
				} catch (InterruptedException ie) {
					// ignore here
				}
				return true;
			}
		};
		new WizardDialog(getShell(event), wiz).open();

		return null;
	}
}
