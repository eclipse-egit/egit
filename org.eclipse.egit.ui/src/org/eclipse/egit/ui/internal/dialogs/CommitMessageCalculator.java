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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;

class CommitMessageCalculator {

	/**
	 * Constant for the extension point for the commit message provider
	 */
	private static final String COMMIT_MESSAGE_PROVIDER_ID = "org.eclipse.egit.ui.commitMessageProvider"; //$NON-NLS-1$

	private final Collection<String> filesToCommit;

	private final List<ICommitMessageProvider> commitMessageProviders;

	private Repository repository;

	/**
	 * @param repository
	 * @param filesToCommit
	 */
	public CommitMessageCalculator(Repository repository,
			Collection<String> filesToCommit) {
		this.repository = repository;
		this.filesToCommit = filesToCommit;
		this.commitMessageProviders = getCommitMessageProviders();
	}

	/**
	 * @return the calculated commit message
	 */
	String calculateCommitMessage() {
		StringBuilder calculatedCommitMessage = new StringBuilder();

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
		IResource[] resourcesArray = resources.toArray(new IResource[0]);
		String providedMessageSeparator = "\n\n"; //$NON-NLS-1$

		for (ICommitMessageProvider messageProvider : commitMessageProviders) {
			String message = null;
			try {
				message = messageProvider.getMessage(resourcesArray);
			} catch (RuntimeException e) {
				Activator.logError(e.getMessage(), e);
			}

			if (message != null && !message.trim().isEmpty()) {
				if (calculatedCommitMessage.length() > 0) {
					calculatedCommitMessage.append(providedMessageSeparator);
				}
				calculatedCommitMessage.append((message.trim()));
			}
		}

		return calculatedCommitMessage.toString();
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
