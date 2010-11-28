/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * A helper class for Team Actions on Git controlled projects.
 * <p>
 * This implements {@link IObjectActionDelegate} so that it can be used on the
 * Team menu and {@link IWorkbenchWindowActionDelegate} so that it works in the
 * command group.
 */
public abstract class RepositoryAction extends AbstractHandler implements
		IObjectActionDelegate, IWorkbenchWindowActionDelegate {
	private ISelection mySelection;

	/**
	 * The command id
	 */
	protected final String commandId;

	/**
	 * The part as set in {@link #setActivePart(IAction, IWorkbenchPart)}
	 */
	protected IServiceLocator serviceLocator;

	private final RepositoryActionHandler handler;

	/**
	 * @param commandId
	 * @param handler
	 */
	protected RepositoryAction(String commandId, RepositoryActionHandler handler) {
		this.commandId = commandId;
		this.handler = handler;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		serviceLocator = targetPart.getSite();
	}

	public void run(IAction action) {

		ICommandService srv = (ICommandService) serviceLocator
				.getService(ICommandService.class);
		IHandlerService hsrv = (IHandlerService) serviceLocator
				.getService(IHandlerService.class);
		Command command = srv.getCommand(commandId);

		ExecutionEvent event = hsrv.createExecutionEvent(command, null);
		if (event.getApplicationContext() instanceof IEvaluationContext) {
			((IEvaluationContext) event.getApplicationContext()).addVariable(
					ISources.ACTIVE_CURRENT_SELECTION_NAME, mySelection);
		}

		try {
			this.handler.execute(event);
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	public final void selectionChanged(IAction action, ISelection selection) {
		mySelection = selection;
		handler.setSelection(mySelection);
		action.setEnabled(isEnabled());
	}

	public final Object execute(ExecutionEvent event) throws ExecutionException {
		ICommandService srv = (ICommandService) serviceLocator
				.getService(ICommandService.class);
		Command command = srv.getCommand(commandId);
		try {
			return command.executeWithChecks(event);
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (NotDefinedException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (NotEnabledException e) {
			Activator.handleError(e.getMessage(), e, true);
		} catch (NotHandledException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		return null;
	}

	@Override
	public final boolean isEnabled() {
		return handler.isEnabled();
	}

	public void init(IWorkbenchWindow window) {
		this.serviceLocator = window;
	}
}
