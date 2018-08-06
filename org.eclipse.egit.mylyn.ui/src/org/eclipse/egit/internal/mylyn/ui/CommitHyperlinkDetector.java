/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

/**
 * Detects Git commit ids in task descriptions and allows users to open them in
 * the commit editor.
 */
public class CommitHyperlinkDetector extends AbstractHyperlinkDetector {

	private static final Pattern PATTERN_COMMIT_ID = Pattern
			.compile("(?<!\\w)([0-9a-f]{8}([0-9a-f]{32})?)"); //$NON-NLS-1$

	private static class CommitHyperlink implements IHyperlink {

		private IRegion region;

		private String objectId;

		private final Shell shell;

		public CommitHyperlink(IRegion region, String objectId, Shell shell) {
			this.shell = shell;
			Assert.isNotNull(objectId);
			Assert.isNotNull(region);
			Assert.isNotNull(shell);

			this.region = region;
			this.objectId = objectId;
		}

		@Override
		public IRegion getHyperlinkRegion() {
			return region;
		}

		@Override
		public String getTypeLabel() {
			return null;
		}

		@Override
		public String getHyperlinkText() {
			return objectId;
		}

		@Override
		public void open() {
			try {
				RepositoryCommit commit;
				commit = searchCommit();
				if (commit != null)
					CommitEditor.openQuiet(commit);
				else
					informCommitNotFound();
			} catch (IOException e) {
				// ignore
			}
		}

		private void informCommitNotFound() {
			MessageDialog
					.openWarning(
							shell,
							Messages.CommitHyperlinkDetector_CommitNotFound,
							NLS.bind(
									Messages.CommitHyperlinkDetector_CommitNotFoundInRepositories,
									objectId));
		}

		private RepositoryCommit searchCommit() throws IOException {
			RepositoryUtil repositoryUtil = Activator.getDefault()
					.getRepositoryUtil();
			List<String> configuredRepositories = repositoryUtil
					.getConfiguredRepositories();
			RepositoryCache repositoryCache = Activator.getDefault()
					.getRepositoryCache();
			for (String repoDir : configuredRepositories) {
				Repository repository = repositoryCache
						.lookupRepository(new File(repoDir));
				RevCommit commit = getCommit(repository);
				if (commit != null)
					return new RepositoryCommit(repository, commit);
			}

			return null;
		}

		private RevCommit getCommit(Repository repository) throws IOException {
			try (RevWalk revWalk = new RevWalk(repository)) {
				return revWalk.parseCommit(ObjectId.fromString(objectId));
			} catch (MissingObjectException e) {
				// ignore
				return null;
			} catch (IncorrectObjectTypeException e) {
				// ignore
				return null;
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CommitHyperlink [region="); //$NON-NLS-1$
			builder.append(region);
			builder.append(", objectId="); //$NON-NLS-1$
			builder.append(objectId);
			builder.append("]"); //$NON-NLS-1$
			return builder.toString();
		}

	}

	/**
	 * Detects and returns all available hyperlinks for the given
	 * {@link TextViewer} which link to a Git commit.
	 */
	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
			final IRegion region, boolean canShowMultipleHyperlinks) {
		IDocument document = textViewer.getDocument();
		if (document == null || document.getLength() == 0) {
			return null;
		}

		String content;
		int contentOffset;
		int index;
		try {
			if (region.getLength() == 0) {
				// expand the region to include the whole line
				IRegion lineInfo = document.getLineInformationOfOffset(region
						.getOffset());
				int lineLength = lineInfo.getLength();
				int lineOffset = lineInfo.getOffset();
				int lineEnd = lineOffset + lineLength;
				int regionEnd = region.getOffset() + region.getLength();
				if (lineOffset < region.getOffset()) {
					int regionLength = Math.max(regionEnd, lineEnd)
							- lineOffset;
					contentOffset = lineOffset;
					content = document.get(lineOffset, regionLength);
					index = region.getOffset() - lineOffset;
				} else {
					// the line starts after region, may never happen
					int regionLength = Math.max(regionEnd, lineEnd)
							- region.getOffset();
					contentOffset = region.getOffset();
					content = document.get(contentOffset, regionLength);
					index = 0;
				}
			} else {
				content = document.get(region.getOffset(), region.getLength());
				contentOffset = region.getOffset();
				index = -1;
			}
		} catch (BadLocationException ex) {
			return null;
		}

		List<IHyperlink> hyperlinks = detectHyperlinks(textViewer, content,
				index, contentOffset);
		if (hyperlinks == null) {
			return null;
		}

		// filter hyperlinks that do not match original region
		if (region.getLength() == 0) {
			for (Iterator<IHyperlink> it = hyperlinks.iterator(); it.hasNext();) {
				IHyperlink hyperlink = it.next();
				IRegion hyperlinkRegion = hyperlink.getHyperlinkRegion();
				if (!isInRegion(region, hyperlinkRegion)) {
					it.remove();
				}
			}
		}
		if (hyperlinks.isEmpty()) {
			return null;
		}
		return hyperlinks.toArray(new IHyperlink[0]);
	}

	private List<IHyperlink> detectHyperlinks(ITextViewer textViewer,
			String content, int index, int contentOffset) {
		Shell shell = textViewer.getTextWidget().getShell();
		List<IHyperlink> links = null;
		Matcher matcher = PATTERN_COMMIT_ID.matcher(content);
		while (matcher.find()) {
			if (index != -1
					&& (index < matcher.start() || index > matcher.end())) {
				continue;
			}
			if (links == null) {
				links = new ArrayList<IHyperlink>();
			}
			int start = matcher.start(1);
			Region region = new Region(contentOffset + start, matcher.end(1)
					- start);

			CommitHyperlink hyperlink = new CommitHyperlink(region,
					matcher.group(1), shell);
			links.add(hyperlink);
		}
		return links;
	}

	private boolean isInRegion(IRegion detectInRegion, IRegion hyperlinkRegion) {
		return detectInRegion.getOffset() >= hyperlinkRegion.getOffset()
				&& detectInRegion.getOffset() <= hyperlinkRegion.getOffset()
						+ hyperlinkRegion.getLength();
	}

}
