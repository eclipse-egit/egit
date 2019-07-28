/*******************************************************************************
 * Copyright (C) 2019, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.gitflow;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellIsActive;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the Team->Gitflow->Feature Start action, when a conflict occurs on
 * checkout, after the branch has been created.
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class FeatureStartCheckoutConflictTest
		extends AbstractGitflowHandlerTest {

	@Test
	public void testFeatureStart() throws Exception {
		init();

		createFeature(FEATURE_NAME);
		checkoutBranch(DEVELOP);
		setContentAddAndCommit("foo");
		checkoutFeature(FEATURE_NAME);
		setContentAndStage("bar");
		setTestFileContent("fnord");
		createFeatureUi("myOtherFeature");

		bot.waitUntil(shellIsActive(UIText.BranchResultDialog_CheckoutConflictsTitle));
		bot.button(UIText.BranchResultDialog_buttonDiscardChanges).click();
	}
}
