/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.efs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.SystemReader;

/**
 * A simple EFS for accessing data from a git repository.
 * <p>
 * URI scheme: <b>egit-internal</b>
 * </p>
 * <p>
 * URI format: egit-internal:/&lt;repo_path>/$&lt;n>$&ltselector>/&lt;git_path>
 * </p>
 * <dl>
 * <dt>repo_path</dt>
 * <dd>Absolute path to the repository's .git directory, using '/' as directory
 * separator.</dd>
 * <dt>$n</dt>
 * <dd>Marker to reliably determine where the {@code repo_path} finishes.
 * {@code n} gives the number of path components of {@code repo_path}.
 * <dt>selector</dt>
 * <dd>Defines which item to serve. Currently supported selectors are:
 * <ul>
 * <li><b>WORK[:[oO[[number]]</b>: serves an item from the repository's working
 * tree. If followed by argument 'o' or 'O' the content is filtered by an
 * OursVersionInputStream. 'O' is for diff3 style; 'o' for merge style. If a
 * number is given (can be multiple digits), it's taken as the length of the
 * conflict markers. If none is given, a default conflict marker size of 7 is
 * assumed.</li>
 * </ul>
 * </dd>
 * <dt>git_path</dt>
 * <dd>Repository-relative path of the item in the git repository.</dd>
 * </dl>
 */
public class EgitFileSystem extends FileSystem {

	/**
	 * The URI scheme for this EFS.
	 */
	public static final String SCHEME = "egit-internal"; //$NON-NLS-1$

	@Override
	public IFileStore getStore(IPath path) {
		return EFS.getNullFileSystem().getStore(path);
	}

