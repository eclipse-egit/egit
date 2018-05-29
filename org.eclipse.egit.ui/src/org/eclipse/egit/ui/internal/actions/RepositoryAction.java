/*******************************************************************************
 * Copyright (C) 2006, 2013 Shawn O. Pearce <spearce@spearce.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
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

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		serviceLocator = targetPart.getSite();
	}

	@Override
	public void run(IAction action) {
		if (!shouldRunAction())
			return;

        ExecutionEvent event = createExecutionEvent();

		try {
			this.handler.execute(event);
		} catch (ExecutionException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
	}

	/**
	 * Creates {@link ExecutionEvent} based on current selection
	 *
	 * @return {@link ExecutionEvent} with current selection
	 */
	protected ExecutionEvent createExecutionEvent() {
		IServiceLocator locator = getServiceLocator();
		ICommandService srv = CommonUtils.getService(locator, ICommandService.class);
		IHandlerService hsrv = CommonUtils.getService(locator, IHandlerService.class);
		Command command = srv.getCommand(commandId);

		ExecutionEvent event = hsrv.createExecutionEvent(command, null);
		if (event.getApplicationContext() instanceof IEvaluationContext)
			((IEvaluationContext) event.getApplicationContext()).addVariable(
					ISources.ACTIVE_CURRENT_SELECTION_NAME, mySelection);
		return event;
	}

	/**
	 * @return the service locator to use in the action
	 */
	protected IServiceLocator getServiceLocator() {
		if (serviceLocator == null)
			serviceLocator = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return serviceLocator;
	}

	@Override
	public final void selectionChanged(IAction action, ISelection selection) {
		mySelection = selection;
		if (handler.alwaysCheckEnabled()) {
			handler.setSelection(mySelection);
			if (action != null) {
				action.setEnabled(isEnabled());
			}
		} else {
			// Compare selection of handler, as it converts it to a suitable
			// selection. E.g. an ITextSelection is converted to a selection of
			// the file. We are only interested in the selection change if a
			// different file was selected, not if the offset of the text
			// selection changed.
			IStructuredSelection selectionBefore = handler.getSelection();
			handler.setSelection(mySelection);
			if (action != null) {
				IStructuredSelection selectionAfter = handler.getSelection();
				boolean equalSelection = (selectionBefore == null)
						? selectionAfter == null
						: selectionBefore.equals(selectionAfter);
				if (!equalSelection) {
					action.setEnabled(isEnabled());
				}
			}
		}
	}

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		if (!shouldRunAction())
			return null;

		ICommandService srv = CommonUtils.getService(getServiceLocator(), ICommandService.class);
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

	@Override
	public void init(IWorkbenchWindow window) {
		this.serviceLocator = window;
	}

	/**
	 * By default always return true. Allow implementers to decide whether
	 * the action should be run or not
	 *
	 * @return {@code true} when action should be executed, {@code false}
	 *         otherwise
	 */
	protected boolean shouldRunAction() {
		return true;
	}
}
