/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dariusz Luksza <dariusz@luksza.org>
 *     Laurent Goubet <laurent.goubet@obeo.fr> - 393294
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;

class GitBaseResourceVariantTree extends GitResourceVariantTree {

	public GitBaseResourceVariantTree(GitSyncCache cache, GitSynchronizeDataSet gsds) {
		super(new SessionResourceVariantByteStore(), cache, gsds);
	}

	@Override
	protected ObjectId getObjectId(ThreeWayDiffEntry diffEntry) {
		return diffEntry.getBaseId().toObjectId();
	}

	@Override
	protected RevCommit getCommitId(GitSynchronizeData gsd) {
		return gsd.getCommonAncestorRev();
	}

}
