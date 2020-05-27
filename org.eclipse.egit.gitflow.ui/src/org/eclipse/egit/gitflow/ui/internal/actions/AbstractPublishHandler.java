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

import static org.eclipse.egit.gitflow.ui.Activator.error;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.CurrentBranchPublishOperation;
import org.eclipse.egit.ui.JobFamilies;

/**
 * git flow {feature,release,hotfix} finish
 */
public abstract class AbstractPublishHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);

		try {
			CurrentBranchPublishOperation publishOperation = new CurrentBranchPublishOperation(
					gfRepo, GitSettings.getRemoteConnectionTimeout());
			JobUtil.scheduleUserWorkspaceJob(publishOperation,
					getProgressText(), JobFamilies.REBASE,
					new PostPublishUiTask(gfRepo, publishOperation));
		} catch (CoreException e) {
			return error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * @return Text to be shown when the operation is in progress
	 */
	abstract protected String getProgressText();
}
