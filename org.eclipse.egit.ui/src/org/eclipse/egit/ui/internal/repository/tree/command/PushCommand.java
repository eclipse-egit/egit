/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.push.PushBranchWizard;
import org.eclipse.egit.ui.internal.push.PushTagsWizard;
import org.eclipse.egit.ui.internal.push.PushWizard;
import org.eclipse.egit.ui.internal.push.PushWizardDialog;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements "Push" from a Repository
 */
public class PushCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<RepositoryTreeNode> nodes = getSelectedNodes(event);
		RepositoryTreeNode node = nodes.get(0);

		IWizard pushWiz = null;

		try {
			switch (node.getType()) {
			case REF:
				Ref ref = (Ref) node.getObject();
				pushWiz = new PushBranchWizard(node.getRepository(), ref);
				break;
			case TAG:
				pushWiz = createPushTagsWizard(nodes);
				break;
			case REPO:
				pushWiz = new PushWizard(node.getRepository());
				break;
			default:
				throw new UnsupportedOperationException("type not supported!"); //$NON-NLS-1$
			}
		} catch (URISyntaxException e1) {
			Activator.handleError(e1.getMessage(), e1, true);
			return null;
		}

		WizardDialog dlg = new PushWizardDialog(getShell(event), pushWiz);
		dlg.setHelpAvailable(pushWiz.isHelpAvailable());
		dlg.open();

		return null;
	}

	private PushTagsWizard createPushTagsWizard(List<RepositoryTreeNode> nodes) {
		List<String> tagNames = new ArrayList<>();
		for (RepositoryTreeNode node : nodes) {
			if (node instanceof TagNode) {
				TagNode tagNode = (TagNode) node;
				tagNames.add(tagNode.getObject().getName());
			}
		}
		Repository repository = nodes.get(0).getRepository();
		return new PushTagsWizard(repository, tagNames);
	}

	@Override
	public boolean isEnabled() {
		List<RepositoryTreeNode> nodes = getSelectedNodes();
		if (nodes.isEmpty())
			return false;
		Repository repository = nodes.get(0).getRepository();
		for (RepositoryTreeNode node : nodes) {
			if (repository != node.getRepository())
				return false;
		}
		return selectedRepositoryHasHead();
	}
}
