/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Benjamin Muskalla (Tasktop Technologies) - extract into operation
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.FileTreeIterator;

/**
 * Creates a patch for a specific commit
 */
public class CreatePatchOperation implements IEGitOperation {

	/**
	 * Diff header format
	 *
	 */
	public enum DiffHeaderFormat {
		/**
		 * No header
		 */
		NONE(CoreText.DiffHeaderFormat_None, false, null),

		/**
		 * Workspace patch
		 */
		WORKSPACE(CoreText.DiffHeaderFormat_Workspace, false, "### Eclipse Workspace Patch 1.0\n#P ${project}\n"), //$NON-NLS-1$

		/**
		 * Email header
		 */
		EMAIL(CoreText.DiffHeaderFormat_Email, true, "From ${sha1} ${date}\nFrom: ${author}\nDate: ${author date}\nSubject: [PATCH] ${title line}\n${full commit message}\n"), //$NON-NLS-1$

		/**
		 * Header designed to be as compact as possible
		 */
		ONELINE(CoreText.DiffHeaderFormat_Oneline, true, "${sha1} ${title line}\n"); //$NON-NLS-1$

		private final String description;

		private final boolean commitRequired;

		private final String template;

		private DiffHeaderFormat(final String d, final boolean c, final String t) {
			description = d;
			commitRequired = c;
			template = t;
		}

		/**
		 * @return if this format requires a commit.
		 */
		public boolean isCommitRequired() {
			return commitRequired;
		}

		/**
		 * @return the template
		 */
		public String getTemplate() {
			return template;
		}

		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
	}

	enum DiffHeaderKeyword{
		SHA1, AUTHOR_DATE, AUTHOR, DATE, TITLE_LINE, FULL_COMMIT_MESSAGE, PROJECT
	}

	/**
	 * The default number of lines to use as context
	 */
	public static final int DEFAULT_CONTEXT_LINES = 3;

	private final RevCommit commit;

	private IResource resource;

	private final Repository repository;

	private DiffHeaderFormat headerFormat = DiffHeaderFormat.EMAIL;

	// the encoding for the currently processed file
	 private String currentEncoding = null;

	private String patchContent;

	private int contextLines = DEFAULT_CONTEXT_LINES;


	/**
	 * Creates the new operation.
	 *
	 * @param repository
	 * @param commit
	 * @param resource
	 */
	public CreatePatchOperation(Repository repository, RevCommit commit, IResource resource) {
		if (repository == null)
			throw new IllegalArgumentException(
					CoreText.CreatePatchOperation_repoRequired);
		this.repository = repository;
		this.commit = commit;
		this.resource = resource;
	}

	/**
	 * Creates the new operation.
	 *
	 * @param repository
	 * @param commit
	 */
	public CreatePatchOperation(Repository repository, RevCommit commit) {
		this(repository, commit, null);
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
		diffFmt.setContext(contextLines);

		if (headerFormat != null && headerFormat != DiffHeaderFormat.NONE)
			writeGitPatchHeader(sb);

		diffFmt.setRepository(repository);

		try {
			if (commit != null) {
				RevCommit[] parents = commit.getParents();
				if (parents.length > 1)
					throw new IllegalStateException(
							CoreText.CreatePatchOperation_cannotCreatePatchForMergeCommit);
				if (parents.length == 0)
					throw new IllegalStateException(
							CoreText.CreatePatchOperation_cannotCreatePatchForFirstCommit);

				List<DiffEntry> diffs = diffFmt.scan(parents[0].getId(),commit.getId());
				for (DiffEntry ent : diffs) {
					String path;
					if (ChangeType.DELETE.equals(ent.getChangeType()))
						path = ent.getOldPath();
					else
						path = ent.getNewPath();
					currentEncoding = CompareCoreUtils.getResourceEncoding(repository, path);
					diffFmt.format(ent);
				}
			} else {
				diffFmt.format(
						new DirCacheIterator(repository.readDirCache()),
						new FileTreeIterator(repository));
			}
		} catch (IOException e) {
			Activator.logError(CoreText.CreatePatchOperation_patchFileCouldNotBeWritten, e);
		}

		if (DiffHeaderFormat.WORKSPACE == headerFormat)
			updateWorkspacePatchPrefixes(sb, resource, diffFmt);

		// trim newline
		if (sb.charAt(sb.length() - 1) == '\n')
			sb.setLength(sb.length() - 1);

		patchContent = sb.toString();
	}

