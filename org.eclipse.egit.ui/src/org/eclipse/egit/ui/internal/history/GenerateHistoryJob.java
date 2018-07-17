/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

class GenerateHistoryJob extends Job {
	private static final int BATCH_SIZE = 256;

	private final GitHistoryPage page;

	private final SWTCommitList loadedCommits;

	private int itemToLoad = 1;

	private RevCommit commitToLoad;

	private RevCommit commitToShow;

	private int lastUpdateCnt;

	private boolean trace;

	private final RevWalk walk;

	private RevFlag highlightFlag;

	private int forcedRedrawsAfterListIsCompleted = 0;

	GenerateHistoryJob(final GitHistoryPage ghp, Control control,
			@NonNull RevWalk walk,
			ResourceManager resources) {
		super(NLS.bind(UIText.HistoryPage_refreshJob, Activator.getDefault()
				.getRepositoryUtil().getRepositoryName(
						ghp.getInputInternal().getRepository())));
		page = ghp;
		this.walk = walk;
		highlightFlag = walk.newFlag("highlight"); //$NON-NLS-1$
		loadedCommits = new SWTCommitList(control, resources);
		loadedCommits.source(walk);
		trace = GitTraceLocation.HISTORYVIEW.isActive();
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		int maxCommits = Activator.getDefault().getPreferenceStore()
					.getInt(UIPreferences.HISTORY_MAX_NUM_COMMITS);
		boolean incomplete = false;
		boolean commitNotFound = false;
		try {
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation());
			final boolean loadIncrementally = !Activator.getDefault()
					.getPreferenceStore()
					.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR);
			try {
				for (;;) {
					int oldsz = loadedCommits.size();
					if (trace)
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.HISTORYVIEW.getLocation(),
								"Filling commit list"); //$NON-NLS-1$
					if (commitToLoad != null) {
						loadedCommits.fillTo(commitToLoad, maxCommits);
						commitToShow = commitToLoad;
						commitToLoad = null;
						boolean commitFound = false;
						for (RevCommit commit : loadedCommits) {
							if (commit.getId().equals(commitToShow.getId())) {
								commitFound = true;
								break;
							}
						}
						commitNotFound = !commitFound;
					} else {
						loadedCommits.fillTo(oldsz + BATCH_SIZE - 1);
						if (oldsz == loadedCommits.size()) {
							forcedRedrawsAfterListIsCompleted++;
							break;
						}
					}
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					if (loadedCommits.size() > itemToLoad + (BATCH_SIZE / 2) + 1 && loadIncrementally)
						break;
					if (maxCommits > 0 && loadedCommits.size() > maxCommits) {
						incomplete = true;
						break;
					}
					if (loadedCommits.size() != 1)
						monitor.setTaskName(MessageFormat
								.format(UIText.GenerateHistoryJob_taskFoundMultipleCommits,
										Integer.valueOf(loadedCommits.size())));
					else
						monitor.setTaskName(UIText.GenerateHistoryJob_taskFoundSingleCommit);
				}
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, Activator.getPluginId(),
						UIText.GenerateHistoryJob_errorComputingHistory, e);
			}
			if (trace)
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.HISTORYVIEW.getLocation(),
						"Loaded " + loadedCommits.size() + " commits"); //$NON-NLS-1$ //$NON-NLS-2$
			if (commitNotFound) {
				if (forcedRedrawsAfterListIsCompleted < 1 && !loadIncrementally)
					page.setWarningTextInUIThread(this);
			}
			else
				updateUI(incomplete);
		} finally {
			monitor.done();
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
		return status;
	}

	private void updateUI(boolean incomplete) {
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());
		try {
			if (!(forcedRedrawsAfterListIsCompleted == 1) && !incomplete
					&& loadedCommits.size() == lastUpdateCnt)
				return;
			if (forcedRedrawsAfterListIsCompleted == 1)
				forcedRedrawsAfterListIsCompleted++;
			final SWTCommit[] asArray = new SWTCommit[loadedCommits.size()];
			loadedCommits.toArray(asArray);
			page.showCommitList(this, loadedCommits, asArray, commitToShow, incomplete, highlightFlag);
			commitToShow = null;
			lastUpdateCnt = loadedCommits.size();
		} finally {
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
	}

	void release() {
		if (getState() == Job.NONE)
			dispose();
		else
			addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(final IJobChangeEvent event) {
					dispose();
				}
			});

	}

	private void dispose() {
		walk.close();
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				loadedCommits.dispose();
			}
		});
	}

	@Override
	public boolean belongsTo(Object family) {
		if (JobFamilies.GENERATE_HISTORY.equals(family))
			return true;
		return super.belongsTo(family);
	}

	void setLoadHint(final int index) {
		itemToLoad = index;
	}

	void setLoadHint(final RevCommit c) {
		commitToLoad = c;
	}

	int loadMoreItemsThreshold() {
		return loadedCommits.size() - (BATCH_SIZE / 2);
	}
}
