/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.test.team.actions;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BranchAndResetActionTest.class, //
		TagActionTest.class, //
		CommitActionTest.class, //
		PushActionTest.class, //
		FetchAndMergeActionTest.class, //
		DisconnectConnectTest.class, //
		ShowInTest.class, //
		CompareActionsTest.class, //
		ReplaceActionsTest.class, //
		CreatePatchActionTest.class, //
		MergeToolTest.class})
public class AllTeamActionTests {
	// nothing
}
