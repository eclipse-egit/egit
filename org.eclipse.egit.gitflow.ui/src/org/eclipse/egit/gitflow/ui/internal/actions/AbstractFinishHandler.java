/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.osgi.util.NLS;

/**
 * Contains shared code for handling finish operations
 */
public abstract class AbstractFinishHandler extends AbstractHandler {
	/**
	 * @param develop
	 * @param featureBranch
	 * @param mergeResult
	 * @return Status containing detailed information about what went wrong
	 *         during merge or rebase
	 */
	protected MultiStatus createConflictWarning(String develop,
			String featureBranch, MergeResult mergeResult) {
		String pluginId = Activator.getPluginId();
		MultiStatus info = new MultiStatus(pluginId, 1,
				UIText.FeatureFinishHandler_featureFinishConflicts, null);
		info.add(new Status(IStatus.WARNING, pluginId, 1, NLS.bind(
				UIText.FeatureFinishHandler_conflictsWhileMergingFromTo,
				featureBranch, develop), null));

		Set<String> paths = mergeResult.getConflicts().keySet();
		for (String path : paths) {
			info.add(new Status(IStatus.WARNING, pluginId, 1, path, null));
		}

		return info;
	}
}
