/*******************************************************************************
 * Copyright (C) 2006, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.jgit.lib.Repository;

/**
 * Input for the {@link GitHistoryPage}
 **/
public class HistoryPageInput {
	private final IResource[] list;

	private final File[] files;

	private final Repository repo;

	/**
	 * @param repository
	 *            the {@link Repository}
	 * @param resourceItems
	 *            the resources
	 */
	public HistoryPageInput(final Repository repository,
			final IResource[] resourceItems) {
		this.repo = repository;
		list = resourceItems;
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
		files = fileItems;
	}

	/**
	 * @param repository
	 *            the {@link Repository}
	 */
	public HistoryPageInput(final Repository repository) {
		this.repo = repository;
		list = null;
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
		return list;
	}

	/**
	 * @return the list provided to our constructor
	 */
	public File[] getFileList() {
		return files;
	}
}
