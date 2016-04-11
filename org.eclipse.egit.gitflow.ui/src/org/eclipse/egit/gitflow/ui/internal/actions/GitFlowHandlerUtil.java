/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 *
 */
public class GitFlowHandlerUtil {
	/**
	 * @param event
	 * @return Selected GitFlowRepository
	 */
	public static @Nullable GitFlowRepository getRepository(ExecutionEvent event) {
		ISelection selection = HandlerUtil
				.getCurrentSelection(event);
		IStructuredSelection structuredSelection = SelectionUtils
				.getStructuredSelection(selection);
		Repository repository = SelectionUtils
				.getRepository(structuredSelection);

		if (repository == null) {
			return null;
		}
		return new GitFlowRepository(repository);
	}

	static IResource[] gatherResourceToOperateOn(ExecutionEvent event) {
		return getSelectedResources(event);
	}

	static IResource[] getSelectedResources(ExecutionEvent event) {
		IStructuredSelection selection = getSelection(event);
		return SelectionUtils.getSelectedResources(selection);
	}

	static IStructuredSelection getSelection(ExecutionEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("event must not be NULL"); //$NON-NLS-1$
		}
		Object context = event.getApplicationContext();
		if (context instanceof IEvaluationContext) {
			return SelectionUtils.getSelection((IEvaluationContext) context);
		}
		return StructuredSelection.EMPTY;
	}

	static String gatherRevision(ExecutionEvent event) throws IOException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil
				.getRepository(event);
		Ref exactRef = gfRepo.getRepository()
				.exactRef(gfRepo.getConfig().getDevelopFull());
		if (exactRef == null) {
			throw new IllegalStateException(
					"Gitflow command called on non-Gitflow repository"); //$NON-NLS-1$
		}
		return exactRef.getName();
	}
}