	/**
	 * Retrieves the content of the requested patch
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
		String template = headerFormat.getTemplate();
		String[] segments = template.split("\\$\\{"); //$NON-NLS-1$
		Stack<String> evaluated = new Stack<String>();
		evaluated.add(segments[0]);

		for (int i = 1; i < segments.length; i++) {
			String segment = segments[i];
			String value = null;
			int brace = segment.indexOf('}');
			if (brace > 0) {
				String keyword = segment.substring(0, brace);
				keyword = keyword.toUpperCase().replaceAll(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
				value = processKeyword(commit, resource, DiffHeaderKeyword.valueOf(keyword));
			}

			String trailingCharacters = segment.substring(brace + 1);
			if (value != null) {
				evaluated.add(value);
				evaluated.add(trailingCharacters);
			} else if (!evaluated.isEmpty())
				evaluated.add(trailingCharacters);
		}
		StringBuffer buffer = new StringBuffer();
		for (String string : evaluated)
			buffer.append(string);

		sb.append(buffer);
	}

	private static String processKeyword(RevCommit commit, IResource resource,
			DiffHeaderKeyword keyword) {
		final SimpleDateFormat dtfmt = new SimpleDateFormat(
				"EEE, d MMM yyyy HH:mm:ss Z", Locale.US); //$NON-NLS-1$
		switch (keyword) {
		case SHA1:
			return commit.getId().getName();
		case AUTHOR:
			return commit.getAuthorIdent().getName()
					+ " <" + commit.getAuthorIdent().getEmailAddress() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		case AUTHOR_DATE:
			dtfmt.setTimeZone(commit.getAuthorIdent().getTimeZone());
			return dtfmt.format(commit.getAuthorIdent().getWhen());
		case DATE:
			return dtfmt.format(Long.valueOf(System.currentTimeMillis()));
		case TITLE_LINE:
			return commit.getShortMessage();
		case FULL_COMMIT_MESSAGE:
			return commit.getFullMessage().substring(
					commit.getShortMessage().length());
		case PROJECT:
			return resource.getProject().getName();
		default:
			return null;
		}
	}

	/**
	 * TODO: made public for tests
	 *
	 * @param sb
	 * @param resource
	 * @param diffFmt
	 */
	public static void updateWorkspacePatchPrefixes(StringBuilder sb, IResource resource, DiffFormatter diffFmt) {
		RawText rt = new RawText(sb.toString().getBytes());

		final String oldPrefix = diffFmt.getOldPrefix();
		final String newPrefix = diffFmt.getNewPrefix();

		StringBuilder newSb = new StringBuilder();
		final String eol = rt.getEOL();

		final Pattern diffPattern = Pattern
				.compile("^diff --git (" + oldPrefix + ".+) (" + newPrefix + ".+)$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final Pattern oldPattern = Pattern
				.compile("^--- (" + oldPrefix + ".+)$"); //$NON-NLS-1$ //$NON-NLS-2$
		final Pattern newPattern = Pattern
				.compile("^\\+\\+\\+ (" + newPrefix + ".+)$"); //$NON-NLS-1$ //$NON-NLS-2$

		int i = 0;
		while (i < rt.size()) {
			String line = rt.getString(i);

			Matcher diffMatcher = diffPattern.matcher(line);
			Matcher oldMatcher = oldPattern.matcher(line);
			Matcher newMatcher = newPattern.matcher(line);
			if (diffMatcher.find()) {
				String group = diffMatcher.group(1); // old path
				IPath newPath = processPath(new Path(group), resource.getProject(), new Path(oldPrefix));
				line = line.replaceAll(group, newPath.toString());
				group = diffMatcher.group(2); // new path
				newPath = processPath(new Path(group), resource.getProject(), new Path(newPrefix));
				line = line.replaceAll(group, newPath.toString());
			} else if (oldMatcher.find()) {
				String group = oldMatcher.group(1);
				IPath newPath = processPath(new Path(group), resource.getProject(), new Path(oldPrefix));
				line = line.replaceAll(group, newPath.toString());
			} else if (newMatcher.find()) {
				String group = newMatcher.group(1);
				IPath newPath = processPath(new Path(group), resource.getProject(), new Path(newPrefix));
				line = line.replaceAll(group, newPath.toString());
			}
			newSb.append(line);

			i++;
			if (i < rt.size() || !rt.isMissingNewlineAtEnd()) {
				newSb.append(eol);
			}
		}
		// reset sb to newSb
		sb.setLength(0);
		sb.append(newSb);
	}

	/**
	 * TODO: made public for tests
	 *
	 * @param path
	 * @param project
	 * @param prefix
	 * @return path
	 */
	public static IPath processPath(final IPath path, final IProject project, final IPath prefix) {
		RepositoryMapping rm = RepositoryMapping.getMapping(project);
		String repoRelativePath = rm.getRepoRelativePath(project);
		// the relative path cannot be determined, return unchanged
		if (repoRelativePath == null)
			return path;
		IPath result = path.removeFirstSegments(prefix.segmentCount());
		// repository and project at the same level
		if (repoRelativePath.equals("")) //$NON-NLS-1$
			return result;
		return result.removeFirstSegments(result
				.matchingFirstSegments(new Path(repoRelativePath)));
	}


	/**
	 * Change the format of diff header
	 *
	 * @param format header format
	 */
	public void setHeaderFormat(DiffHeaderFormat format) {
		this.headerFormat = format;
	}

	/**
	 * Change the number of lines of context to display.
	 *
	 * @param contextLines line count
	 */
	public void setContextLines(int contextLines) {
		this.contextLines = contextLines;
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