	@Override
	public IFileStore getStore(URI uri) {
		if (uri.getScheme().equalsIgnoreCase(getScheme())) {
			try {
				UriComponents parsedUri = UriComponents.parse(uri);
				switch (parsedUri.getKind()) {
				case COMMIT:
					throw new URISyntaxException(uri.toString(),
							"COMMIT selector not implemented yet"); //$NON-NLS-1$
				case INDEX:
					// Not implemented yet. Contrary to static data (working
					// tree and commits), the git index is highly volatile.
					// Resources appear/disappear when the user stages/unstages
					// changes, or rebase/merges, conflicts appear or are
					// resolved, ... An EFS has no way to notify higher layers
					// of file system changes, and it is a bit unclear how and
					// when to react on index changes. When would a listener
					// need to be unregistered? React on JGit IndexChangedEvents
					// or on EGit IndexDiffChangedEvents? What to do when an
					// index item is opened in an editor, say from a conflict
					// stage, and the conflict is resolved and the stage
					// disappears?
					throw new URISyntaxException(uri.toString(),
							"INDEX selector not implemented yet"); //$NON-NLS-1$
				case WORKTREE:
					if (StringUtils.isEmptyOrNull(parsedUri.getArguments())) {
						return parsedUri.getBaseFile();
					}
					return new FilteredWorktreeFileStore(parsedUri);
				default:
					throw new URISyntaxException(uri.toString(),
							MessageFormat.format("Unknown selector {0}", //$NON-NLS-1$
									parsedUri.getKind()));
				}
			} catch (IOException | URISyntaxException e) {
				Activator.logError(e.getLocalizedMessage(), e);
			}
		}
		return EFS.getNullFileSystem().getStore(uri);
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public int attributes() {
		return EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_SYMLINK; // immutable?
	}

	/**
	 * Creates an URI appropriate for this EFS.
	 *
	 * @param repository
	 *            to encode in the URI
	 * @param gitPath
	 *            to encode in the URI
	 * @param arguments
	 *            to encode in the URI
	 * @return the {@link URI}
	 * @throws URISyntaxException
	 *             if the URI cannot be created
	 */
	public static URI createURI(Repository repository, String gitPath,
			String arguments) throws URISyntaxException {
		File gitDir = repository.getDirectory();
		String repoPath = gitDir.toURI().getPath();
		// 'repoPath' starts and ends with a forward slash and uses forward
		// slashes as file separator.
		int n = slashCount(repoPath);
		if (n > 0) {
			n--;
			if (n > 0 && repoPath.length() > 2 && repoPath.charAt(1) == '/') {
				// UNC path.
				n--;
			}
		}
		URI uri = new URI(SCHEME, null,
				"//" + repoPath + '$' + n + '$' + arguments + '/' + gitPath, //$NON-NLS-1$
				null);
		UriComponents.parse(uri);
		return uri;
	}

	private static int slashCount(String s) {
		int n = 0;
		int l = s.length();
		for (int i = 0; i < l; i++) {
			if (s.charAt(i) == '/') {
				n++;
			}
		}
		return n;
	}

	enum StoreKind {

		/** Data from the index. */
		INDEX {

			@Override
			public boolean validate(String args) {
				return false; // Not implemented yet
			}
		},

		/** Data from a commit. */
		COMMIT {

			@Override
			public boolean validate(String args) {
				return false; // Not implemented yet
			}
		},

		/** Data from a working tree. */
		WORKTREE {

			@Override
			public boolean validate(String args) {
				if (StringUtils.isEmptyOrNull(args)) {
					return true;
				}
				return WORKTREE_ARGS.matcher(args).matches();
			}
		};

		private static final Pattern WORKTREE_ARGS = Pattern
				.compile("[oO]\\d*"); //$NON-NLS-1$

		public abstract boolean validate(String args);
	}

	static class UriComponents {

		private static final Pattern MARKER = Pattern
				.compile("/\\$(\\d+)\\$([^/]*)(?:/|$)"); //$NON-NLS-1$

		private final String repoPath;

		private final int segmentCount;

		private final StoreKind kind;

		private final String arguments;

		private final String gitPath;

		private UriComponents(String repoPath, int segmentCount,
				StoreKind kind, String arguments, String gitPath) {
			this.repoPath = repoPath;
			this.segmentCount = segmentCount;
			this.kind = kind;
			this.arguments = arguments;
			this.gitPath = gitPath;
		}

		static UriComponents parse(URI uri) throws URISyntaxException {
			String path = uri.getPath();
			if (!path.startsWith("/")) { //$NON-NLS-1$
				throw new URISyntaxException(uri.toString(),
						"URI must be absolute"); //$NON-NLS-1$
			}
			Matcher m = MARKER.matcher(path);
			String repoPath;
			String selector;
			String filePath;
			int segments = -1;
			if (m.find()) {
				repoPath = path.substring(0, m.start());
				try {
					segments = Integer.parseInt(m.group(1));
				} catch (NumberFormatException e) {
					throw new URISyntaxException(uri.toString(),
							"Invalid number of segments", //$NON-NLS-1$
							m.start(1));
				}
				selector = m.group(2);
				filePath = path.substring(m.end());
			} else {
				throw new URISyntaxException(uri.toString(),
						"Invalid URI"); //$NON-NLS-1$
			}
			if (repoPath.isEmpty()) {
				throw new URISyntaxException(uri.toString(),
						"No repository path"); //$NON-NLS-1$
			}
			int n = segmentCount(repoPath);
			if (segments >= 0 && n != segments) {
				throw new URISyntaxException(uri.toString(),
						MessageFormat.format(
								"Expected {0} segments for the repository path", //$NON-NLS-1$
								Integer.toString(segments)));
			}
			int i = selector.indexOf(':');
			StoreKind kind = null;
			String arguments = null;
			try {
				if (i < 0) {
					kind = StoreKind.valueOf(selector);
				} else {
					kind = StoreKind.valueOf(selector.substring(0, i));
					arguments = selector.substring(i + 1);
				}
			} catch (IllegalArgumentException e) {
				throw new URISyntaxException(uri.toString(),
						MessageFormat.format("Unknown selector {0}", //$NON-NLS-1$
								selector));
			}
			if (!kind.validate(arguments)) {
				throw new URISyntaxException(uri.toString(),
						MessageFormat.format("Unknown selector arguments {0}", //$NON-NLS-1$
								arguments));
			}
			return new UriComponents(repoPath, segments, kind,
					arguments, filePath);
		}

		private static int segmentCount(String path) {
			int n = slashCount(path);
			if (path.length() > 1 && path.charAt(1) == '/') {
				// UNC path
				return n - 1;
			}
			return n;
		}

		UriComponents parent() {
			int i = gitPath.lastIndexOf('/');
			if (i > 0) {
				return new UriComponents(repoPath, segmentCount,
						kind, arguments, gitPath.substring(0, i));
			}
			return null;
		}

		UriComponents child(String name) {
			return new UriComponents(repoPath, segmentCount, kind, arguments,
					gitPath.isEmpty() ? name : gitPath + '/' + name);
		}

		private String path() {
			String args = StringUtils.isEmptyOrNull(arguments) ? "" //$NON-NLS-1$
					: ':' + arguments;
			return repoPath + '/' + '$' + segmentCount + '$' + kind.name()
					+ args + '/' + gitPath;
		}

		URI toUri() {
			try {
				return new URI(SCHEME, null, path(), null);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}

		IFileStore getBaseFile() throws IOException {
			File gitDir = getRepoDir();
			Repository repository = RepositoryCache.getInstance()
					.lookupRepository(gitDir);
			if (repository == null) {
				throw new IOException(MessageFormat
						.format("Cannot find repository {0}", gitDir)); //$NON-NLS-1$
			}
			File worktree = repository.getWorkTree();
			if (!StringUtils.isEmptyOrNull(gitPath)) {
				worktree = new File(worktree, gitPath);
			}
			return EFS.getLocalFileSystem().fromLocalFile(worktree);
		}

		final Repository getRepository() {
			try {
				return RepositoryCache.getInstance()
						.lookupRepository(getRepoDir());
			} catch (IOException e) {
				return null;
			}
		}

		final File getRepoDir() {
			String path = repoPath;
			if (SystemReader.getInstance().isWindows()) {
				if (path.length() > 2 && path.charAt(0) == '/'
						&& path.charAt(2) == ':') {
					char ch = path.charAt(1);
					if (ch >= 'A' && ch <= 'Z') {
						path = path.substring(1);
					}
				}
			}
			if (File.separatorChar != '/') {
				path = path.replace('/', File.separatorChar);
			}
			return new File(path);
		}

		final int getSegmentCount() {
			return segmentCount;
		}

		final String getGitPath() {
			return gitPath;
		}

		final StoreKind getKind() {
			return kind;
		}

		final String getArguments() {
			return arguments;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			UriComponents other = (UriComponents) obj;
			return segmentCount == other.segmentCount
					&& Objects.equals(repoPath, other.repoPath)
					&& Objects.equals(gitPath, other.gitPath)
					&& Objects.equals(arguments, other.arguments);
		}

		@Override
		public int hashCode() {
			return Objects.hash(repoPath, gitPath, arguments) * 31
					+ Integer.hashCode(segmentCount);
		}

		@Override
		public String toString() {
			return SCHEME + "://" + path(); //$NON-NLS-1$
		}
	}
}
