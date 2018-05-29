/*******************************************************************************
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog.command;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Common helper methods for RefLogView command handlers
 */
abstract class AbstractReflogCommandHandler extends AbstractHandler {

	protected IWorkbenchPart getPart(ExecutionEvent event)
			throws ExecutionException {
		return HandlerUtil.getActivePartChecked(event);
	}

	protected Repository getRepository(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = getPart(event);
		if (!(part instanceof ReflogView))
			throw new ExecutionException(
					UIText.AbstractReflogCommandHandler_NoInput);
		return (((ReflogView) part).getRepository());
	}

	protected ReflogView getView() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window == null)
			return null;
		if (window.getActivePage() == null)
			return null;
		IWorkbenchPart part = window.getActivePage().getActivePart();
		if (!(part instanceof ReflogView))
			return null;
		return (ReflogView) part;
	}

	/**
	 * @param event
	 * @param repo
	 * @return commit selected in Reflog View
	 * @throws ExecutionException
	 */
	protected RevCommit getSelectedCommit(ExecutionEvent event, Repository repo)
			throws ExecutionException {
		ReflogEntry entry = (ReflogEntry) ((IStructuredSelection) HandlerUtil
				.getCurrentSelectionChecked(event)).getFirstElement();
		if (entry == null)
			return null;

		RevCommit commit = null;
		try (RevWalk w = new RevWalk(repo)) {
			commit = w.parseCommit(entry.getNewId());
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
		return commit;
	}
}
