/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens Baumgart (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements committing to a repository
 */
public class CommitCommand extends
		RepositoriesViewCommandHandler<RepositoryNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryNode node = getSelectedNodes(event).get(0);
		Repository repository = node.getRepository();
		if (RepositoryMapping.findRepositoryMapping(repository) == null) {
			MessageDialog.openInformation(getShell(event),
					UIText.CommitCommand_committingNotPossible,
					UIText.CommitCommand_noProjectsImported);
			return null;
		}
		// preselect files in all projects related to the repository
		IProject[] selectedProjects = ProjectUtil.getProjects(repository);
		new CommitUI(getShell(event), new Repository[] { repository },
				selectedProjects).commit();
		return null;
	}
}
