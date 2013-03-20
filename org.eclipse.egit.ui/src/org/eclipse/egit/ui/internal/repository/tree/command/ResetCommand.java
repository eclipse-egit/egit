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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.op.ResetOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.SelectResetTypePage;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.osgi.util.NLS;

/**
 * "Resets" a repository
 */
public class ResetCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode<?>> {

	/**
	 * Command id
	 */
	public static final String ID = "org.eclipse.egit.ui.team.Reset"; //$NON-NLS-1$

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final RepositoryTreeNode<?> node = getSelectedNodes(event).get(0);
		final String currentBranch;
		try {
			currentBranch = node.getRepository().getFullBranch();
		} catch (IOException e1) {
			throw new ExecutionException(e1.getMessage(), e1);
		}
		final String targetBranch;
		if (node.getObject() instanceof Ref)
			targetBranch = ((Ref) node.getObject()).getName();
		else
			targetBranch = currentBranch;
		final String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(node.getRepository());

		Wizard wiz = new Wizard() {

			@Override
			public void addPages() {
				addPage(new SelectResetTypePage(repoName, node.getRepository(),
						currentBranch, targetBranch));
				setWindowTitle(UIText.ResetCommand_WizardTitle);
			}

			@Override
			public boolean performFinish() {
				final ResetType resetType = ((SelectResetTypePage) getPages()[0])
						.getResetType();
				if (resetType == ResetType.HARD)
					if (!MessageDialog
							.openQuestion(
									getShell(),
									UIText.ResetTargetSelectionDialog_ResetQuestion,
									UIText.ResetTargetSelectionDialog_ResetConfirmQuestion))
						return true;

				try {
					getContainer().run(false, true,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {

									String jobname = NLS.bind(
											UIText.ResetAction_reset,
											targetBranch);
									final ResetOperation operation = new ResetOperation(
											node.getRepository(), targetBranch,
											resetType);
									JobUtil.scheduleUserJob(operation, jobname,
											JobFamilies.RESET);
								}
							});
				} catch (InvocationTargetException ite) {
					Activator.handleError(
							UIText.ResetCommand_ResetFailureMessage, ite
									.getCause(), true);
					return false;
				} catch (InterruptedException ie) {
					// ignore here
				}
				return true;
			}
		};
		WizardDialog dlg = new WizardDialog(getShell(event), wiz);
		dlg.setHelpAvailable(false);
		dlg.open();

		return null;
	}
}
