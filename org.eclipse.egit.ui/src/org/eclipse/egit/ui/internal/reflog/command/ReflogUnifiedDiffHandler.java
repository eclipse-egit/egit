/******************************************************************************
 *  Copyright (c) 2020 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.reflog.command;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.commit.command.UnifiedDiffHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Shows a unified diff between two {@link ReflogEntry} objects from the reflog
 * view.
 */
public class ReflogUnifiedDiffHandler extends AbstractReflogCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(event);
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 2) {
				List<?> items = ssel.toList();
				ReflogEntry first = Adapters.adapt(items.get(0),
						ReflogEntry.class);
				ReflogEntry second = Adapters.adapt(items.get(1),
						ReflogEntry.class);
				RevCommit tip = null;
				RevCommit base = null;
				try (RevWalk w = new RevWalk(repository)) {
					if (first != null) {
						tip = w.parseCommit(first.getNewId());
					}
					if (second != null) {
						base = w.parseCommit(second.getNewId());
					}
				} catch (IOException e) {
					throw new ExecutionException(e.getMessage(), e);
				}
				if (tip != null && base != null) {
					UnifiedDiffHandler.show(
							new RepositoryCommit(repository, tip),
							new RepositoryCommit(repository, base));
				}
			}
		}
		return null;
	}

}
