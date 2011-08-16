/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jgit.lib.Constants;
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

		public IRegion getHyperlinkRegion() {
			return region;
		}

		public String getTypeLabel() {
			return null;
		}

		public String getHyperlinkText() {
			return objectId;
		}

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
			RevWalk revWalk = null;
			try {
				revWalk = new RevWalk(repository);
				return revWalk.parseCommit(ObjectId.fromString(objectId));
			} catch (MissingObjectException e) {
				// ignore
				return null;
			} catch (IncorrectObjectTypeException e) {
				// ignore
				return null;
			} finally {
				if (revWalk != null)
					revWalk.release();
			}

		}

	}

	/**
	 * Detects and returns all available hyperlinks for the given {@link TextViewer} which link to a Git commit.
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
			IRegion region, boolean canShowMultipleHyperlinks) {

		IDocument document = textViewer.getDocument();
		if (document == null || document.getLength() == 0)
			return null;

		String content;
		int offset = region.getOffset();
		int length = region.getLength();
		try {
			if (length == 0) {
				IRegion lineInformation = document
						.getLineInformationOfOffset(offset);
				offset = lineInformation.getOffset();
				length = lineInformation.getLength();
			}
			content = document.get(offset, length);
		} catch (BadLocationException e) {
			return null;
		}

		List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>();
		String[] words = content.split(" "); //$NON-NLS-1$
		Shell shell = textViewer.getTextWidget().getShell();
		for (String potentialId : words) {
			String foundId = null;
			int foundOffset = 0;
			if (ObjectId.isId(potentialId)) {
				foundId = potentialId;
				foundOffset = offset;
			} else if (potentialId.length() > Constants.OBJECT_ID_STRING_LENGTH) {
				// could be beginning or end of a sentence
				String potentialIdAtBeginning = potentialId.substring(0,
						Constants.OBJECT_ID_STRING_LENGTH);
				if (ObjectId.isId(potentialIdAtBeginning)) {
					foundId = potentialIdAtBeginning;
					foundOffset = offset;
				} else {
					String potentialIdAtEnd = potentialId.substring(potentialId
							.length() - Constants.OBJECT_ID_STRING_LENGTH);
					if (ObjectId.isId(potentialIdAtEnd)) {
						foundId = potentialIdAtEnd;
						foundOffset = potentialId.length()
								- Constants.OBJECT_ID_STRING_LENGTH;
					}

				}
			}
			if (foundId != null) {
				CommitHyperlink hyperlink = new CommitHyperlink(new Region(
						foundOffset, Constants.OBJECT_ID_STRING_LENGTH),
						foundId, shell);
				hyperlinks.add(hyperlink);
			}
			offset += potentialId.length() + 1;
		}

		// filter hyperlinks that do not match original region
		if (region.getLength() == 0) {
			for (Iterator<IHyperlink> it = hyperlinks.iterator(); it.hasNext();) {
				IHyperlink hyperlink = it.next();
				IRegion hyperlinkRegion = hyperlink.getHyperlinkRegion();
				if (!isInRegion(region, hyperlinkRegion))
					it.remove();
			}
		}

		if (hyperlinks.isEmpty())
			return null;

		return hyperlinks.toArray(new IHyperlink[hyperlinks.size()]);
	}

	private boolean isInRegion(IRegion detectInRegion, IRegion hyperlinkRegion) {
		return detectInRegion.getOffset() >= hyperlinkRegion.getOffset()
				&& detectInRegion.getOffset() <= hyperlinkRegion.getOffset()
						+ hyperlinkRegion.getLength();
	}

}
