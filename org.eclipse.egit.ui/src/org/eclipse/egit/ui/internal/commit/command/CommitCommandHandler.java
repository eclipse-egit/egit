/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit.command;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.handler.SelectionHandler;

/**
 * Commit command handler
 */
public abstract class CommitCommandHandler extends SelectionHandler {

	/**
	 * Get commits in current selection
	 *
	 * @param event
	 * @return non-null but possibly empty list of commits
	 */
	protected List<RepositoryCommit> getCommits(ExecutionEvent event) {
		return getSelectedItems(RepositoryCommit.class, event);
	}
}
