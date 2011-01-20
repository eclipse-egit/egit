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
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;

class GitBaseResourceVariantTree extends GitResourceVariantTree {

	GitBaseResourceVariantTree(GitSynchronizeDataSet gsds) {
		super(new SessionResourceVariantByteStore(), gsds);
	}

	@Override
	protected RevCommit getRevCommit(GitSynchronizeData gsd)
			throws TeamException {
		return gsd.getCommonAncestorRev();
	}

}
