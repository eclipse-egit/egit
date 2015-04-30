/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import static org.eclipse.egit.gitflow.op.GitFlowOperation.SEP;
import static org.eclipse.egit.gitflow.ui.Activator.error;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.op.FeatureListOperation;
import org.eclipse.egit.gitflow.op.FeatureTrackOperation;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIText;
import org.eclipse.egit.gitflow.ui.internal.dialog.AbstractSelectionDialog;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;

/**
 * git flow feature track
 */
@SuppressWarnings("restriction")
public class FeatureTrackHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);
		final Repository repository = SelectionUtils.getRepository(selection);
		final GitFlowRepository gfRepo = new GitFlowRepository(repository);

		final List<Ref> refs = new ArrayList<Ref>();

		UIJob uiJob = new UIJob(HandlerUtil.getActiveShell(event).getDisplay(),
				UIText.FeatureTrackHandler_fetchingRemoteFeatures) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					int timeout = Activator.getDefault().getPreferenceStore()
							.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
					FeatureListOperation featureListOperation = new FeatureListOperation(
							gfRepo, timeout);
					featureListOperation.execute(monitor);
					refs.addAll(featureListOperation.getResult());
				} catch (CoreException e) {
					return error(e.getMessage(), e);
				}

				AbstractSelectionDialog<Ref> dialog = new AbstractSelectionDialog<Ref>(
						getDisplay().getActiveShell(), refs,
						UIText.FeatureTrackHandler_selectFeature,
						UIText.FeatureTrackHandler_remoteFeatures) {
					@Override
					protected String getPrefix() {
						return R_REMOTES + DEFAULT_REMOTE_NAME + SEP
								+ gfRepo.getFeaturePrefix();
					}
				};
				if (dialog.open() != Window.OK) {
					return Status.CANCEL_STATUS;
				}

				setName(UIText.FeatureTrackHandler_trackingFeature);
				Ref ref = dialog.getSelectedNode();
				try {
					new FeatureTrackOperation(gfRepo, ref).execute(monitor);
				} catch (CoreException e) {
					return error(e.getMessage(), e);
				}

				return Status.OK_STATUS;
			}
		};

		uiJob.setUser(true);
		uiJob.schedule();

		return null;
	}
}
