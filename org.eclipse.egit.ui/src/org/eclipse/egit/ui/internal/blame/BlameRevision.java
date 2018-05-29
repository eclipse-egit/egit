/******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.RGB;

/**
 * Annotation revision
 */
public class BlameRevision extends Revision {

	private int start;

	private int lines = 1;

	private RevCommit commit;

	private Repository repository;

	private String sourcePath;

	private Map<Integer, Integer> sourceLines = new HashMap<>();

	private Map<RevCommit, Diff> diffToParentCommit = new HashMap<>();

	@Override
	public Object getHoverInfo() {
		return this;
	}

	@Override
	public RGB getColor() {
		return AuthorColors.getDefault().getCommitterRGB(getAuthor());
	}

	@Override
	public String getId() {
		return commit.abbreviate(7).name();
	}

	@Override
	public Date getDate() {
		PersonIdent person = commit.getAuthorIdent();
		if( person == null)
			person = commit.getCommitterIdent();
		return person != null ? person.getWhen() : new Date(0);
	}

	/**
	 * Register revision
	 *
	 * @return this revision
	 */
	public BlameRevision register() {
		addRange(new LineRange(start, lines));
		return this;
	}

	/**
	 * Increment line count
	 *
	 * @return this revision
	 */
	public BlameRevision addLine() {
		lines++;
		return this;
	}

	/**
	 * Reset revision
	 *
	 * @param number
	 * @return this revision
	 */
	public BlameRevision reset(int number) {
		start = number;
		lines = 1;
		return this;
	}

	/**
	 * Set revision
	 *
	 * @param commit
	 * @return this
	 */
	public BlameRevision setCommit(RevCommit commit) {
		this.commit = commit;
		return this;
	}

	/**
	 * Get revision
	 *
	 * @return revision
	 */
	public RevCommit getCommit() {
		return this.commit;
	}

	/**
	 * Set repository
	 *
	 * @param repository
	 * @return this
	 */
	public BlameRevision setRepository(Repository repository) {
		this.repository = repository;
		return this;
	}

	/**
	 * Get repository
	 *
	 * @return repository
	 */
	public Repository getRepository() {
		return this.repository;
	}

	@Override
	public String getAuthor() {
		return commit.getAuthorIdent().getName();
	}

	/**
	 * @return repository-relative path of file
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * @param sourcePath
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * Get the line number the content had in the source of this blame
	 * information.
	 *
	 * @param currentLine
	 *            0-based line number
	 * @return 0-based source line or null
	 */
	public Integer getSourceLine(int currentLine) {
		return sourceLines.get(Integer.valueOf(currentLine));
	}

	/**
	 * @param currentLine
	 *            0-based line number
	 * @param sourceLine
	 *            0-based line number
	 */
	public void addSourceLine(int currentLine, int sourceLine) {
		sourceLines.put(Integer.valueOf(currentLine),
				Integer.valueOf(sourceLine));
	}

	/**
	 * @param parentCommit
	 * @return the diff or null if none could be calculated
	 */
	public Diff getDiffToParent(RevCommit parentCommit) {
		if (diffToParentCommit.containsKey(parentCommit))
			return diffToParentCommit.get(parentCommit);

		Diff diff = calculateDiffToParent(parentCommit);
		diffToParentCommit.put(parentCommit, diff);
		return diff;
	}

	private Diff calculateDiffToParent(RevCommit parentCommit) {
		try (ObjectReader reader = repository.newObjectReader()) {
			DiffEntry diffEntry = CompareCoreUtils.getChangeDiffEntry(
					repository, sourcePath, commit, parentCommit, reader);
			if (diffEntry == null)
				return null;

			RawText oldText = readText(diffEntry.getOldId(), reader);
			RawText newText = readText(diffEntry.getNewId(), reader);

			StoredConfig config = repository.getConfig();
			DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(config
					.getEnum(ConfigConstants.CONFIG_DIFF_SECTION, null,
							ConfigConstants.CONFIG_KEY_ALGORITHM,
							SupportedAlgorithm.HISTOGRAM));

			EditList editList = diffAlgorithm.diff(RawTextComparator.DEFAULT,
					oldText, newText);

			return new Diff(diffEntry.getOldPath(), oldText, newText, editList);
		} catch (IOException e) {
			return null;
		}
	}

	private static RawText readText(AbbreviatedObjectId blobId,
			ObjectReader reader) throws IOException {
		ObjectLoader oldLoader = reader.open(blobId.toObjectId(),
				Constants.OBJ_BLOB);
		return new RawText(oldLoader.getCachedBytes());
	}

	/**
	 * Information about the diff to a parent commit of the blamed revision.
	 */
	public static class Diff {
		private final String oldPath;

		private final RawText oldText;

		private final RawText newText;

		private final EditList editList;

		private Diff(String oldPath, RawText oldText, RawText newText,
				EditList editList) {
			this.oldPath = oldPath;
			this.oldText = oldText;
			this.newText = newText;
			this.editList = editList;
		}

		/**
		 * @return old path of file in diff
		 */
		public String getOldPath() {
			return oldPath;
		}

		/**
		 * @return old text of diff
		 */
		public RawText getOldText() {
			return oldText;
		}

		/**
		 * @return new text of diff
		 */
		public RawText getNewText() {
			return newText;
		}

		/**
		 * @return edit list of diff
		 */
		public EditList getEditList() {
			return editList;
		}
	}
}
