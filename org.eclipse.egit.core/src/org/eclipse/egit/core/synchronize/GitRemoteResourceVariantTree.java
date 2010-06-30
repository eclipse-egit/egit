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

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;

class GitRemoteResourceVariantTree extends GitResourceVariantTree {

	GitRemoteResourceVariantTree(GitSynchronizeDataSet data) {
		super(new SessionResourceVariantByteStore(), data);
	}

	@Override
	protected RevCommit getRevCommit(GitSynchronizeData gsd) throws TeamException {
		RevCommit result;
		Repository repo = gsd.getRepository();
		RevWalk rw = new RevWalk(repo);
		rw.setRevFilter(RevFilter.MERGE_BASE);

		try {
			Ref dstRef = repo.getRef(gsd.getDstRev());
			if (dstRef == null)
				result = null;
			else
				result = rw.parseCommit(dstRef.getObjectId());
		} catch (IOException e) {
			throw new TeamException("", e); //$NON-NLS-1$
		}

		return result != null ? result : null;
	}

}
