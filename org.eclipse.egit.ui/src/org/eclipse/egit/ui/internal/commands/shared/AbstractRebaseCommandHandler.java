/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands.shared;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.ui.internal.rebase.RebaseHelper;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISources;

/**
 * Rebase command base class
 */
public abstract class AbstractRebaseCommandHandler extends AbstractSharedCommandHandler {
	private final Operation operation;

	private final String jobname;

	private final String dialogMessage;

	private final boolean interactive;

	private static final List<RebaseCommandFinishedListener> listeners = new ArrayList<RebaseCommandFinishedListener>();

	/**
	 * @param operation
	 * @param jobname
	 * @param dialogMessage
	 * @param interactive
	 */
	protected AbstractRebaseCommandHandler(Operation operation, String jobname,
			String dialogMessage, boolean interactive) {
		this.operation = operation;
		this.jobname = jobname;
		this.dialogMessage = dialogMessage;
		this.interactive = interactive;
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		final Repository repository = getRepository(event);
		final Ref ref = getRef(event);
		return execute(repository, ref);
	}

	/**
	 * @param repository
	 * @param ref
	 * @return {@code null}
	 * @throws ExecutionException
	 */
	public Object execute(final Repository repository, Ref ref)
			throws ExecutionException {
		if (repository == null)
			return null;
		RebaseHelper.runRebaseJob(repository, jobname, ref, operation,
				interactive, dialogMessage, listeners);
		return null;
	}

	/**
	 * Retrieve the current selection. The global selection is used if the menu
	 * selection is not available.
	 *
	 * @param ctx
	 * @return the selection
	 */
	protected Object getSelection(IEvaluationContext ctx) {
		Object selection = ctx.getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		if (selection == null || !(selection instanceof ISelection))
			selection = ctx.getVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME);
		return selection;
	}

	/**
	 * Extracts the editor input from the given context.
	 *
	 * @param ctx the context
	 * @return the editor input for the given context or <code>null</code> if not available
	 * @since 2.1
	 */
	protected IEditorInput getActiveEditorInput(IEvaluationContext ctx) {
		Object editorInput = ctx.getVariable(ISources.ACTIVE_EDITOR_INPUT_NAME);
		if (editorInput instanceof IEditorInput)
			return (IEditorInput) editorInput;

		return null;
	}

	/**
	 * @param listener
	 */
	public static void addRebaseCommandFinishListener(
			RebaseCommandFinishedListener listener) {
		if (listeners.contains(listener))
			return;
		listeners.add(listener);
	}
	/**
	 * @param listener
	 */
	public static void removeRebaseCommandFinishListener(
			RebaseCommandFinishedListener listener) {
		listeners.remove(listener);
	}

	/**
	 *
	 */
	public interface RebaseCommandFinishedListener {
		/**
		 * Called when a
		 *
		 * @param operation
		 * @param ref
		 * @param repository
		 * @param result
		 */
		void operationFinished(IStatus result, Repository repository, Ref ref,
				Operation operation);
	}

	/**
	 * @param result
	 * @param repository
	 * @param ref
	 * @param operation
	 */
	public static void fireRebaseCommandFinished(IStatus result,
			Repository repository, Ref ref, Operation operation) {
		for (RebaseCommandFinishedListener listener : listeners) {
			listener.operationFinished(result, repository, ref, operation);
		}
	}

}
