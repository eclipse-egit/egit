/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.lib.Repository;

/** Input for the {@link GitHistoryPage} */
public class HistoryPageInput {
	private final IResource[] list;

	private final Repository repo;

	private final File[] fileList;

	/**
	 * @param repository
	 *            the {@link Repository}
	 * @param items
	 *            the items to contain in this list
	 */
	public HistoryPageInput(final Repository repository, final IResource[] items) {
		list = items;
		repo = repository;
		fileList = null;
	}

	/**
	 * @param repository
	 *            the {@link Repository} provided to our constructor
	 */
	public HistoryPageInput(final Repository repository) {
		repo = repository;
		list = null;
		fileList = null;
	}

	/**
	 * @param repository
	 *            the {@link Repository} provided to our constructor
	 * @param files
	 *            selected files
	 */
	public HistoryPageInput(final Repository repository, File[] files) {
		repo = repository;
		list = null;
		fileList = files;
	}

	/**
	 * @return the list provided to our constructor
	 */
	public IResource[] getItems() {
		return list;
	}

	/**
	 * @return the Repository
	 */
	public Repository getRepository() {
		return repo;
	}

	/**
	 * @return a list of Files
	 */
	public File[] getFileList() {
		return fileList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(fileList);
		result = prime * result + Arrays.hashCode(list);
		result = prime * result + repo.getWorkTree().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HistoryPageInput other = (HistoryPageInput) obj;
		if (!Arrays.equals(fileList, other.fileList))
			return false;
		if (!Arrays.equals(list, other.list))
			return false;
		if (!repo.getWorkTree().equals(other.repo.getWorkTree()))
			return false;
		return true;
	}
}
