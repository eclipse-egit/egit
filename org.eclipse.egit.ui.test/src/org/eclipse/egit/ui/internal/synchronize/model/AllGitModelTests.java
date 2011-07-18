/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ GitModelBlobTest.class,
		GitModelCacheFileTest.class,
		GitModelCacheTest.class,
		GitModelCacheTreeTest.class,
		GitModelCommitTest.class,
		GitModelRepositoryTest.class,
		GitModelTreeTest.class,
		GitModelWorkingTreeTest.class,
		GitModelWorkingFileTest.class,
		GitModelRootTest.class })
public class AllGitModelTests {
	// empty class, don't need anything here
}
