/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import static org.eclipse.egit.gitflow.ui.Activator.error;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.op.FeatureFinishOperation;
import org.eclipse.egit.gitflow.ui.internal.JobUtil;
import org.eclipse.egit.gitflow.ui.internal.UIText;

/**
 * git flow feature finish
 */
public class FeatureFinishHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final GitFlowRepository gfRepo = GitFlowHandlerUtil.getRepository(event);

		try {
			FeatureFinishOperation operation = new FeatureFinishOperation(gfRepo);
			JobUtil.scheduleUserWorkspaceJob(operation,
					UIText.FeatureFinishHandler_finishingFeature,
					JobUtil.GITFLOW_FAMILY);
		} catch (WrongGitFlowStateException | CoreException | IOException e) {
			return error(e.getMessage(), e);
		}

		return null;
	}
}
