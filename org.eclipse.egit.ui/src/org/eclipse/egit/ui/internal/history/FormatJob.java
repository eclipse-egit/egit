/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2011, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
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
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revplot.PlotCommit;

class FormatJob extends Job {

	@Override
	public boolean belongsTo(Object family) {
		if (JobFamilies.FORMAT_COMMIT_INFO.equals(family))
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
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		FormatResult commitInfo;
		CommitInfoBuilder builder;
		try {
			synchronized(lock) {
				SWTCommit commit = (SWTCommit)formatRequest.getCommit();
				commit.parseBody();
				builder = new CommitInfoBuilder(formatRequest.getRepository(),
						commit, formatRequest.isFill(),
						formatRequest.getAllRefs());
			}
			commitInfo = builder.format(monitor);
		} catch (IOException e) {
			return Activator.createErrorStatus(e.getMessage(), e);
		}
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		synchronized(lock) {
			formatResult = commitInfo;
		}
		return Status.OK_STATUS;
	}

	static class FormatRequest {

		public Collection<Ref> getAllRefs() {
			return allRefs;
		}

		public void setAllRefs(Collection<Ref> allRefs) {
			this.allRefs = allRefs;
		}

		private Repository repository;

		private PlotCommit<?> commit;

		private boolean fill;

		private Collection<Ref> allRefs;

		FormatRequest(Repository repository, PlotCommit<?> commit, boolean fill,
				Collection<Ref> allRefs) {
			this.repository = repository;
			this.commit = commit;
			this.fill = fill;
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

	}

	static class FormatResult{
		private final String commitInfo;

		private final List<GitCommitReference> knownLinks;

		private final int headerEnd;

		private final int footerStart;

		FormatResult(String commmitInfo, List<GitCommitReference> links,
				int headerEnd, int footerStart) {
			this.commitInfo = commmitInfo;
			this.knownLinks = links;
			this.headerEnd = headerEnd;
			this.footerStart = footerStart;
		}

		public String getCommitInfo() {
			return commitInfo;
		}

		public List<GitCommitReference> getKnownLinks() {
			return knownLinks;
		}

		public int getHeaderEnd() {
			return headerEnd;
		}

		public int getFooterStart() {
			return footerStart;
		}
	}

}
