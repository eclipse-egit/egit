/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

class FormatJob extends Job {

	@Override
	public boolean belongsTo(Object family) {
		if (family.equals(JobFamilies.FORMAT_COMMIT_INFO))
			return true;
		return super.belongsTo(family);
	}

	private Object lock = new Object(); // guards formatRequest and formatResult
	private FormatRequest formatRequest;
	private FormatResult formatResult;

	FormatJob(FormatRequest formatRequest) {
		super(UIText.FormatJob_buildingCommitInfo);
		this.formatRequest = formatRequest;
	}

	FormatResult getFormatResult() {
		synchronized(lock) {
			return formatResult;
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if(monitor.isCanceled())
			return Status.CANCEL_STATUS;
		final List<StyleRange> styles = new ArrayList<StyleRange>();
		final String commitInfo;
		CommitInfoBuilder builder;
		try {
			synchronized(lock) {
				SWTCommit commit = (SWTCommit)formatRequest.getCommit();
				commit.parseBody();
				builder = new CommitInfoBuilder(formatRequest.getRepository(), commit,
						formatRequest.getCurrentDiffs(), formatRequest.isFill(), formatRequest.getAllRefs());
				builder.setColors(formatRequest.getLinkColor(),
						formatRequest.getDarkGrey(),
						formatRequest.getHunkheaderColor(),
						formatRequest.getLinesAddedColor(),
						formatRequest.getLinesRemovedColor());
			}
			commitInfo = builder.format(styles, monitor);
		} catch (IOException e) {
			return Activator.createErrorStatus(e.getMessage(), e);
		}
		final StyleRange[] arr = new StyleRange[styles.size()];
		styles.toArray(arr);
		Arrays.sort(arr, new Comparator<StyleRange>() {
			public int compare(StyleRange o1, StyleRange o2) {
				return o1.start - o2.start;
			}
		});
		if(monitor.isCanceled())
			return Status.CANCEL_STATUS;
		synchronized(lock) {
			formatResult = new FormatResult(commitInfo, arr);
		}
		return Status.OK_STATUS;
	}

	static class FormatRequest {

		public Color getLinkColor() {
			return linkColor;
		}

		public void setLinkColor(Color linkColor) {
			this.linkColor = linkColor;
		}

		public Color getDarkGrey() {
			return darkGrey;
		}

		public void setDarkGrey(Color darkGrey) {
			this.darkGrey = darkGrey;
		}

		public Color getHunkheaderColor() {
			return hunkheaderColor;
		}

		public void setHunkheaderColor(Color hunkheaderColor) {
			this.hunkheaderColor = hunkheaderColor;
		}

		public Color getLinesAddedColor() {
			return linesAddedColor;
		}

		public void setLinesAddedColor(Color linesAddedColor) {
			this.linesAddedColor = linesAddedColor;
		}

		public Color getLinesRemovedColor() {
			return linesRemovedColor;
		}

		public void setLinesRemovedColor(Color linesRemovedColor) {
			this.linesRemovedColor = linesRemovedColor;
		}

		public Collection<Ref> getAllRefs() {
			return allRefs;
		}

		public void setAllRefs(Collection<Ref> allRefs) {
			this.allRefs = allRefs;
		}

		private Repository repository;

		private PlotCommit<?> commit;

		private boolean fill;

		List<FileDiff> currentDiffs;

		private Color linkColor;

		private Color darkGrey;

		private Color hunkheaderColor;

		private Color linesAddedColor;

		private Color linesRemovedColor;

		private Collection<Ref> allRefs;

		FormatRequest(Repository repository, PlotCommit<?> commit,
				boolean fill, List<FileDiff> currentDiffs, Color linkColor,
				Color darkGrey, Color hunkheaderColor, Color linesAddedColor,
				Color linesRemovedColor, Collection<Ref> allRefs) {
			this.repository = repository;
			this.commit = commit;
			this.fill = fill;
			this.currentDiffs = new ArrayList<FileDiff>(currentDiffs);
			this.linkColor = linkColor;
			this.darkGrey = darkGrey;
			this.hunkheaderColor = hunkheaderColor;
			this.linesAddedColor = linesAddedColor;
			this.linesRemovedColor = linesRemovedColor;
			this.allRefs = allRefs;
		}

		public Repository getRepository() {
			return repository;
		}

		public PlotCommit<?> getCommit() {
			return commit;
		}

		public boolean isFill() {
			return fill;
		}

		public List<FileDiff> getCurrentDiffs() {
			return currentDiffs;
		}

	}

	static class FormatResult{
		String commitInfo;
		StyleRange[] styleRange;

		FormatResult(String commmitInfo, StyleRange[] styleRange) {
			this.commitInfo = commmitInfo;
			this.styleRange = styleRange;
		}

		public String getCommitInfo() {
			return commitInfo;
		}

		public StyleRange[] getStyleRange() {
			return styleRange;
		}
	}

}
