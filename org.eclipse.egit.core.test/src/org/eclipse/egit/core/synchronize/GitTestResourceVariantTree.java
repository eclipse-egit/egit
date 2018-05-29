/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

/**
 * Implementation of abstract {@link GitResourceVariantTree} class. It is only
 * used in {@link GitResourceVariantTreeTest} for testing public methods that
 * are implemented in base class.
 */
class GitTestResourceVariantTree extends GitResourceVariantTree {

	GitTestResourceVariantTree(GitSynchronizeDataSet data,
			GitSyncCache cache, ResourceVariantByteStore store) {
		super(store, cache, data);
	}

	@Override
	protected ObjectId getObjectId(ThreeWayDiffEntry diffEntry) {
		return null;
	}

	@Override
	protected RevCommit getCommitId(GitSynchronizeData gsd) {
		return null;
	}

}
