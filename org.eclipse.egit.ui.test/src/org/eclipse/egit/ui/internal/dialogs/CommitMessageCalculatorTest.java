/*******************************************************************************
 * Copyright (C) 2015 SAP SE (Christian Georgi <christian.georgi@sap.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.ICommitMessageProvider2;
import org.eclipse.egit.ui.ICommitMessageProvider2.CommitMessageWithCaretPosition;
import org.eclipse.egit.ui.internal.UIText;
import org.junit.Test;

public class CommitMessageCalculatorTest {

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

	@Test
	public void commitMessageProvider_noProvider() throws Exception {
		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				createProviderList());

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals("", commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_oneProvider() throws Exception {
		String message = "example single-line commit message";

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				createProviderList(message));

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(message, commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_twoProviders() throws Exception {
		String message1 = "example single-line commit message";
		String message2 = "example multi-line\n\ncommit message";

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				createProviderList(message1, message2));

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(message1 + "\n\n" + message2,
				commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_oneCrashingProvider() throws Exception {

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				Arrays.asList(new CrashingCommitMessageProvider()));

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals("", commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_twoProvidersSecondOneCrashing()
			throws Exception {
		String message = "example single-line commit message";
		List<ICommitMessageProvider> providers = createProviderList(message);
		providers.add(new CrashingCommitMessageProvider());

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(message, commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_twoProvidersFirstOneCrashing()
			throws Exception {
		String message = "example single-line commit message";
		List<ICommitMessageProvider> providers = createProviderList(message);
		providers.add(0, new CrashingCommitMessageProvider());

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(message, commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_multipleProvidersWithCrashAndNull()
			throws Exception {
		String message1 = "\nexample commit message";
		String multiLineMessage = "example\nmulti-line\n\ncommit message\n\n\n";
		List<ICommitMessageProvider> providers = createProviderList(
				multiLineMessage, null, message1);
		providers.add(0, new CrashingCommitMessageProvider());
		providers.add(3, new CrashingCommitMessageProvider());

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(multiLineMessage + "\n\n" + message1,
				commitMessageProvider.getMessage());
	}

	@Test
	public void commitMessageProvider_oneProviderWithCaretPositioning() {
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = 13;

		ICommitMessageProvider2 providerWithCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = new ArrayList<>();
		providers.add(providerWithCaretPositioning);

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);
		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(caretPositionInMultiLineMessage,
				commitMessageProvider.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersFirstWithCaretPositioning() {
		String singleLineMessage = "Descr.: ";
		int caretPositionInSingleLineMessage = 8;
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";

		ICommitMessageProvider2 firstProviderWithCaretPositioning = createProviderWithCaretPositioning(
				singleLineMessage, caretPositionInSingleLineMessage);

		List<ICommitMessageProvider> providers = createProviderList(
				multiLineMessage);
		providers.add(0, firstProviderWithCaretPositioning);

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);
		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(caretPositionInSingleLineMessage,
				commitMessageProvider.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersSecondWithCaretPositioning() {
		String singleLineMessage = "Descr.: ";
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = 13;

		ICommitMessageProvider2 secondProviderWithCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = createProviderList(
				singleLineMessage);
		providers.add(secondProviderWithCaretPositioning);

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);
		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(
				singleLineMessage.length() + "\n\n".length()
				+ caretPositionInMultiLineMessage,
				commitMessageProvider.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersWithCaretPositioning() {
		String singleLineMessage = "Descr.: ";
		int caretPositionInSingleLineMessage = 8;
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = 13;

		ICommitMessageProvider2 firstProviderWithCaretPositioning = createProviderWithCaretPositioning(
				singleLineMessage, caretPositionInSingleLineMessage);
		ICommitMessageProvider2 secondProviderWithCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = new ArrayList<>();
		providers.add(firstProviderWithCaretPositioning);
		providers.add(secondProviderWithCaretPositioning);

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);
		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(caretPositionInSingleLineMessage,
				commitMessageProvider.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_multipleProvidersWithCrashingAndNullAndOneCaretPositioning() {
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = 13;
		String singleLineMessage = "example single-line commit message";

		ICommitMessageProvider2 providerWithCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = createProviderList(null,
				singleLineMessage);
		providers.add(0, new CrashingCommitMessageProvider());
		providers.add(0, providerWithCaretPositioning);
		providers.add(3, new CrashingCommitMessageProvider());

		CommitMessageCalculator commitMessageCalculator = newCommitMessageCalculator(
				providers);

		CommitMessageWithCaretPosition commitMessageProvider = commitMessageCalculator
				.calculateCommitMessageAndCaretPosition();

		assertEquals(multiLineMessage + "\n\n" + singleLineMessage,
				commitMessageProvider.getMessage());
	}

	private CommitMessageCalculator newCommitMessageCalculator(
			List<ICommitMessageProvider> providers) {
		// Create anonymous subclass, as mocking does not currently work.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=349164
		return new CommitMessageCalculator(null, Collections.emptyList()) {

			@Override
			List<ICommitMessageProvider> getCommitMessageProviders() {
				return providers;
			}
		};
	}

	private List<ICommitMessageProvider> createProviderList(
			String... messages) {
		List<ICommitMessageProvider> providerList = new ArrayList<>();

		for (String message : messages) {
			providerList.add(new ICommitMessageProvider() {

				@Override
				public String getMessage(IResource[] resources) {
					return message;
				}
			});
		}

		return providerList;
	}

	private ICommitMessageProvider2 createProviderWithCaretPositioning(
			String message, int caretPosition) {
		return new ICommitMessageProvider2() {

			@Override
			public String getMessage(IResource[] resources) {
				return message;
			}

			@Override
			public CommitMessageWithCaretPosition getCommitMessageWithPosition(
					IResource[] resources) {
				return new CommitMessageWithCaretPosition(message,
						caretPosition);
			}
		};
	}

	private static class CrashingCommitMessageProvider
			implements ICommitMessageProvider {

		@Override
		public String getMessage(IResource[] resources) {
			throw new IllegalStateException(
					"CrashingCommitMessageProvider fails on purpose.");
		}

	}

}
