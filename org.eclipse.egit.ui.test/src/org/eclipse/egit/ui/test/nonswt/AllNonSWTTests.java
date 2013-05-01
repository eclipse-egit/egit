/*******************************************************************************
 * Copyright (C) 2011, Philipp Thun <philipp.thun@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.nonswt;

import org.eclipse.egit.ui.internal.actions.LinkedResourcesTest;
import org.eclipse.egit.ui.internal.decorators.DecoratableResourceAdapterTest;
import org.eclipse.egit.ui.internal.synchronize.model.AllGitModelTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DecoratableResourceAdapterTest.class,
		LinkedResourcesTest.class,
		AllGitModelTests.class })
public class AllNonSWTTests {
	// Empty class
}
