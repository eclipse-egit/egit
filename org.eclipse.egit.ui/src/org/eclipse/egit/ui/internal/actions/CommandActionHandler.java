/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * An wrapper class for launching {@link Command}s based on its name
 */
public class CommandActionHandler extends RepositoryActionHandler {

	private String commandName;

	/**
	 *
	 * @param commandName name of command that should be launched
	 */
	public CommandActionHandler(String commandName) {
		this.commandName = commandName;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow activeWorkbenchWindow = workbench
				.getActiveWorkbenchWindow();

		ICommandService commandService = (ICommandService) activeWorkbenchWindow
				.getService(ICommandService.class);
		Command continueCommand = commandService.getCommand(commandName);

		try {
			continueCommand.executeWithChecks(event);
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}

		return null;
	}

}
