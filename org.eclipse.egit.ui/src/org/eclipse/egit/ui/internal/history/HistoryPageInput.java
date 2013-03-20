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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.Activator;
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
		if (fileItems.length == 1 && fileItems[0].isFile()) {
			singleItem = fileItems[0];
			singleFile = fileItems[0];
		} else {
			singleItem = null;
			singleFile = null;
		}
		files = Arrays.asList(fileItems);
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
			Ref h = repo.getRef(Constants.HEAD);
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
}
