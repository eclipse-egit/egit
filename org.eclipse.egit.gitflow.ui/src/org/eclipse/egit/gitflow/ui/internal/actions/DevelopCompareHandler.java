/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import static org.eclipse.egit.gitflow.ui.Activator.error;
import static org.eclipse.egit.ui.Activator.handleError;
import static org.eclipse.egit.ui.internal.UIText.CompareWithRefAction_errorOnSynchronize;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.ui.PlatformUI.getWorkbench;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Compare content of selected resources with that on develop branch.
 */
public class DevelopCompareHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);
		if (gfRepo == null) {
			return error(UIText.Handlers_noGitflowRepositoryFound);
		}

		IResource[] selectedResources = GitFlowHandlerUtil.gatherResourceToOperateOn(event);
		String revision;
		try {
			revision = GitFlowHandlerUtil.gatherRevision(event);
		} catch (OperationCanceledException e) {
			return null;
		} catch (IOException e) {
			throw new ExecutionException(e.getMessage(), e);
		}

		IWorkbenchPage workBenchPage = getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			CompareUtils.compare(selectedResources, gfRepo.getRepository(),
					HEAD, revision, true, workBenchPage);
		} catch (IOException e) {
			handleError(CompareWithRefAction_errorOnSynchronize, e, true);
		}
		return null;
	}
}
