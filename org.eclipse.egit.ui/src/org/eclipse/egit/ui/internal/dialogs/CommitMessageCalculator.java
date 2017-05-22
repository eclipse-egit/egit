package org.eclipse.egit.ui.internal.dialogs;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	private Map<ICommitMessageProvider, String> providerToCommitMessageMap;

	private ICommitMessageProvider2 commitMessageProviderWithCursorPosition;

	/**
	 * @param repository
	 * @param filesToCommit
	 */
	public CommitMessageCalculator(Repository repository,
			Collection<String> filesToCommit) {
		this.repository = repository;
		this.resourcesArray = convertToRessourceArray(filesToCommit);
		List<ICommitMessageProvider> messageProviders = getCommitMessageProviders();
		this.providerToCommitMessageMap = mapMessagesToProviders(messageProviders);
		findCommitMessageProviderWithCursorPositioning(
				this.providerToCommitMessageMap);
	}

	/**
	 * @return the calculated commit message
	 */
	String calculateCommitMessage() {
		StringBuilder calculatedCommitMessage = new StringBuilder();

		for (Map.Entry<ICommitMessageProvider, String> entry : providerToCommitMessageMap
				.entrySet()) {
			// providerToCommitMessageMap.forEach((provider, message) -> {
			String message = entry.getValue();
			if (calculatedCommitMessage.length() > 0) {
				calculatedCommitMessage.append(MESSAGE_SEPARATOR);
			}
			calculatedCommitMessage.append((message));
		}

		return calculatedCommitMessage.toString();
	}

	/**
	 *
	 * @return the caret position within the calculated commit message or -1, if
	 *         no implementation of {@link ICommitMessageProvider2} was found.
	 */
	int calculateCaretPosition() {
		if (commitMessageProviderWithCursorPosition == null) {
			return -1;
		}

		int position = 0;
		for (Map.Entry<ICommitMessageProvider, String> entry : providerToCommitMessageMap
				.entrySet()) {
			ICommitMessageProvider provider = entry.getKey();

			if (provider == commitMessageProviderWithCursorPosition) {
				position += ((ICommitMessageProvider2) provider)
						.getCaretPosition();
				return position;
			} else {
				position += entry.getValue().length();
				position += MESSAGE_SEPARATOR.length();
			}
		}

		// if the return statement above wasn't executed, return the default
		// caret position
		return 0;
	}

	private Map<ICommitMessageProvider, String> mapMessagesToProviders(
			List<ICommitMessageProvider> messageProviders) {
		// keep the order, in which the providers were discovered
		Map<ICommitMessageProvider, String> providerMessageMap = new LinkedHashMap<>();

		for (ICommitMessageProvider messageProvider : messageProviders) {
			String message = getMessage(messageProvider);
			if (!message.isEmpty()) {
				providerMessageMap.put(messageProvider, message);
			}
		}
		return providerMessageMap;
	}

	private void findCommitMessageProviderWithCursorPositioning(
			Map<ICommitMessageProvider, String> providerToMessageMap) {
		providerToMessageMap.forEach((messageProvider, msg) -> {
			if (messageProvider instanceof ICommitMessageProvider2) {
				if (commitMessageProviderWithCursorPosition == null) {
					this.commitMessageProviderWithCursorPosition = (ICommitMessageProvider2) messageProvider;
				} else {
					Activator.logError(
							MessageFormat.format(
									UIText.CommitDialog_MultipleCommitMessageProvider2Implementations,
									messageProvider.getClass().getName(),
									commitMessageProviderWithCursorPosition
											.getClass().getName()),
							null);
				}
			}
		});
	}

	private String getMessage(ICommitMessageProvider messageProvider) {
		String message = null;
		try {
			message = messageProvider.getMessage(resourcesArray);
		} catch (RuntimeException e) {
			Activator.logError(e.getMessage(), e);
		}

		return (message != null) ? message.trim() : ""; //$NON-NLS-1$
	}

	List<ICommitMessageProvider> getCommitMessageProviders() {
		List<ICommitMessageProvider> providers = new ArrayList<>();

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
		return providers;
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
