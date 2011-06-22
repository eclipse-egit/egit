/*******************************************************************************
 * Copyright (c) 2011 Tasktop Technologies.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * a utility for accessing common functions of the EGit UI
 */
public class UIProjectUtils {
	private static final String IMPORT_COMMAND_ID =  "org.eclipse.egit.ui.RepositoriesViewImportProjects"; //$NON-NLS-1$

	/**
	 * Open the import projects dialog with the given Git repositories as options.
	 * Must be called from the UI thread.
	 *
	 * @param activeShell the active shell.
	 * @param repository the repository from which projects should be imported into the workspace
	 *
	 * @throws ExecutionException if an exception occurred while executing the command
	 * @throws CoreException
	 */
	public static void openImportProjectsDialog(Shell activeShell,Repository repository) throws ExecutionException, CoreException {
		if (repository == null)
			throw new IllegalArgumentException();
		if (activeShell == null)
			throw new IllegalArgumentException();

		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(
				ICommandService.class);
		if (commandService == null)
			throw new IllegalStateException();

		Command command = commandService.getCommand(IMPORT_COMMAND_ID);
		if (command == null)
			throw new IllegalStateException();

		IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(
				IHandlerService.class);
		if (handlerService == null)
			throw new IllegalStateException();

		ExecutionEvent executionEvent = handlerService.createExecutionEvent(command, new Event());
		@SuppressWarnings("unchecked")
		Map<String, Object> parameters = new HashMap<String,Object>(executionEvent.getParameters());
		StructuredSelection structuredSelection = new StructuredSelection(new RepositoryNode(null,repository));
		parameters.put(ISources.ACTIVE_CURRENT_SELECTION_NAME, structuredSelection);
		parameters.put(ISources.ACTIVE_SHELL_NAME, activeShell);
		EvaluationContext evaluationContext = new EvaluationContext(
				(IEvaluationContext) (executionEvent.getApplicationContext() instanceof IEvaluationContext ? executionEvent.getApplicationContext()
						: null), structuredSelection);

		for (Map.Entry<String, Object> parameter : parameters.entrySet())
			evaluationContext.addVariable(parameter.getKey(), parameter.getValue());

		executionEvent = new ExecutionEvent(command, parameters, executionEvent.getTrigger(),
				evaluationContext);

		try {
			command.executeWithChecks(executionEvent);
		} catch (NotDefinedException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(),
					UIText.EGitUI_ImportProjectsNotDefined, e));
		} catch (NotEnabledException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(),
					UIText.EGitUI_ImportProjectsNotEnabled, e));
		} catch (NotHandledException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(),
					UIText.EGitUI_ImportProjectsNotHandled, e));
		}
	}
}
