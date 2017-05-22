/*******************************************************************************
 * Copyright (C) 2017, Stefan Rademacher <stefan.rademacher@tk.de>
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.ICommitMessageProvider2;
import org.eclipse.egit.ui.ICommitMessageProvider2.CommitMessageWithCaretPosition;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;

class CommitMessageCalculator {

	/**
	 * Constant for the extension point for the commit message provider
	 */
	private static final String COMMIT_MESSAGE_PROVIDER_ID = "org.eclipse.egit.ui.commitMessageProvider"; //$NON-NLS-1$

	private static final String MESSAGE_SEPARATOR = "\n\n"; //$NON-NLS-1$

	private final IResource[] resourcesArray;

	private Repository repository;

	private List<ICommitMessageProvider> providers;

	private boolean isCaretPositionSet = false;

	/**
	 * Creates a CommitMessageCalculator for the specified repository and the
	 * files, that are about to be committed.
	 *
	 * @param repository
	 *            the repository, messages are calculated for
	 * @param filesToCommit
	 *            list of files, selected for the next commit
	 */
	CommitMessageCalculator(Repository repository,
			Collection<String> filesToCommit) {
		this.repository = repository;
		this.resourcesArray = convertToRessourceArray(filesToCommit);
		this.providers = getCommitMessageProviders();
	}

	/**
	 * Returns an object, containing the calculated commit message and the caret
	 * position within that message.
	 *
	 * @return a commit message with caret position. if no implementation of
	 *         {@link ICommitMessageProvider2} was found.
	 */
	CommitMessageWithCaretPosition calculateCommitMessageAndCaretPosition() {
		StringBuilder finalMessage = new StringBuilder();
		int caretPosition = CommitMessageWithCaretPosition.DEFAULT_POSITION;

		for (ICommitMessageProvider provider : providers) {
			String messagePart = ""; //$NON-NLS-1$

			try {
				if (provider instanceof ICommitMessageProvider2) {
					CommitMessageWithCaretPosition commitMessageWithPosition = ((ICommitMessageProvider2) provider)
							.getCommitMessageWithPosition(resourcesArray);
					messagePart = getCommitMessage(
							commitMessageWithPosition,
							finalMessage.length() == 0);

					// update caret position
					if (commitMessageWithPosition != null) {
						caretPosition = getUpdatedCaretPosition(finalMessage,
								caretPosition, commitMessageWithPosition,
								(ICommitMessageProvider2) provider);
					}

				} else {
					messagePart = getCommitMessage(provider,
							finalMessage.length() == 0);
				}
			} catch (RuntimeException e) {
				Activator.logError(e.getMessage(), e);
			}

			finalMessage.append(messagePart);
		}

		if (isCaretPositionSet) {
			return new CommitMessageWithCaretPosition(finalMessage.toString(),
					caretPosition);
		} else {
			return new CommitMessageWithCaretPosition(finalMessage.toString(),
					CommitMessageWithCaretPosition.DEFAULT_POSITION);
		}
	}

	private String getCommitMessage(
			CommitMessageWithCaretPosition messageWithPosition,
			boolean isFirstMessage) {
		if (messageWithPosition == null) {
			return ""; //$NON-NLS-1$
		} else {
			return buildCommitMessage(messageWithPosition.getMessage(),
					isFirstMessage);
		}
	}

	private String getCommitMessage(ICommitMessageProvider provider,
			boolean isFirstMessage) {
		return buildCommitMessage(provider.getMessage(resourcesArray),
				isFirstMessage);
	}

	private String buildCommitMessage(String msg, boolean isFirstMessage) {
		StringBuilder returnMsg = new StringBuilder();

		if (msg != null && !msg.trim().isEmpty()) {
			if (!isFirstMessage) {
				returnMsg.append(MESSAGE_SEPARATOR);
			}
			returnMsg.append(msg);
		}

		return returnMsg.toString();
	}

	private int getUpdatedCaretPosition(StringBuilder currentCalculatedMessage,
			int currentCaretPosition,
			CommitMessageWithCaretPosition messageWithPosition,
			ICommitMessageProvider2 provider) {
		int pos = currentCaretPosition;

		if (!isCaretPositionSet) {
			pos += currentCalculatedMessage.length();
			if (currentCalculatedMessage.length() > 0) {
				pos += MESSAGE_SEPARATOR.length();
			}
			pos += messageWithPosition.getDesiredCaretPosition();
			isCaretPositionSet = true;
		} else {
			Activator
					.logWarning(
							MessageFormat.format(
									UIText.CommitDialog_MultipleCommitMessageProvider2Implementations,
									provider.getClass().getName()),
							null);
		}

		return pos;
	}

	List<ICommitMessageProvider> getCommitMessageProviders() {
		List<ICommitMessageProvider> foundProviders = new ArrayList<>();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] configs = registry
				.getConfigurationElementsFor(COMMIT_MESSAGE_PROVIDER_ID);
		for (IConfigurationElement config : configs) {
			Object provider;
			String contributorName = "<unknown>"; //$NON-NLS-1$
			String extensionId = "<unknown>"; //$NON-NLS-1$
			try {
				extensionId = config.getDeclaringExtension()
						.getUniqueIdentifier();
				contributorName = config.getContributor().getName();
				provider = config.createExecutableExtension("class");//$NON-NLS-1$
				if (provider instanceof ICommitMessageProvider) {
					providers.add((ICommitMessageProvider) provider);
				} else {
					Activator.logError(
							MessageFormat.format(
									UIText.CommitDialog_WrongTypeOfCommitMessageProvider,
									extensionId, contributorName),
							null);
				}
			} catch (CoreException | RuntimeException e) {
				Activator
						.logError(
								MessageFormat.format(
										UIText.CommitDialog_ErrorCreatingCommitMessageProvider,
										extensionId, contributorName),
								e);
			}
		}
		return foundProviders;
	}

	private IResource[] convertToRessourceArray(
			Collection<String> filesToCommit) {

		Set<IResource> resources = new HashSet<>();
		for (String path : filesToCommit) {
			IFile file = ResourceUtil.getFileForLocation(repository, path,
					false);
			if (file != null)
				resources.add(file.getProject());
		}
		if (resources.size() == 0 && repository != null) {
			resources
					.addAll(Arrays.asList(ProjectUtil.getProjects(repository)));
		}

		return resources.toArray(new IResource[0]);
	}

}
