/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

/**
 * Implementation of abstract {@link GitResourceVariantTree} class. It is only
 * used in {@link GitResourceVariantTreeTest} for testing public methods that
 * are implemented in base class.
 */
class GitTestResourceVariantTree extends GitResourceVariantTree {

	GitTestResourceVariantTree(GitSynchronizeDataSet data,
			ResourceVariantByteStore store) {
		super(data, store);
	}

	@Override
	ObjectId getRevObjId(IResource resource) throws IOException {
		// not used in test case
		return null;
	}

	@Override
	Tree getRevTree(IResource resource) throws IOException {
		// TODO not used in test case
		return null;
	}

}
