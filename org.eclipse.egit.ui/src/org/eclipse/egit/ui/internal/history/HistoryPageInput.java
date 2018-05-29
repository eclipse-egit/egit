/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

/**
 * Input for the {@link GitHistoryPage}
 **/
public class HistoryPageInput {
	private final List<IResource> list;

	private final List<File> files;

	private final Repository repo;

	private final Object singleFile;

	private final Object singleItem;

	/**
	 * @param repository
	 *            the {@link Repository}
	 * @param resourceItems
	 *            the resources
	 */
	public HistoryPageInput(final Repository repository,
			final IResource[] resourceItems) {
		this.repo = repository;
		list = Arrays.asList(resourceItems);
		if (resourceItems.length == 1) {
			singleItem = resourceItems[0];
			if (resourceItems[0].getType() == IResource.FILE)
				singleFile = resourceItems[0];
			else
				singleFile = null;
		} else {
			singleItem = null;
			singleFile = null;
		}
		files = null;
	}

	/**
	 * @param repository
	 *            the {@link Repository}
	 * @param fileItems
	 *            the files
	 */
	public HistoryPageInput(final Repository repository, final File[] fileItems) {
		this.repo = repository;
		list = null;
		if (fileItems.length == 1 && fileItems[0].isFile()
				&& !inGitDir(repository, fileItems[0])) {
			singleItem = fileItems[0];
			singleFile = fileItems[0];
			files = Arrays.asList(fileItems);
		} else {
			singleItem = null;
			singleFile = null;
			files = filterFilesInGitDir(repository, fileItems);
		}
	}

	/**
	 * @param repository
	 *            the {@link Repository}
	 */
	public HistoryPageInput(final Repository repository) {
		this.repo = repository;
		list = null;
		singleFile = null;
		singleItem = null;
		files = null;
	}

	/**
	 * @return the {@link Repository} provided to the constructor
	 */
	public Repository getRepository() {
		return repo;
	}

	/**
	 * @return the list provided to our constructor
	 */
	public IResource[] getItems() {
		return list == null ? null : list.toArray(new IResource[list.size()]);
	}

	/**
	 * @return the list provided to our constructor
	 */
	public File[] getFileList() {
		return files == null ? null : files.toArray(new File[files.size()]);
	}

	/**
	 * @return the single File, either a {@link IResource} or {@link File}, or
	 *         <code>null</code>
	 */
	public Object getSingleFile() {
		return singleFile;
	}

	/**
	 * @return the single Item, either a {@link IResource} or {@link File}, or
	 *         <code>null</code>
	 */
	public Object getSingleItem() {
		return singleItem;
	}

	/**
	 * @return <code>true</code> if this represents a single file (either as
	 *         {@link IResource} or as {@link File})
	 */
	public boolean isSingleFile() {
		return singleFile != null;
	}

	/**
	 * @return the HEAD Ref
	 */
	public Ref getHead() {
		try {
			Ref h = repo.exactRef(Constants.HEAD);
			if (h != null && h.isSymbolic())
				return h;
			return null;
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(
					UIText.GitHistoryPage_errorParsingHead, Activator
							.getDefault().getRepositoryUtil()
							.getRepositoryName(repo)), e);
		}
	}

	/**
	 * Tests whether a file is in a repository's .git directory (or is that
	 * directory itself).
	 *
	 * @param repository
	 *            to test against
	 * @param file
	 *            to test
	 * @return {@code true} if the file is in the .git directory of the
	 *         repository, or is that directory itself.
	 */
	private boolean inGitDir(Repository repository, File file) {
		return file.getAbsoluteFile().toPath().startsWith(
				repository.getDirectory().getAbsoluteFile().toPath());
	}

	/**
	 * Filters all files that are inside a repository's .git directory from the
	 * given files.
	 *
	 * @param repository
	 *            to test against
	 * @param fileItems
	 *            to test
	 * @return a list of all files from {@code files} that are <em>not</em> in
	 *         the .git directory of the repository, or {@code null} if there
	 *         are none.
	 */
	private List<File> filterFilesInGitDir(Repository repository,
			File[] fileItems) {
		List<File> result = new ArrayList<>(fileItems.length);
		Path gitDirPath = repository.getDirectory().getAbsoluteFile().toPath();
		for (File f : fileItems) {
			if (!f.getAbsoluteFile().toPath().startsWith(gitDirPath)) {
				result.add(f);
			}
		}
		if (result.isEmpty()) {
			return null;
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HistoryPageInput)) {
			return false;
		}
		HistoryPageInput other = (HistoryPageInput) obj;
		return repo == other.repo && singleFile == other.singleFile
				&& singleItem == other.singleItem
				&& listEquals(files, other.files)
				&& listEquals(list, other.list);
	}

	private <T> boolean listEquals(List<? extends T> a, List<? extends T> b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return Arrays.equals(a.toArray(), b.toArray());
	}

	@Override
	public int hashCode() {
		return (repo == null ? 0 : repo.hashCode())
				^ (singleFile == null ? 0 : singleFile.hashCode())
				^ (singleItem == null ? 0 : singleItem.hashCode())
				^ (files == null ? 0 : Arrays.hashCode(files.toArray()))
				^ (list == null ? 0 : Arrays.hashCode(list.toArray()));
	}
}
