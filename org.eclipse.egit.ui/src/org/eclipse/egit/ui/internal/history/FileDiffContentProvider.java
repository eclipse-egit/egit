/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
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
import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.UIJob;

/**
 * Content provider for {@link FileDiff} objects. The diffs are computed
 * asynchronously in a background job.
 */
public class FileDiffContentProvider implements IStructuredContentProvider {

	static final int INTERESTING_MARK_TREE_FILTER_INDEX = 0;

	private FileDiff[] diff;

	private TreeFilter markTreeFilter = TreeFilter.ALL;

	private CommitFileDiffViewer viewer;

	private boolean needsRecompute;

	private FileDiffLoader loader;

	private FileDiffInput currentInput;

	@Override
	public void inputChanged(final Viewer newViewer, final Object oldInput,
			final Object newInput) {
		cancel();
		viewer = (CommitFileDiffViewer) newViewer;
		if (newInput != null) {
			currentInput = (FileDiffInput) newInput;
			setInterestingPaths(currentInput.getInterestingPaths());
		} else {
			currentInput = null;
		}
		diff = null;
		needsRecompute = true;
	}

	/**
	 * Set the paths which are interesting and should be highlighted in the view.
	 * @param interestingPaths
	 */
	void setInterestingPaths(Collection<String> interestingPaths) {
		if (interestingPaths != null) {
			this.markTreeFilter = PathFilterGroup.createFromStrings(interestingPaths);
		} else {
			this.markTreeFilter = TreeFilter.ALL;
		}
		needsRecompute = true;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		if (!needsRecompute) {
			return diff != null ? diff : new Object[0];
		}
		needsRecompute = false;
		FileDiffInput input = currentInput;
		if (input == null) {
			diff = null;
			return new Object[0];
		}
		cancel();
		FileDiffLoader job = new FileDiffLoader(input, markTreeFilter);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (!event.getResult().isOK()) {
					return;
				}
				UIJob updater = new UpdateJob(MessageFormat.format(
						UIText.FileDiffContentProvider_updatingFileDiffs,
						input.getCommit().getName()), job);
				updater.schedule();
			}
		});
		job.setUser(false);
		job.setSystem(true);
		loader = job;
		loader.schedule();
		return new Object[0];
	}

	private void cancel() {
		if (loader != null) {
			loader.cancel();
			loader = null;
		}
	}

	@Override
	public void dispose() {
		cancel();
		viewer = null;
		diff = null;
		currentInput = null;
	}

	private static class FileDiffLoader extends Job {

		private FileDiff[] diffs;

		private final FileDiffInput input;

		private final TreeFilter filter;

		public FileDiffLoader(FileDiffInput input, TreeFilter filter) {
			super(MessageFormat.format(
					UIText.FileDiffContentProvider_computingFileDiffs,
					input.getCommit().getName()));
			this.input = input;
			this.filter = filter;
			setRule(new TreeWalkSchedulingRule(input.getTreeWalk()));
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				diffs = FileDiff.compute(input.getRepository(),
						input.getTreeWalk(),
						input.getCommit(), monitor, filter);
			} catch (IOException err) {
				Activator.handleError(MessageFormat.format(
						UIText.FileDiffContentProvider_errorGettingDifference,
						input.getCommit().getId()), err, false);
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		}

		public FileDiff[] getDiffs() {
			return diffs;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == JobFamilies.HISTORY_FILE_DIFF
					|| super.belongsTo(family);
		}
	}

	private class UpdateJob extends UIJob {

		FileDiffLoader loadJob;

		public UpdateJob(String name, FileDiffLoader loadJob) {
			super(name);
			this.loadJob = loadJob;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Control control = viewer == null ? null : viewer.getControl();
			if (control == null || control.isDisposed() || loader != loadJob) {
				return Status.CANCEL_STATUS;
			}
			diff = loadJob.getDiffs();
			try {
				control.setRedraw(false);
				viewer.refresh();
				FileDiff interesting = getFirstInterestingElement();
				if (interesting != null) {
					if (currentInput.isSelectMarked()) {
						viewer.setSelection(
								new StructuredSelection(interesting), true);
					} else {
						viewer.reveal(interesting);
					}
				}
			} finally {
				control.setRedraw(true);
			}
			return Status.OK_STATUS;
		}

		private FileDiff getFirstInterestingElement() {
			FileDiff[] diffs = diff;
			if (diffs != null) {
				for (FileDiff d : diffs) {
					if (d.isMarked(INTERESTING_MARK_TREE_FILTER_INDEX)) {
						return d;
					}
				}
			}
			return null;
		}

	}

	/**
	 * Serializes all load jobs using the same tree walk. Tree walks are not
	 * thread safe.
	 */
	private static class TreeWalkSchedulingRule implements ISchedulingRule {

		private final TreeWalk treeWalk;

		public TreeWalkSchedulingRule(TreeWalk treeWalk) {
			this.treeWalk = treeWalk;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			if (rule instanceof TreeWalkSchedulingRule) {
				return Objects.equals(treeWalk,
						((TreeWalkSchedulingRule) rule).treeWalk);
			}
			return false;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

	}

}
