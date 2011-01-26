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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.storage.CommitBlobStorage;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;

/**
 * This is a representation of a file's blob in some branch.
 */
public class GitBlobResourceVariant extends GitResourceVariant {

	GitBlobResourceVariant(Repository repo, RevCommit revCommit,
			ObjectId objectId, String path) throws IOException {
		super(repo, revCommit, objectId, path);
	}

	public boolean isContainer() {
		return false;
	}

	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		return new CommitBlobStorage(getRepository(), getPath(),
					getObjectId(), getRevCommit());
	}

}
