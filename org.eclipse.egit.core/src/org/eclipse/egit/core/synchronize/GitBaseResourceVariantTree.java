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

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

class GitBaseResourceVariantTree extends GitResourceVariantTree {

	GitBaseResourceVariantTree(GitSynchronizeDataSet data, ResourceVariantByteStore store) {
		super(data, store);
	}

	@Override
	Tree getRevTree(IResource resource) throws IOException {
		return getSyncData().getData(resource.getProject()).mapSrcTree();
	}

	@Override
	ObjectId getRevObjId(IResource resource) throws IOException {
		return getSyncData().getData(resource.getProject()).getSrcObjectId();
	}

}
