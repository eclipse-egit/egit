/*******************************************************************************
 * Copyright (C) 2025 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test.stagview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.common.StagingViewTester;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.test.TestUtil;
import org.eclipse.egit.ui.test.commit.ShowCommitMessageHandler;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.junit.Test;

public class CommitToolbarTest extends AbstractStagingViewTestCase {

	@Test
	public void testCommitMessageContribution() throws Exception {
		setContent("something");

		StagingViewTester stagingViewTester = StagingViewTester
				.openStagingView();
		stagingViewTester.setAuthor(TestUtil.TESTAUTHOR);
		stagingViewTester.setCommitter(TestUtil.TESTCOMMITTER);
		stagingViewTester.setCommitMessage("Commit message");
		stagingViewTester.setSignedOff(true);
		String commitMessage = stagingViewTester.getCommitMessage();
		assertTrue("Should have a signed-off footer",
				commitMessage.indexOf("Signed-off-by") > 0);
		SWTBotView view = stagingViewTester.getView();
		view.bot().toolbarButtonWithTooltip("Test Commit Message Contribution")
				.click();
		assertNotNull(ShowCommitMessageHandler.lastData);
		Repository repo = ShowCommitMessageHandler.lastData.repository();
		assertNotNull("Repository should have been found", repo);
		assertEquals("Unexpected repository", repo.getDirectory(),
				repository.getDirectory());
		assertEquals("Unexpected commit message",
				"Commit message\n\nSigned-off-by: " + TestUtil.TESTCOMMITTER,
				ShowCommitMessageHandler.lastData.message());
		// Attention: just calling stagingViewTester.getCommitMessage() uses
		// SWTUtils.getText(), which replaces all Text.DELIMITERs by "\n"! So
		// get the text ourselves, without substitution, since we want to test
		// that the line endings _are_ Text.DELIMITER.
		StyledText widget = view.bot()
				.styledTextWithLabel(UIText.StagingView_CommitMessage).widget;
		String newMessage = UIThreadRunnable.syncExec(widget.getDisplay(),
				() -> widget.getText());
		assertEquals("Commit message should have been changed",
				ShowCommitMessageHandler.NEW_MESSAGE.replaceAll("\n",
						Text.DELIMITER),
				newMessage);
		// The new message that was set has no signed-off-by line, so the sign
		// off button should be unchecked.
		assertFalse("Sign off should be unchecked",
				stagingViewTester.getSignedOff());
	}

}
