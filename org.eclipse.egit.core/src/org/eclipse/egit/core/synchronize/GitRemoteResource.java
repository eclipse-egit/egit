/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.storage.CommitBlobStorage;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.CachedResourceVariant;

abstract class GitRemoteResource extends CachedResourceVariant {

	private final String path;

	private final Repository repo;

	private final ObjectId objectId;

	private final RevCommit revCommit;

	GitRemoteResource(Repository repo, RevCommit revCommit, ObjectId objectId,
			String path) {
		this.repo = repo;
		this.path = path;
		this.objectId = objectId;
		this.revCommit = revCommit;
	}

	public String getName() {
		int lastSeparator = path.lastIndexOf("/"); //$NON-NLS-1$
		return path.substring(lastSeparator + 1, path.length());
	}

	public String getContentIdentifier() {
		return revCommit.abbreviate(7).name() + "..."; //$NON-NLS-1$
	}

	public byte[] asBytes() {
		return getObjectId().name().getBytes();
	}

	@Override
	protected void fetchContents(IProgressMonitor monitor) throws TeamException {
		CommitBlobStorage content = new CommitBlobStorage(repo, path, objectId, revCommit);
		try {
			setContents(content.getContents(), monitor);
		} catch (CoreException e) {
			Activator.error("", e); //$NON-NLS-1$
		}
	}

	@Override
	protected String getCachePath() {
		return path;
	}

	@Override
	protected String getCacheId() {
		return "org.eclipse.egit"; //$NON-NLS-1$
	}

	boolean exists() {
		return objectId != null;
	}

	/**
	 * @return object id, or {code {@link ObjectId#zeroId()} if object doesn't
	 *         exist in repository
	 */
	ObjectId getObjectId() {
		return objectId != null ? objectId : zeroId();
	}

	String getPath() {
		return path;
	}

	Repository getRepo() {
		return repo;
	}

	RevCommit getCommit() {
		return revCommit;
	}

}
