/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.view.repositories;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { GitRepositoriesViewBranchHandlingTest.class,//
		GitRepositoriesViewRepoHandlingTest.class,//
		GitRepositoriesViewRemoteHandlingTest.class,//
		GitRepositoriesViewFetchAndPushTest.class,//
		GitRepositoriesViewTest.class //
})
public class AllRepositoriesViewTests {
	// suite, nothing else
}
