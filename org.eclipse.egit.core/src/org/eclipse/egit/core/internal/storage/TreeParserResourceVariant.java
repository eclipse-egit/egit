/*******************************************************************************
 * Copyright (C) 2014, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.team.core.TeamException;

/**
 * Implementation of a resource variant populated through a CanonicalTreeParser.
 * This can provide access to local resources as well as their remote variants.
 */
public class TreeParserResourceVariant extends AbstractGitResourceVariant {
	private TreeParserResourceVariant(Repository repository, String path,
			String fileName, boolean isContainer, ObjectId objectId, int rawMode) {
		super(repository, path, fileName, isContainer, objectId, rawMode);
	}

	/**
	 * Constructs a resource variant corresponding to the current entry of the
	 * given CanonicalTreeParser.
	 *
	 * @param repository
	 *            Repository from which this CanonicalTreeParser was created.
	 * @param treeParser
	 *            A CanonicalTreeParser to retrieve information from. This will
	 *            only read information about the current entry on which this
	 *            parser is positioned and will not change its state.
	 * @return The created variant.
	 */
	public static TreeParserResourceVariant create(Repository repository,
			CanonicalTreeParser treeParser) {
		final String path = treeParser.getEntryPathString();
		final String fileName;
		int lastSeparator = path.lastIndexOf('/');
		if (lastSeparator > 0)
			fileName = path.substring(lastSeparator + 1);
		else
			fileName = path;

		final boolean isContainer = FileMode.TREE.equals(treeParser
				.getEntryFileMode());
		final ObjectId objectId = treeParser.getEntryObjectId();
		final int rawMode = treeParser.getEntryRawMode();

		return new TreeParserResourceVariant(repository, path, fileName,
				isContainer, objectId, rawMode);
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return new BlobStorage(repository, path, objectId);
	}
}
