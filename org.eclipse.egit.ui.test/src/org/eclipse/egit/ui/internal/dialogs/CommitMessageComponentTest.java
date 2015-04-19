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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.Test;

public class CommitMessageComponentTest {

	@Test
	public void commitFormat_simple() {
		IDocument document = new Document("Simple message");

		String message = CommitMessageComponent
				.formatIssuesInCommitMessage(document);
		assertEquals(null, message);
	}

	@Test
	public void commitFormat_trailingWhitespace_ok() {
		IDocument document = new Document("Simple message\n\n\n");

		String message = CommitMessageComponent
				.formatIssuesInCommitMessage(document);
		assertEquals(null, message);
	}

	@Test
	public void commitFormat_MultipleLines_ok() {
		IDocument document = new Document("Summary\n\nDetails");

		String message = CommitMessageComponent
				.formatIssuesInCommitMessage(document);
		assertEquals(null, message);
	}

	@Test
	public void commitFormat_MultipleLines_notOk() {
		IDocument document = new Document("Summary\nDetails");

		String message = CommitMessageComponent
				.formatIssuesInCommitMessage(document);
		assertEquals(UIText.CommitMessageComponent_MessageSecondLineNotEmpty,
				message);
	}

	@Test
	public void commitFormat_MultipleLines_notOk2() {
		IDocument document = new Document("Summary\n \nDetails");

		String message = CommitMessageComponent
				.formatIssuesInCommitMessage(document);
		assertEquals(UIText.CommitMessageComponent_MessageSecondLineNotEmpty,
				message);
	}

}
