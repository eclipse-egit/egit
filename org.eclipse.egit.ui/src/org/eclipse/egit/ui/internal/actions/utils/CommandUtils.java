/*******************************************************************************
 * Copyright (C) 2011, Andre Dietisheim <adietish@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions.utils;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * @author André Dietisheim
 */
public class CommandUtils {

	/**
	 * Executes a command for a given id and selection. Returns
	 * <code>true</code> if the command was executed successfully,
	 * <code>false</code> otherwise.
	 *
	 * @param commandId
	 *            the id of the command that shall be executed
	 * @param selection
	 *            the selection to use when executing the command
	 * @return true if the execution was successful, false otherwise.
	 * @throws ExecutionException
	 * @throws NotDefinedException
	 * @throws NotEnabledException
	 * @throws NotHandledException
	 */
	public static boolean executeCommand(String commandId,
			IStructuredSelection selection) throws ExecutionException,
			NotDefinedException, NotEnabledException, NotHandledException {
		Command command = getCommand(commandId);
		if (!command.isDefined()) {
			return false;
		}

		IHandlerService handlerService = (IHandlerService) PlatformUI
				.getWorkbench().getService(IHandlerService.class);
		EvaluationContext context = createEvaluationContext(selection,
				handlerService);
		return doExecuteCommand(commandId, command, handlerService, context);
	}

	private static EvaluationContext createEvaluationContext(
			IStructuredSelection selection, IHandlerService handlerService) {
		EvaluationContext context = null;
		if (selection != null) {
			context = new EvaluationContext(
					handlerService.createContextSnapshot(false),
					selection.toList());
			context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME,
					selection);
			context.removeVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		}
		return context;
	}

	private static boolean doExecuteCommand(String commandId, Command command,
			IHandlerService handlerService, EvaluationContext context)
			throws ExecutionException, NotDefinedException,
			NotEnabledException, NotHandledException {
		if (context != null) {
			handlerService.executeCommandInContext(new ParameterizedCommand(
					command, null), null, context);
		} else {
			handlerService.executeCommand(commandId, null);
		}
		return true;
	}

	/**
	 * Returns a command for a given id. If no command with the given id exists
	 * then an undefined command with the given id is created.
	 *
	 * @param commandId
	 * @return the command with the given id
	 */
	public static Command getCommand(String commandId) {
		ICommandService commandService = (ICommandService) PlatformUI
				.getWorkbench().getService(ICommandService.class);
		return commandService.getCommand(commandId);
	}
}
