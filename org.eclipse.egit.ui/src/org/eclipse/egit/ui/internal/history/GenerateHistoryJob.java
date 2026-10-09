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
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.CancelledException;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;

class GenerateHistoryJob extends Job {
	private static final int BATCH_SIZE = 256;

	private final Object lock = new Object();

	private final GitHistoryPage page;

	private final SWTCommitList loadedCommits;

	private int itemToLoad = 1;

	private RevCommit commitToLoad;

	private RevCommit commitToShow;

	private int wantedIndex = -1;

	private int lastUpdateCnt = -1;

	private boolean trace;

	private final RevWalk walk;

	private RevFlag highlightFlag;

	private int forcedRedrawsAfterListIsCompleted = 0;

	// Guarded by 'lock'
	private boolean hasMore = true;

	// Guarded by 'lock'
	private int size = -1;

	// Guarded by 'lock'
	private int nextLoadHint = -1;

	// Guarded by 'lock'
	private int maxCommitsLimit;

	GenerateHistoryJob(final GitHistoryPage ghp, @NonNull RevWalk walk,
			ResourceManager resources) {
		super(NLS.bind(UIText.HistoryPage_refreshJob,
				RepositoryUtil.INSTANCE.getRepositoryName(
						ghp.getInputInternal().getRepository())));
		page = ghp;
		this.walk = walk;
		highlightFlag = walk.newFlag("highlight"); //$NON-NLS-1$
		loadedCommits = new SWTCommitList(resources) {

			@Override
			protected void enter(int index, PlotCommit<SWTLane> currCommit) {
				super.enter(index, currCommit);
				if (wantedIndex < 0 && commitToLoad != null
						&& currCommit.getId().equals(commitToLoad.getId())) {
					wantedIndex = index;
					commitToShow = commitToLoad;
					commitToLoad = null;
				}
			}
		};
		loadedCommits.source(walk);
		trace = GitTraceLocation.HISTORYVIEW.isActive();
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		int maxCommits = getMaxCommits();
		synchronized (lock) {
			nextLoadHint = -1;
			maxCommitsLimit = maxCommits;
		}
		IStatus status = Status.OK_STATUS;
		boolean incomplete = false;
		walk.setProgressMonitor(new EclipseGitProgressTransformer(monitor));
		try {
			if (trace)
				GitTraceLocation.getTrace().traceEntry(
						GitTraceLocation.HISTORYVIEW.getLocation());

			int initialSize = loadedCommits.size();
			try {
				do {
					if (trace)
						GitTraceLocation.getTrace().trace(
								GitTraceLocation.HISTORYVIEW.getLocation(),
								"Filling commit list"); //$NON-NLS-1$
					if (commitToLoad != null) {
						loadedCommits.fillTo(commitToLoad,
								getNextMaximumCommitsCount());
					} else {
						int next = getNextMaximumCommitsCount();
						loadedCommits.fillTo(
								maxCommits > 0 ? Math.min(next, maxCommits - 1)
										: next);
						if (!loadedCommits.isPending()) {
							forcedRedrawsAfterListIsCompleted++;
							break;
						}
					}
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					if (loadedCommits.size() > itemToLoad + (BATCH_SIZE / 2) + 1
							&& isFindToolbarHidden() && commitFound()) {
						break;
					}
					if (maxCommits > 0 && loadedCommits.size() >= maxCommits) {
						incomplete = true;
						if (commitToLoad == null) {
							break;
						}
					}
					if (!loadedCommits.isPending()) {
						break;
					}
					monitor.setTaskName(MessageFormat.format(
							UIText.GenerateHistoryJob_taskFoundCommits,
							Integer.valueOf(loadedCommits.size())));

					updateUI(incomplete);
				} while (!isFindToolbarHidden()
						|| (commitToLoad != null && !commitFound()));
			} catch (CancelledException e) {
				return Status.CANCEL_STATUS;
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						UIText.GenerateHistoryJob_errorComputingHistory, e);
			}
			synchronized (lock) {
				hasMore = loadedCommits.isPending();
				size = loadedCommits.size();
				incomplete = hasMore && maxCommits > 0 && size >= maxCommits;
			}
			if (trace)
				GitTraceLocation.getTrace().trace(
						GitTraceLocation.HISTORYVIEW.getLocation(),
						"Loaded " + loadedCommits.size() + " commits"); //$NON-NLS-1$ //$NON-NLS-2$
			if (!commitFound() && !loadedCommits.isEmpty()) {
				if (initialSize != loadedCommits.size()) {
					updateUI(incomplete);
				}
			}
			else {
				updateUI(incomplete);
			}

			if (incomplete) {
				page.setWarningTextInUIThread(this);
			} else {
				page.clearWarningTextInUIThread(this);
			}

		} finally {
			monitor.done();
			if (trace)
				GitTraceLocation.getTrace().traceExit(
						GitTraceLocation.HISTORYVIEW.getLocation());
		}
		return status;
	}

	private static boolean isFindToolbarHidden() {
		// While the find toolbar is displayed, all commits up to the limit are
		// loaded so that the user can search through them
		return !Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR);
	}

	private static int getMaxCommits() {
		return Activator.getDefault().getPreferenceStore()
				.getInt(UIPreferences.HISTORY_MAX_NUM_COMMITS);
	}

	private int getNextMaximumCommitsCount() {
		return loadedCommits.size() + BATCH_SIZE - 1;
	}

	private boolean commitFound() {
		return wantedIndex >= 0;
	}

	private void updateUI(boolean incomplete) {
		if (trace)
			GitTraceLocation.getTrace().traceEntry(
					GitTraceLocation.HISTORYVIEW.getLocation());
		try {
			if (forcedRedrawsAfterListIsCompleted != 1 && !incomplete
					&& loadedCommits.size() == lastUpdateCnt) {
				return;
			}
			if (forcedRedrawsAfterListIsCompleted == 1)
				forcedRedrawsAfterListIsCompleted++;
			final SWTCommit[] asArray = new SWTCommit[loadedCommits.size()];
			loadedCommits.toArray(asArray);
			page.showCommitList(this, loadedCommits, asArray, commitToShow, highlightFlag);
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
		loadedCommits.clear();
		walk.close();
	}

	@Override
	public boolean belongsTo(Object family) {
		if (JobFamilies.GENERATE_HISTORY.equals(family))
			return true;
		return super.belongsTo(family);
	}

	void setLoadHint(final int index) {
		itemToLoad = index;
		if (index < 0) {
			commitToLoad = null;
		}
	}

	void setLoadHint(final RevCommit c) {
		commitToLoad = c;
		if (c == null) {
			wantedIndex = -1;
		}
	}

	void setShowHint(final RevCommit c) {
		commitToShow = c;
	}

	boolean loadNextBatch(int currentIndex) {
		synchronized (lock) {
			if (hasMore && (maxCommitsLimit <= 0 || size < maxCommitsLimit)
					&& currentIndex + (BATCH_SIZE / 2) > size
					&& currentIndex > nextLoadHint) {
				nextLoadHint = currentIndex;
				return true;
			}
		}
		return false;
	}
}
