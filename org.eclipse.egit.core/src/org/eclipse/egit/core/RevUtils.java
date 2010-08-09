/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.IOException;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Utility class for obtaining Rev object instances.
 */
public class RevUtils {

	private RevUtils() {
		// non instanciable utility class
	}

	/**
	 * Finds and returns instance of common ancestor commit for given to
	 * commit's
	 *
	 * @param repo
	 * @param commit1
	 * @param commit2
	 * @return common ancestor for commit1 and commit2 parameters
	 * @throws IOException
	 */
	public static RevCommit getCommonAncestor(Repository repo,
			AnyObjectId commit1, AnyObjectId commit2) throws IOException {
		RevWalk rw = new RevWalk(repo);
		rw.setRevFilter(RevFilter.MERGE_BASE);

		RevCommit srcRev = rw.lookupCommit(commit1);
		RevCommit dstRev = rw.lookupCommit(commit2);

		rw.markStart(dstRev);
		rw.markStart(srcRev);

		RevCommit result;
		result = rw.next();

		return result != null ? result : null;
	}

}
