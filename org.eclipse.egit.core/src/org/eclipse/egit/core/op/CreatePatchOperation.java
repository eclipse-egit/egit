/*******************************************************************************
 * Copyright (C) 2011, Benjamin Muskalla <benjamin.muskalla@tasktop.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Creates a patch for a specific commit
 */
public class CreatePatchOperation implements IEGitOperation {

	private final RevCommit commit;

	private final Repository repository;

	private boolean useGitFormat = true;

	// the encoding for the currently processed file
	 private String currentEncoding = null;

	private String patchContent;

	/**
	 * Creates the new operation.
	 *
	 * @param repository
	 * @param commit
	 */
	public CreatePatchOperation(Repository repository, RevCommit commit) {
		if (repository == null)
			throw new IllegalArgumentException(
					CoreText.CreatePatchOperation_repoRequired);
		if (commit == null)
			throw new IllegalArgumentException(
					CoreText.CreatePatchOperation_commitRequired);
		this.repository = repository;
		this.commit = commit;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		EclipseGitProgressTransformer gitMonitor;
		if (monitor == null)
			gitMonitor = new EclipseGitProgressTransformer(
					new NullProgressMonitor());
		else
			gitMonitor = new EclipseGitProgressTransformer(monitor);

		final StringBuilder sb = new StringBuilder();
		final DiffFormatter diffFmt = new DiffFormatter(
				new ByteArrayOutputStream() {

					@Override
					public synchronized void write(byte[] b, int off, int len) {
						super.write(b, off, len);
						if (currentEncoding == null)
							sb.append(toString());
						else try {
							sb.append(toString(currentEncoding));
						} catch (UnsupportedEncodingException e) {
							sb.append(toString());
						}
						reset();
					}
				});

		diffFmt.setProgressMonitor(gitMonitor);

		RevCommit[] parents = commit.getParents();
		if (parents.length > 1)
			throw new IllegalStateException(
					"Cannot create patch for merge commit"); //$NON-NLS-1$

		if (parents.length == 0)
			throw new IllegalStateException(
					"Cannot create patch for first commit"); //$NON-NLS-1$

		if (useGitFormat)
			writeGitPatchHeader(sb);

		try {
			diffFmt.setRepository(repository);
			List<DiffEntry> diffs = diffFmt.scan(parents[0].getId(), commit.getId());
			for (DiffEntry ent : diffs) {
				String path;
				if (ChangeType.DELETE.equals(ent.getChangeType()))
					path = ent.getOldPath();
				else
					path = ent.getNewPath();
				currentEncoding = CompareCoreUtils.getResourceEncoding(repository, path);
				diffFmt.format(ent);
			}
		} catch (IOException e) {
			Activator.logError("Patch file could not be written", e); //$NON-NLS-1$
		}

		patchContent = sb.toString();
		// trim newline
		if (patchContent.endsWith("\n")) //$NON-NLS-1$
			patchContent = patchContent.substring(0, patchContent.length() - 1);
	}

	/**
	 * Retrieves the contents of the requested patch
	 *
	 * @return the content of the patch
	 */
	public String getPatchContent() {
		if (patchContent == null)
			throw new IllegalStateException(
					"#execute needs to be called before this method."); //$NON-NLS-1$
		return patchContent;
	}

	private void writeGitPatchHeader(StringBuilder sb) {
		final SimpleDateFormat dtfmt;
		dtfmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US); //$NON-NLS-1$
		dtfmt.setTimeZone(commit.getAuthorIdent().getTimeZone());
		sb.append("From").append(" ") //$NON-NLS-1$ //$NON-NLS-2$
				.append(commit.getId().getName()).append(" ") //$NON-NLS-1$
				.append(dtfmt.format(Long.valueOf(System.currentTimeMillis())))
				.append("\n"); //$NON-NLS-1$
		sb.append("From") //$NON-NLS-1$
				.append(": ") //$NON-NLS-1$
				.append(commit.getAuthorIdent().getName())
				.append(" <").append(commit.getAuthorIdent().getEmailAddress()) //$NON-NLS-1$
				.append(">\n"); //$NON-NLS-1$
		sb.append("Date").append(": ") //$NON-NLS-1$ //$NON-NLS-2$
				.append(dtfmt.format(commit.getAuthorIdent().getWhen()))
				.append("\n"); //$NON-NLS-1$
		sb.append("Subject").append(": [PATCH] ") //$NON-NLS-1$ //$NON-NLS-2$
				.append(commit.getShortMessage());

		String message = commit.getFullMessage().substring(
				commit.getShortMessage().length());
		sb.append(message).append("\n\n"); //$NON-NLS-1$
	}

	/**
	 * Decides whether to use the git format for patches.
	 *
	 * @param useFormat
	 */
	public void useGitFormat(boolean useFormat) {
		this.useGitFormat = useFormat;
	}

	/**
	 * Suggests a file name for the patch given the commit.
	 *
	 * @param commit
	 * @return a file name for a patch
	 */
	public static String suggestFileName(RevCommit commit) {
		String name = commit.getShortMessage();

		name = name.trim();
		StringBuilder filteredBuilder = new StringBuilder();
		char[] charArray = name.toCharArray();
		for (char c : charArray) {
			if(Character.isLetter(c) || Character.isDigit(c))
				filteredBuilder.append(c);
			if(Character.isWhitespace(c) || c == '/')
				filteredBuilder.append("-"); //$NON-NLS-1$
		}
		name = filteredBuilder.toString();
		if (name.length() > 52)
			name = name.substring(0, 52);
		while (name.endsWith(".")) //$NON-NLS-1$
			name = name.substring(0, name.length() - 1);
		name = name.concat(".patch"); //$NON-NLS-1$

		return name;
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}

}
