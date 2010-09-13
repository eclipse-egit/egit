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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * This is a representation of a file's blob in some branch.
 */
public class GitBlobResourceVariant extends GitResourceVariant {

	private IStorage storage;

	private final ObjectLoader blob;

	GitBlobResourceVariant(Repository repo, RevCommit revCommit,
			ObjectId objectId, String path) throws IOException {
		super(repo, revCommit, objectId, path);

		blob = repo.open(getObjectId());
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
					try {
						return blob.openStream();
					} catch (IOException e) {
						throw new TeamException(e.getMessage(), e);
					}
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

}
