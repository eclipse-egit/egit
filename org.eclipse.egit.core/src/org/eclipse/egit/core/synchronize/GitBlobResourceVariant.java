/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.egit.core.Activator;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.team.core.TeamException;

/**
 * This is a representation of a file's blob in some branch.
 */
class GitBlobResourceVariant extends GitResourceVariant {

	private IStorage storage;

	private byte[] bytes;

	GitBlobResourceVariant(Repository repo, RevCommit revCommit, String path)
			throws IOException {
		super(repo, revCommit, path);

		if (getObjectId() != null) {
			ObjectLoader blob = repo.open(getObjectId());
			bytes = blob.getBytes();
		}
	}

	public boolean isContainer() {
		return false;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		if (storage == null) {
			storage = new IEncodedStorage() {
				public Object getAdapter(Class adapter) {
					return null;
				}

				public boolean isReadOnly() {
					return true;
				}

				public String getName() {
					return GitBlobResourceVariant.this.getName();
				}

				public IPath getFullPath() {
					return null;
				}

				public InputStream getContents() throws CoreException {
					return new ByteArrayInputStream(bytes);
				}

				public String getCharset() throws CoreException {
					IContentTypeManager manager = Platform
							.getContentTypeManager();
					try {
						IContentDescription description = manager
								.getDescriptionFor(getContents(),
										getName(), IContentDescription.ALL);
						return description == null ? null : description
								.getCharset();
					} catch (IOException e) {
						throw new CoreException(new Status(IStatus.ERROR,
								Activator.getPluginId(), e.getMessage(), e));
					}
				}
			};
		}

		return storage;
	}

	public byte[] asBytes() {
		return bytes;
	}

	@Override
	protected TreeWalk getTreeWalk(Repository repo, RevTree revTree,
			String path) throws IOException {
		TreeWalk tw = new TreeWalk(repo);
		tw.reset();
		tw.addTree(revTree);
		tw.setRecursive(true);
		tw.setFilter(PathFilter.create(path));

		return tw.next() ? tw : null;
	}

}
