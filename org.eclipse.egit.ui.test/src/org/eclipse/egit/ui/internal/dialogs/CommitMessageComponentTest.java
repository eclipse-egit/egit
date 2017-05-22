/*******************************************************************************
 * Copyright (C) 2015 SAP SE (Christian Georgi <christian.georgi@sap.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.assertEquals;

import org.eclipse.egit.ui.internal.UIText;
import org.junit.Test;

public class CommitMessageComponentTest {

	@Test
	public void commitFormat_simple() {
		String commitMessage = "Simple message";

		String formattedMessage = CommitMessageComponent
				.formatIssuesInCommitMessage(commitMessage);
		assertEquals(null, formattedMessage);
	}

	@Test
	public void commitFormat_trailingWhitespace_ok() {
		String commitMessage = "Simple message\n\n\n";

		String formattedMessage = CommitMessageComponent
				.formatIssuesInCommitMessage(commitMessage);
		assertEquals(null, formattedMessage);
	}

	@Test
	public void commitFormat_MultipleLines_ok() {
		String commitMessage = "Simple message\n\nDetails";

		String formattedMessage = CommitMessageComponent
				.formatIssuesInCommitMessage(commitMessage);
		assertEquals(null, formattedMessage);
	}

	@Test
	public void commitFormat_MultipleLines_notOk() {
		String commitMessage = "Simple message\nDetails";

		String formattedMessage = CommitMessageComponent
				.formatIssuesInCommitMessage(commitMessage);
		assertEquals(UIText.CommitMessageComponent_MessageSecondLineNotEmpty,
				formattedMessage);
	}

	@Test
	public void commitFormat_MultipleLines_notOk2() {
		String commitMessage = "Simple message\n \nDetails";

		String formattedMessage = CommitMessageComponent
				.formatIssuesInCommitMessage(commitMessage);
		assertEquals(UIText.CommitMessageComponent_MessageSecondLineNotEmpty,
				formattedMessage);
	}

}
