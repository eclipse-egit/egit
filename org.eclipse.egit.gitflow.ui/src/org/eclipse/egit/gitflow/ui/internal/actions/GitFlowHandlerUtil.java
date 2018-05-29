/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.Nullable;
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
		if (gfRepo == null) {
			throw new IllegalStateException(
					"Gitflow command called with no Gitflow repository present"); //$NON-NLS-1$
		}

		Ref develop = gfRepo.getRepository()
				.exactRef(gfRepo.getConfig().getDevelopFull());
		if (develop == null) {
			throw new IllegalStateException(
					"Gitflow command called on Gitflow repository with no develop branch. " //$NON-NLS-1$
							+ "The Gitflow configuration is either corrupt or incomplete."); //$NON-NLS-1$
		}
		return develop.getName();
	}
}
