/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepository;
import org.junit.Before;

public abstract class AbstractCacheTest extends LocalDiskRepositoryTestCase {

	protected FileRepository db;

	protected static final String INITIAL_TAG = "initial-tag";

	protected static final AbbreviatedObjectId ZERO_ID = AbbreviatedObjectId.fromObjectId(ObjectId.zeroId());

	@Before
	@Override
	// copied from org.eclipse.jgit.lib.RepositoryTestCase
	public void setUp() throws Exception {
		super.setUp();
		db = createWorkRepository();
		Git git = new Git(db);
		git.commit().setMessage("initial commit").call();
		git.tag().setName(INITIAL_TAG).call();
	}
}
