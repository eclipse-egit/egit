package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.internal.util.ProjectUtil;
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
	 * @param repository
	 * @param filesToCommit
	 */
	public CommitMessageCalculator(Repository repository,
			Collection<String> filesToCommit) {
		this.repository = repository;
		this.resourcesArray = convertToRessourceArray(filesToCommit);
		this.providers = getCommitMessageProviders();
	}

	/**
	 *
	 * @return the caret position within the calculated commit message or -1, if
	 *         no implementation of {@link ICommitMessageProvider2} was found.
	 */
	CommitMessageWithCaretPosition calculateCommitMessageAndCaretPosition() {
		StringBuilder finalMessage = new StringBuilder();
		int caretPosition = CommitMessageWithCaretPosition.DEFAULT_POSITION;

		for (ICommitMessageProvider provider : providers) {
			String messagePart = ""; //$NON-NLS-1$

			try {
				if (provider instanceof ICommitMessageProvider2) {
					messagePart = getCommitMessage(
							(ICommitMessageProvider2) provider,
							finalMessage.length() == 0);
				} else {
					messagePart = getCommitMessage(provider,
							finalMessage.length() == 0);
				}

				// update caret position
				caretPosition = getUpdatedCaretPosition(finalMessage,
						caretPosition, provider);
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

	private String getCommitMessage(ICommitMessageProvider2 provider,
			boolean isFirstMessage) {
		CommitMessageWithCaretPosition c = provider
				.getCommitMessageWithPosition(resourcesArray);

		if (c == null) {
			return ""; //$NON-NLS-1$
		} else {
			return _buildCommitMessage(c.getMessage(), isFirstMessage);
		}
	}

	private String getCommitMessage(ICommitMessageProvider provider,
			boolean isFirstMessage) {
		return _buildCommitMessage(provider.getMessage(resourcesArray),
				isFirstMessage);
	}

	private String _buildCommitMessage(String msg, boolean isFirstMessage) {
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
			ICommitMessageProvider provider) {
		int pos = currentCaretPosition;

		if (!isCaretPositionSet) {
			if (provider instanceof ICommitMessageProvider2) {
				pos += currentCalculatedMessage.length();
				if (currentCalculatedMessage.length() > 0)
					pos += MESSAGE_SEPARATOR.length();
				pos += getCaretPosition((ICommitMessageProvider2) provider);
				isCaretPositionSet = true;
			}
		} else {
			if (provider instanceof ICommitMessageProvider2) {
				Activator
						.logError(
								MessageFormat.format(
										UIText.CommitDialog_MultipleCommitMessageProvider2Implementations,
										provider.getClass().getName()),
								null);
			}
		}

		return pos;
	}

	private int getCaretPosition(ICommitMessageProvider2 provider) {
		CommitMessageWithCaretPosition c = provider
				.getCommitMessageWithPosition(resourcesArray);

		if (c == null) {
			return CommitMessageWithCaretPosition.DEFAULT_POSITION;
		} else {
			return c.getDesiredCaretPosition();
		}
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
			IFile file = findFile(path);
			if (file != null)
				resources.add(file.getProject());
		}
		if (resources.size() == 0 && repository != null) {
			resources
					.addAll(Arrays.asList(ProjectUtil.getProjects(repository)));
		}

		return resources.toArray(new IResource[0]);
	}

	// TODO: move to utils
	private IFile findFile(String path) {
		URI uri = new File(repository.getWorkTree(), path).toURI();
		IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot()
				.findFilesForLocationURI(uri);
		if (workspaceFiles.length > 0)
			return workspaceFiles[0];
		else
			return null;
	}

}
