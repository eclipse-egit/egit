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
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.CommitMessageWithCaretPosition;
import org.eclipse.egit.ui.ICommitMessageProvider;
import org.eclipse.egit.ui.ICommitMessageProvider2;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;

class CommitMessageBuilder {

	/**
	 * Constant for the extension point for the commit message provider.
	 */
	private static final String COMMIT_MESSAGE_PROVIDER_ID = "org.eclipse.egit.ui.commitMessageProvider"; //$NON-NLS-1$

	private static final String MESSAGE_SEPARATOR = "\n\n"; //$NON-NLS-1$

	private final IResource[] resourcesArray;

	private boolean isMessageEmpty;

	/**
	 * Creates a CommitMessageBuilder for the specified repository and the
	 * files, that are about to be committed.
	 *
	 * @param repository
	 *            the message is built for
	 * @param paths
	 *            list of file paths, selected for the next commit
	 */
	CommitMessageBuilder(Repository repository,
			Collection<String> paths) {
		this.resourcesArray = toResourceArray(repository, paths);
	}

	/**
	 * Returns an object, containing the commit message and the caret position
	 * within that message.
	 *
	 * @return a commit message with caret position. The caret position is 0, if
	 *         there was no {@link ICommitMessageProvider2} providing a caret
	 *         position.
	 */
	CommitMessageWithCaretPosition build() {
		StringBuilder finalMessage = new StringBuilder();
		int caretPosition = CommitMessageWithCaretPosition.NO_POSITION;
		isMessageEmpty = true;

		for (ICommitMessageProvider provider : getCommitMessageProviders()) {
			String message = ""; //$NON-NLS-1$
			try {
				if (provider instanceof ICommitMessageProvider2) {
					CommitMessageWithCaretPosition commitMessageWithPosition = ((ICommitMessageProvider2) provider)
							.getCommitMessageWithPosition(resourcesArray);
					if (commitMessageWithPosition != null) {
						caretPosition = updateCaretPosition(finalMessage,
								caretPosition, commitMessageWithPosition,
								(ICommitMessageProvider2) provider);
					}
					message = getCommitMessage(commitMessageWithPosition);
				} else {
					message = append(
							provider.getMessage(resourcesArray));
				}
			} catch (RuntimeException e) {
				Activator.logError(e.getMessage(), e);
			}
			finalMessage.append(message);
			isMessageEmpty = finalMessage.length() == 0;
		}
		return new CommitMessageWithCaretPosition(finalMessage.toString(),
				Math.max(0, caretPosition));
	}

	private String getCommitMessage(
			CommitMessageWithCaretPosition messageWithPosition) {
		if (messageWithPosition == null) {
			return ""; //$NON-NLS-1$
		} else {
			return append(messageWithPosition.getMessage());
		}
	}

	private String append(String msg) {
		StringBuilder returnMsg = new StringBuilder();
		if (msg != null && !msg.trim().isEmpty()) {
			if (!isMessageEmpty) {
				returnMsg.append(MESSAGE_SEPARATOR);
			}
			returnMsg.append(msg);
		}
		return returnMsg.toString();
	}

	@SuppressWarnings("boxing")
	private int updateCaretPosition(StringBuilder currentMessage,
			int currentCaretPosition,
			CommitMessageWithCaretPosition messageWithPosition,
			ICommitMessageProvider2 provider) {
		int pos = currentCaretPosition;
		if (currentCaretPosition == CommitMessageWithCaretPosition.NO_POSITION) {
			String providedMessage = messageWithPosition.getMessage();
			if (providedMessage == null || providedMessage.trim().isEmpty()) {
				return pos;
			}
			int providedCaretPosition = messageWithPosition
					.getDesiredCaretPosition();
			if (providedCaretPosition == CommitMessageWithCaretPosition.NO_POSITION) {
				return pos;
			}
			if (providedCaretPosition > providedMessage.length()
					|| providedCaretPosition < 0) {
				Activator.logWarning(
						MessageFormat.format(
								UIText.CommitDialog_CaretPositionOutOfBounds,
								provider.getClass().getName(),
								providedCaretPosition),
						null);
				return CommitMessageWithCaretPosition.NO_POSITION;

			} else {
				pos = currentMessage.length();
				if (currentMessage.length() > 0) {
					pos += MESSAGE_SEPARATOR.length();
				}
				pos += providedCaretPosition;
			}
		} else {
			Activator
					.logWarning(
							MessageFormat.format(
									UIText.CommitDialog_IgnoreCaretPosition,
									provider.getClass().getName()),
							null);
		}

		return pos;
	}

	List<ICommitMessageProvider> getCommitMessageProviders() {
		List<ICommitMessageProvider> providers = new ArrayList<>();

		IConfigurationElement[] configs = Platform.getExtensionRegistry()
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
		return providers;
	}

	private IResource[] toResourceArray(Repository repository,
			Collection<String> paths) {
		if (repository == null || repository.isBare()) {
			return new IResource[0];
		}
		Set<IResource> resources = new HashSet<>();
		for (String path : paths) {
			IFile file = null;
			if (path != null) {
				file = ResourceUtil.getFileForLocation(repository, path,
						false);
			}
			if (file != null) {
				resources.add(file.getProject());
			}
		}
		if (resources.size() == 0) {
			resources
					.addAll(Arrays
							.asList(ProjectUtil.getProjects(repository)));
		}
		return resources.toArray(new IResource[0]);
	}

}
