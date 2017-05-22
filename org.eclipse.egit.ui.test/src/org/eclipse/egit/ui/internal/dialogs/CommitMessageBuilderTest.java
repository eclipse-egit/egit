/*******************************************************************************
 * Copyright (C) 2017, Stefan Rademacher <stefan.rademacher@tk.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.test.GitTestCase;
import org.eclipse.egit.core.test.TestRepository;
import org.eclipse.egit.ui.CommitMessageWithCaretPosition;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.ICommitMessageProvider2;
import org.eclipse.jgit.lib.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommitMessageBuilderTest extends GitTestCase {

	TestRepository testRepository;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		gitDir = new File(project.getProject().getLocationURI().getPath(),
				Constants.DOT_GIT);
		testRepository = new TestRepository(gitDir);
		testRepository.connect(project.getProject());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		testRepository.dispose();
		super.tearDown();
	}

	@Test
	public void commitMessageProvider_noProvider() throws Exception {
		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				createProviderList());

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals("", commitMessageWithPosition.getMessage());
	}

	@Test
	public void commitMessageProvider_oneProvider() throws Exception {
		String message = "example single-line commit message";

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				createProviderList(message));

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(message, commitMessageWithPosition.getMessage());
	}

	@Test
	public void commitMessageProvider_twoProviders() throws Exception {
		String message1 = "example single-line commit message";
		String message2 = "example multi-line\n\ncommit message";

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				createProviderList(message1, message2));

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(message1 + "\n\n" + message2,
				commitMessageWithPosition.getMessage());
	}

	@Test
	public void commitMessageProvider_oneCrashingProvider() throws Exception {

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				Arrays.asList(new CrashingCommitMessageProvider()));

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals("", commitMessageWithPosition.getMessage());
	}

	@Test
	public void commitMessageProvider_oneCrashingProviderWithCaretPosition() {

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				Arrays.asList(
						new CrashingCommitMessageProviderWithCaretPositioning()));

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals("", commitMessageWithPosition.getMessage());
		assertEquals(CommitMessageComponentState.CARET_DEFAULT_POSITION,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersSecondOneCrashing()
			throws Exception {
		String message = "example single-line commit message";
		List<ICommitMessageProvider> providers = createProviderList(message);
		providers.add(new CrashingCommitMessageProvider());

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(message, commitMessageWithPosition.getMessage());
	}

	@Test
	public void commitMessageProvider_twoProvidersFirstOneCrashing()
			throws Exception {
		String message = "example single-line commit message";
		List<ICommitMessageProvider> providers = createProviderList(message);
		providers.add(0, new CrashingCommitMessageProvider());

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(message, commitMessageWithPosition.getMessage());
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

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(multiLineMessage + "\n\n" + message1,
				commitMessageWithPosition.getMessage());
	}

	@Test
	public void commitMessageProvider_oneProviderWithCaretPositioning() {
		String message = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPosition = 13;
		CommitMessageWithCaretPosition commitMessageWithPosition = getCommitMessageWithCaretPosition(
				message, caretPosition);

		assertEquals(caretPosition,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_oneProviderWithCaretPositionEqualsMessageLength() {
		String message = "Description: ";
		int caretPosition = message.length();
		CommitMessageWithCaretPosition commitMessageWithPosition = getCommitMessageWithCaretPosition(
				message, caretPosition);

		assertEquals(caretPosition,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_oneProviderWithInvalidNegativeCaretPosition() {
		String message = "Description: ";
		CommitMessageWithCaretPosition commitMessageWithPosition = getCommitMessageWithCaretPosition(
				message, -42);

		assertEquals(0,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_oneProviderWithInvalidCaretPositionExceedingMessageLength() {
		String message = "Description: ";
		CommitMessageWithCaretPosition commitMessageWithPosition = getCommitMessageWithCaretPosition(
				message, message.length() + 1);

		assertEquals(0, commitMessageWithPosition.getDesiredCaretPosition());
	}

	private CommitMessageWithCaretPosition getCommitMessageWithCaretPosition(
			String message, int caretPosition) {
		ICommitMessageProvider2 providerWithCaretPositioning = createProviderWithCaretPositioning(
				message, caretPosition);

		List<ICommitMessageProvider> providers = new ArrayList<>();
		providers.add(providerWithCaretPositioning);

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		return commitMessageBuilder
				.build();
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

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(caretPositionInSingleLineMessage,
				commitMessageWithPosition.getDesiredCaretPosition());
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

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(
				singleLineMessage.length() + "\n\n".length()
				+ caretPositionInMultiLineMessage,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersSecondWithCaretPositionZero() {
		String singleLineMessage = "Descr.: ";
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = 0;

		ICommitMessageProvider2 secondProviderWithUndefinedCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = createProviderList(
				singleLineMessage);
		providers.add(secondProviderWithUndefinedCaretPositioning);

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(
				singleLineMessage.length() + "\n\n".length()
						+ caretPositionInMultiLineMessage,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersSecondWithUndefinedCaretPosition() {
		String singleLineMessage = "Descr.: ";
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = CommitMessageWithCaretPosition.NO_POSITION;

		ICommitMessageProvider2 secondProviderWithUndefinedCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = createProviderList(
				singleLineMessage);
		providers.add(secondProviderWithUndefinedCaretPositioning);

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(CommitMessageComponentState.CARET_DEFAULT_POSITION,
				commitMessageWithPosition.getDesiredCaretPosition());
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

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(caretPositionInSingleLineMessage,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersWithCaretPositioningFirstWithUndefinedCaretPosition() {
		String singleLineMessage = "Descr.: ";
		int caretPositionInSingleLineMessage = CommitMessageWithCaretPosition.NO_POSITION;
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = 13;

		ICommitMessageProvider2 firstProviderWithCaretPositioning = createProviderWithCaretPositioning(
				singleLineMessage, caretPositionInSingleLineMessage);
		ICommitMessageProvider2 secondProviderWithCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = new ArrayList<>();
		providers.add(firstProviderWithCaretPositioning);
		providers.add(secondProviderWithCaretPositioning);

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(
				singleLineMessage.length() + "\n\n".length()
						+ caretPositionInMultiLineMessage,
				commitMessageWithPosition.getDesiredCaretPosition());
	}

	@Test
	public void commitMessageProvider_twoProvidersWithCaretPositioningSecondWithUndefinedCaretPosition() {
		String singleLineMessage = "Descr.: ";
		int caretPositionInSingleLineMessage = 8;
		String multiLineMessage = "Description: \n\nExample\nmulti-line\n\ncommit message";
		int caretPositionInMultiLineMessage = CommitMessageWithCaretPosition.NO_POSITION;

		ICommitMessageProvider2 firstProviderWithCaretPositioning = createProviderWithCaretPositioning(
				singleLineMessage, caretPositionInSingleLineMessage);
		ICommitMessageProvider2 secondProviderWithCaretPositioning = createProviderWithCaretPositioning(
				multiLineMessage, caretPositionInMultiLineMessage);

		List<ICommitMessageProvider> providers = new ArrayList<>();
		providers.add(firstProviderWithCaretPositioning);
		providers.add(secondProviderWithCaretPositioning);

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);
		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(caretPositionInSingleLineMessage,
				commitMessageWithPosition.getDesiredCaretPosition());
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

		CommitMessageBuilder commitMessageBuilder = newCommitMessageBuilder(
				providers);

		CommitMessageWithCaretPosition commitMessageWithPosition = commitMessageBuilder
				.build();

		assertEquals(multiLineMessage + "\n\n" + singleLineMessage,
				commitMessageWithPosition.getMessage());
	}

	private CommitMessageBuilder newCommitMessageBuilder(
			List<ICommitMessageProvider> providers) {
		// Create anonymous subclass, as mocking does not currently work.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=349164
		return new CommitMessageBuilder(testRepository.getRepository(),
				Collections.emptyList()) {

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

	private static class CrashingCommitMessageProviderWithCaretPositioning
			implements ICommitMessageProvider2 {

		@Override
		public String getMessage(IResource[] resources) {
			return "getMessage() is not supposed to be called.";
		}

		@Override
		public CommitMessageWithCaretPosition getCommitMessageWithPosition(
				IResource[] resources) {
			throw new IllegalStateException(
					"CrashingCommitMessageProviderWithCaretPositioning fails on purpose.");
		}

	}

}
