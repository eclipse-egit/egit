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

class GitBaseResourceVariantTree extends GitResourceVariantTree {

	GitBaseResourceVariantTree(GitSynchronizeDataSet data) {
		super(new GitResourceVariantByteStore(), data);
	}

	@Override
	protected RevCommit getBaseRevCommit(GitSynchronizeData gsd) throws IOException {
		Repository repo = gsd.getRepository();

		Ref srcRef = repo.getRef(gsd.getSrcRev());
		Ref dstRef = repo.getRef(gsd.getDstRev());

		RevWalk rw = new RevWalk(repo);
		RevCommit srcRev = rw.lookupCommit(srcRef.getObjectId());
		RevCommit dstRev = rw.lookupCommit(dstRef.getObjectId());

		rw.setRevFilter(RevFilter.MERGE_BASE);
		rw.markStart(dstRev);
		rw.markStart(srcRev);

		return rw.next();
	}

}
