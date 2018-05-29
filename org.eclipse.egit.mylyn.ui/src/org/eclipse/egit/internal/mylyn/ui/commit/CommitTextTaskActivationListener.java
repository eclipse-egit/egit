/*******************************************************************************
 * Copyright (c) 2016 Brandon Dong <brandon.dong@tasktop.com>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskActivationAdapter;
import org.eclipse.ui.PlatformUI;

/**
 * A listener that updates commit text in staging view when a task is activated
 */
public class CommitTextTaskActivationListener extends TaskActivationAdapter {

	@Override
	public void taskActivated(ITask task) {
		StagingView view = (StagingView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(StagingView.VIEW_ID);
		if (view != null) {
			view.resetCommitMessageComponent();
		}
	}

}
