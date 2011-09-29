/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import org.eclipse.egit.ui.test.nonswt.AllNonSWTTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for running both the SWTBot tests and the Non-SWT Tests
 * on maven build.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ AllNonSWTTests.class, AllLocalTests.class })

public class AllSWTAndNonSWTTests {
	// empty class, don't need anything here
}
