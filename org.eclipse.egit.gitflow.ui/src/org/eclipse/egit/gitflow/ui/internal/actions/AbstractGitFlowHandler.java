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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.osgi.util.NLS;

/**
 * Contains shared code for handling git flow operations
 */
public abstract class AbstractGitFlowHandler extends AbstractHandler {
	/**
	 * @param develop
	 * @param branch
	 * @param mergeResult
	 * @return Status describing which branches were involved in a conflicting
	 *         merge
	 */
	protected MultiStatus createMergeConflictInfo(String develop,
			String branch, MergeResult mergeResult) {
		String pluginId = Activator.getPluginId();
		MultiStatus info = new MultiStatus(pluginId, 1,
				UIText.AbstractGitFlowHandler_finishConflicts, null);
		info.add(new Status(IStatus.WARNING, pluginId, 1, NLS.bind(
				UIText.FinishHandler_conflictsWhileMergingFromTo,
				branch, develop), null));

		MultiStatus warning = createMergeConflictWarning(mergeResult);
		info.addAll(warning);

		return info;
	}

	/**
	 * @param mergeResult
	 * @return Status containing detailed information about what went wrong
	 *         during merge
	 */
	private MultiStatus createMergeConflictWarning(MergeResult mergeResult) {
		Iterable<String> paths = mergeResult.getConflicts().keySet();
		return docreateConflictWarning(paths,
				UIText.AbstractGitFlowHandler_finishConflicts);
	}

	/**
	 * @param rebaseResult
	 * @return Status containing detailed information about what went wrong
	 *         during rebase
	 */
	protected MultiStatus createRebaseConflictWarning(RebaseResult rebaseResult) {
		Iterable<String> paths = rebaseResult.getConflicts();
		return docreateConflictWarning(paths,
				UIText.AbstractGitFlowHandler_rebaseConflicts);
	}

	private MultiStatus docreateConflictWarning(Iterable<String> paths,
			String message) {
		String pluginId = Activator.getPluginId();
		MultiStatus multiStatus = new MultiStatus(pluginId, 1, message, null);
		for (String path : paths) {
			multiStatus.add(new Status(IStatus.WARNING, pluginId, 1, path, null));
		}

		return multiStatus;
	}
}
