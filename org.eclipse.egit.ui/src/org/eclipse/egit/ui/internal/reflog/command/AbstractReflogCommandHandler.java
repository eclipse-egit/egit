/*******************************************************************************
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
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

	protected IStructuredSelection getSelection(ReflogView view) {
		ISelection pageSelection = view.getSelectionProvider().getSelection();
		if (pageSelection instanceof IStructuredSelection)
			return (IStructuredSelection) pageSelection;
		else
			return new StructuredSelection();
	}

}