/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import org.eclipse.egit.gitflow.op.FeatureCheckoutOperationTest;
import org.eclipse.egit.gitflow.op.FeatureFinishOperationTest;
import org.eclipse.egit.gitflow.op.HotfixStartOperationTest;
import org.eclipse.egit.gitflow.op.FeatureListOperationTest;
import org.eclipse.egit.gitflow.op.CurrentBranchPublishOperationTest;
import org.eclipse.egit.gitflow.op.FeatureRebaseOperationTest;
import org.eclipse.egit.gitflow.op.FeatureStartOperationTest;
import org.eclipse.egit.gitflow.op.FeatureTrackOperationTest;
import org.eclipse.egit.gitflow.op.HotfixFinishOperationTest;
import org.eclipse.egit.gitflow.op.InitOperationTest;
import org.eclipse.egit.gitflow.op.ReleaseFinishOperationTest;
import org.eclipse.egit.gitflow.op.ReleaseStartOperationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ InitOperationTest.class, FeatureStartOperationTest.class,
		FeatureFinishOperationTest.class, ReleaseStartOperationTest.class,
		ReleaseFinishOperationTest.class, CurrentBranchPublishOperationTest.class,
		BranchNameValidatorTest.class, FeatureTrackOperationTest.class,
		FeatureListOperationTest.class, FeatureCheckoutOperationTest.class,
		FeatureRebaseOperationTest.class, HotfixStartOperationTest.class,
		HotfixFinishOperationTest.class, GitFlowRepositoryTest.class })
public class AllTests {
	// test suite
}
