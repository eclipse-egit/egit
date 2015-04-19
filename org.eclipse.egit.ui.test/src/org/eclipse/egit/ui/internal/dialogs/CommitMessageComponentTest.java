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

import java.util.Collection;
import java.util.Collections;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.egit.ui.internal.dialogs.CommitMessageComponent.CommitStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommitMessageComponentTest {

	private Shell shell;
	private CommitMessageComponent component;
	private CommitMessageArea commitArea;

	@Before
	public void setUp() throws Exception {
		component = new CommitMessageComponent(
				(ICommitMessageComponentNotifications) null);
		shell = new Shell(Display.getDefault());
		shell.setLayout(new FillLayout());
		commitArea = new TestCommitMessageArea(shell, "", SWT.NONE);
		Text authorText = new Text(shell, SWT.NONE);
		Text committerText = new Text(shell, SWT.NONE);
		component.attachControls(commitArea, authorText, committerText);
		shell.open();

		String user = "user<user@egit>";
		component.setAuthor(user);
		component.setCommitter(user);
		component.updateUIFromState();
	}

	@After
	public void tearDown() throws Exception {
		if (shell != null) {
			shell.dispose();
		}
	}

	@Test
	public void commitFormat_simple() {
		commitArea.setText("Simple message");

		CommitStatus status = component.getStatus();
		assertEquals(IMessageProvider.NONE, status.getMessageType());
		assertEquals(null, status.getMessage());
	}

	@Test
	public void commitFormat_MultipleLines_ok() {
		commitArea.setText("Summary\n\nDetails");

		CommitStatus status = component.getStatus();
		assertEquals(IMessageProvider.NONE, status.getMessageType());
		assertEquals(null, status.getMessage());
	}

	@Test
	public void commitFormat_MultipleLines_notOk() {
		commitArea.setText("Summary\nDetails");

		CommitStatus status = component.getStatus();
		assertEquals(IMessageProvider.WARNING, status.getMessageType());
		assertEquals(UIText.CommitMessageComponent_MessageSecondLineNotEmpty,
				status.getMessage());
	}

	@Test
	public void commitFormat_MultipleLines_notOk2() {
		commitArea.setText("Summary\n \nDetails");

		CommitStatus status = component.getStatus();
		assertEquals(IMessageProvider.WARNING, status.getMessageType());
		assertEquals(UIText.CommitMessageComponent_MessageSecondLineNotEmpty,
				status.getMessage());
	}

	private static final class TestCommitMessageArea extends CommitMessageArea {
		private TestCommitMessageArea(Composite parent, String initialText,
				int styles) {
			super(parent, initialText, styles);
		}

		@Override
		protected CommitProposalProcessor getCommitProposalProcessor() {
			return new CommitProposalProcessor() {
				@Override
				protected Collection<String> computeMessageProposals() {
					return Collections.emptyList();
				}

				@Override
				protected Collection<String> computeFileNameProposals() {
					return Collections.emptyList();
				}
			};
		}
	}

}
