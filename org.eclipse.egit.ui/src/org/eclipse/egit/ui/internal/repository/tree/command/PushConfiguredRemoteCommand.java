/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * Copyright (c) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (c) 2011, Dariusz Luksza <dariusz@luksa.org>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org - set action disabled when there is
 *    										no configuration for remotes
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.push.PushOperationUI;
import org.eclipse.egit.ui.internal.push.SimpleConfigurePushDialog;
import org.eclipse.egit.ui.internal.repository.tree.PushNode;
import org.eclipse.egit.ui.internal.repository.tree.RemoteNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.ui.ISources;

/**
 * Pushes to the remote
 */
public class PushConfiguredRemoteCommand extends
		RepositoriesViewCommandHandler<PushNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryTreeNode node = getSelectedNodes(event).get(0);
		RemoteConfig config = getRemoteConfig(node);
		if (config == null) {
			MessageDialog.openInformation(getShell(event),
					UIText.SimplePushActionHandler_NothingToPushDialogTitle,
					UIText.SimplePushActionHandler_NothingToPushDialogMessage);
			return null;
		}
		int timeout = Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
		new PushOperationUI(node.getRepository(), config.getName(), timeout, false)
				.start();
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		if (evaluationContext instanceof IEvaluationContext) {
			IEvaluationContext ctx = (IEvaluationContext) evaluationContext;
			Object selection = ctx
					.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection sel = (IStructuredSelection) selection;
				if (sel.getFirstElement() instanceof RepositoryTreeNode) {
					RepositoryTreeNode node = (RepositoryTreeNode) sel.getFirstElement();
					try {
						setBaseEnabled(getRemoteConfig(node) != null);
					} catch (ExecutionException e) {
						Activator.logError(e.getMessage(), e);
						setBaseEnabled(false);
					}

					return;
				}
			}
		}

		setBaseEnabled(false);
	}

	private RemoteConfig getRemoteConfig(RepositoryTreeNode node)
			throws ExecutionException {
		if (node instanceof PushNode)
			try {
				RemoteNode remote = (RemoteNode) node.getParent();
				return new RemoteConfig(node.getRepository().getConfig(),
						remote.getObject());
			} catch (URISyntaxException e) {
				throw new ExecutionException(e.getMessage());
			}

		if (node instanceof RepositoryNode)
			return SimpleConfigurePushDialog.getConfiguredRemote(node
					.getRepository());

		return null;
	}

}
