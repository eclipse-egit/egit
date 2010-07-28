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
package org.eclipse.egit.ui.internal.history.command;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract superclass for history view based command handlers.
 * <p>
 * This provides some convenience methods for concrete subclasses
 */
public abstract class AbstractHistoryViewCommandHandler extends AbstractHandler
		implements IHandler {
	/**
	 * @param event
	 * @return the selection
	 * @throws ExecutionException
	 */
	@SuppressWarnings("unchecked")
	protected List<PlotCommit> getSelection(ExecutionEvent event)
			throws ExecutionException {
		try {
			ISelection selection = HandlerUtil
					.getCurrentSelectionChecked(event);
			return ((StructuredSelection) selection).toList();
		} catch (ClassCastException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * @param event
	 * @return the selection
	 * @throws ExecutionException
	 */
	protected PlotCommit getSingleCommit(ExecutionEvent event)
			throws ExecutionException {
		List<PlotCommit> selection = getSelection(event);
		if (selection.size() == 1)
			return selection.get(0);
		throw new ExecutionException(
				UIText.AbstractHitoryViewCommandHandler_CanNotGetCommitMessage);
	}

	/**
	 * @param event
	 * @return the history page
	 * @throws ExecutionException
	 */
	private GitHistoryPage getPage(ExecutionEvent event)
			throws ExecutionException {
		try {
			IHistoryView view = (IHistoryView) HandlerUtil
					.getActivePartChecked(event);
			return (GitHistoryPage) view.getHistoryPage();
		} catch (ClassCastException e) {
			throw new ExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * @param event
	 * @return the {@link Repository} of the history view's input
	 * @throws ExecutionException
	 */
	protected Repository getRepository(ExecutionEvent event)
			throws ExecutionException {
		GitHistoryPage page = getPage(event);
		RepositoryMapping mapping = RepositoryMapping
				.getMapping((IResource) page.getInput());

		if (mapping == null)
			throw new ExecutionException(
					UIText.AbstractHitoryViewCommandHandler_NoRepositoryMessage);
		return mapping.getRepository();
	}
}
