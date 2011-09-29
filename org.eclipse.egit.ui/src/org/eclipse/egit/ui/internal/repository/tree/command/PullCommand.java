/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza (dariusz@luksza.org) - disable command when HEAD cannot be
 *    										resolved
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.pull.PullOperationUI;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jgit.lib.Repository;

/**
 * Implements "Pulling" using the upstream configuration of the currently
 * checked out branch
 */
public class PullCommand extends RepositoriesViewCommandHandler<RepositoryNode> {
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Set<Repository> repositories = new HashSet<Repository>();
		for (RepositoryNode node : getSelectedNodes(event)) {
			if (node.getRepository() != null)
				repositories.add(node.getRepository());
		}

		if (repositories.isEmpty())
			return null;
		new PullOperationUI(repositories).start();
		return null;
	}

	public void setEnabled(Object evaluationContext) {
		enableWhenAllRepositoriesHaveHead(evaluationContext);
	}

}
