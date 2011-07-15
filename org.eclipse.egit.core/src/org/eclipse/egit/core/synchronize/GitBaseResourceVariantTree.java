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

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;

class GitBaseResourceVariantTree extends GitResourceVariantTree {

	public GitBaseResourceVariantTree(GitSyncCache cache, GitSynchronizeDataSet gsds) {
		super(new SessionResourceVariantByteStore(), cache, gsds);
	}

	@Override
	protected ObjectId getObjectId(DiffEntry diffEntry) {
		return diffEntry.getOldId().toObjectId();
	}

	@Override
	protected ObjectId getObjectId(GitSynchronizeData gsd) {
		return gsd.getDstRevCommit().getTree();
	}

	@Override
	protected RevCommit getCommitId(GitSynchronizeData gsd) {
		return gsd.getDstRevCommit();
	}

}
