/*******************************************************************************
 * Copyright (C) 2011, 2025 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.ProjectReference;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CloneOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.CreateLocalBranchOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * Processes project references, clones and imports them.
 */
public class ProjectReferenceImporter {

	private final String[] referenceStrings;

	/**
	 * @param referenceStrings the reference strings to import
	 */
	public ProjectReferenceImporter(String[] referenceStrings) {
		this.referenceStrings = referenceStrings;
	}

	/**
	 * Imports the projects as described in the reference strings.
	 *
	 * @param monitor progress monitor
	 * @return the imported projects
	 * @throws TeamException
	 */
	public List<IProject> run(IProgressMonitor monitor) throws TeamException {

		final Map<URIish, Map<String, Set<ProjectReference>>> repositories = parseReferenceStrings();

		final List<IProject> importedProjects = new ArrayList<>();

		SubMonitor progress = SubMonitor.convert(monitor, repositories.size());
		for (final Map.Entry<URIish, Map<String, Set<ProjectReference>>> entry : repositories
				.entrySet()) {
			final URIish gitUrl = entry.getKey();
			final Map<String, Set<ProjectReference>> refs = entry
					.getValue();

			SubMonitor subProgress = progress.newChild(1)
					.setWorkRemaining(refs.size());
			for (final Map.Entry<String, Set<ProjectReference>> refEntry : refs
					.entrySet()) {
				final String refName = refEntry.getKey();
				final Set<ProjectReference> projects = refEntry.getValue();

				final Set<String> allRefs = refs.keySet();

				File repositoryPath = null;
				if (allRefs.size() == 1)
					repositoryPath = findConfiguredRepository(gitUrl);

				SubMonitor subSubProgress = subProgress.newChild(1)
						.setWorkRemaining(repositoryPath == null ? 2 : 1);
				if (repositoryPath == null) {
					try {
						IPath workDir = getWorkingDir(gitUrl, refName, refs.keySet());
						repositoryPath = cloneIfNecessary(gitUrl, refName,
								workDir, projects, subSubProgress.newChild(1));
					} catch (final InterruptedException e) {
						// was canceled by user
						return Collections.emptyList();
					}
				}

				RepositoryUtil.INSTANCE.addConfiguredRepository(repositoryPath);

				for (ProjectReference projectReference : projects) {
					checkoutBranchIfNecessary(projectReference, monitor);
				}

				IPath newWorkDir = new Path(repositoryPath.getAbsolutePath())
						.removeLastSegments(1);
				List<IProject> p = importProjects(projects, newWorkDir,
						repositoryPath, subSubProgress.newChild(1));
				importedProjects.addAll(p);
			}
		}
		return importedProjects;
	}

	/**
	 * Check whether current branch is the same as configured in project
	 * reference. Checkout configured branch if current branch is different.
	 * Create new local branch if required.
	 *
	 * @param projectReference
	 *            the project reference to be checked
	 * @param monitor
	 *            progress monitor to be used for branch operation
	 * @throws TeamException
	 */
	private static void checkoutBranchIfNecessary(
			ProjectReference projectReference, IProgressMonitor monitor)
			throws TeamException {
		String projectName = new Path(projectReference.getProjectDir())
				.lastSegment();
		IProject projectInWorkspace = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);

		if (projectInWorkspace != null) {
			RepositoryMapping mapping = RepositoryMapping
					.getMapping(projectInWorkspace);
			if (mapping != null) {
				// get current branch
				String currentBranch;
				Repository repository = mapping.getRepository();
				try {
					currentBranch = repository.getBranch();
				} catch (IOException e) {
					throw new TeamException(e.getMessage());
				}
				if (RepositoryUtil.isDetachedHead(repository)) {
					currentBranch = RepositoryUtil.INSTANCE
							.mapCommitToRef(repository, currentBranch, false);
				}
				// Check for local changes
				if (RepositoryUtil.hasChanges(repository)) {
					throw new TeamException(NLS.bind(
							CoreText.GitProjectSetCapability_RepositoryIsDirty,
							repository.getWorkTree()));
				}
				// compare with current branch
				String configuredBranch = projectReference.getBranch();
				if (!configuredBranch.equals(currentBranch)) {
					// prepare to checkout configured branch
					Ref ref = null;
					try {
						ref = repository.findRef(configuredBranch);
					} catch (IOException e) {
						throw new TeamException(e.getMessage());
					}
					if (ref == null) {
						// if branch does not exist locally, find corresponding
						// remote branch
						try {
							Set<String> remoteNames = repository
									.getRemoteNames();
							for (String remote : remoteNames) {
								ref = repository.findRef(
										Constants.R_REMOTES + remote + "/" //$NON-NLS-1$
												+ configuredBranch);
								if (ref != null) {
									break; // remote branch found
								}
							}
							if (ref == null) {
								throw new TeamException(NLS.bind(
										CoreText.GitProjectSetCapability_RemoteBranchNotFound,
										configuredBranch,
										repository.getIdentifier()));
							}
						} catch (IOException e) {
							throw new TeamException(e.getMessage());
						}
						// create new local branch
						CreateLocalBranchOperation createLocalBranchOperation = new CreateLocalBranchOperation(
								repository, configuredBranch, ref,
								BranchRebaseMode.NONE);
						try {
							createLocalBranchOperation.execute(monitor);
						} catch (CoreException e) {
							throw new TeamException(e.getMessage());
						}
					}
					// Checkout configured branch
					BranchOperation branchOperation = new BranchOperation(
							repository, configuredBranch);
					try {
						branchOperation.execute(monitor);
					} catch (CoreException e) {
						throw new TeamException(e.getMessage());
					}
				}
			}
		}
	}

	private static File cloneIfNecessary(final URIish gitUrl, final String refToCheckout, final IPath workDir,
			final Set<ProjectReference> projects, IProgressMonitor monitor) throws TeamException, InterruptedException {

		final File repositoryPath = workDir.append(Constants.DOT_GIT_EXT).toFile();

		if (workDir.toFile().exists()) {
			if (repositoryAlreadyExistsForUrl(repositoryPath, gitUrl))
				return repositoryPath;
			else {
				final Collection<String> projectNames = new LinkedList<>();
				for (final ProjectReference projectReference : projects)
					projectNames.add(projectReference.getProjectDir());
				throw new TeamException(
						NLS.bind(CoreText.GitProjectSetCapability_CloneToExistingDirectory,
								new Object[] { workDir, projectNames, gitUrl }));
			}
		} else {
			try {
				int timeout = GitSettings.getRemoteConnectionTimeout();
				final CloneOperation cloneOperation = new CloneOperation(
						gitUrl, true, null, workDir.toFile(), refToCheckout,
						Constants.DEFAULT_REMOTE_NAME, timeout);
				cloneOperation.run(monitor);

				return repositoryPath;
			} catch (final InvocationTargetException e) {
				throw getTeamException(e);
			}
		}
	}

	private Map<URIish, Map<String, Set<ProjectReference>>> parseReferenceStrings()
			throws TeamException {
		final Map<URIish, Map<String, Set<ProjectReference>>> repositories = new LinkedHashMap<>();

		for (final String reference : referenceStrings) {
			if (reference == null) {
				// BundleImporterDelegate doesn't check invalid project URI's,
				// so we can receive null references.
				continue;
			}
			try {
				final ProjectReference projectReference = new ProjectReference(
						reference);
				Set<ProjectReference> projectReferences = repositories
						.computeIfAbsent(projectReference.getRepository(),
								repo -> new HashMap<>())
						.computeIfAbsent(projectReference.getBranch(),
								branch -> new LinkedHashSet<>());
				projectReferences.add(projectReference);
			} catch (final IllegalArgumentException | URISyntaxException e) {
				throw new TeamException(reference, e);
			}
		}

		return repositories;
	}

	/**
	 * @param gitUrl
	 * @param branch
	 *            the branch to check out
	 * @param allBranches
	 *            all branches which should be checked out for this gitUrl
	 * @return the directory where the project should be checked out
	 */
	private static IPath getWorkingDir(URIish gitUrl, String branch,
			Set<String> allBranches) {
		final IPath defaultRepoLocation = new Path(
				RepositoryUtil.getDefaultRepositoryDir());
		final String humanishName = gitUrl.getHumanishName();
		String extendedName;
		if (allBranches.size() == 1 || branch.equals(Constants.MASTER)) {
			extendedName = humanishName;
		} else {
			extendedName = humanishName + "_" + branch; //$NON-NLS-1$
		}
		return defaultRepoLocation.append(extendedName);
	}

	static File findConfiguredRepository(URIish gitUrl) {
		for (String repoDir : RepositoryUtil.INSTANCE
				.getConfiguredRepositories()) {
			File repoDirFile = new File(repoDir);
			if (repositoryAlreadyExistsForUrl(repoDirFile, gitUrl))
				return repoDirFile;
		}
		return null;
	}

	private static boolean repositoryAlreadyExistsForUrl(File repositoryPath,
			URIish gitUrl) {
		if (repositoryPath.exists()) {
			Repository existingRepository;
			try {
				existingRepository = FileRepositoryBuilder
						.create(repositoryPath);
			} catch (IOException e) {
				return false;
			}
			try {
				boolean exists = containsRemoteForUrl(
						existingRepository.getConfig(), gitUrl);
				return exists;
			} catch (URISyntaxException e) {
				return false;
			} finally {
				existingRepository.close();
			}
		}
		return false;
	}

	private static boolean containsRemoteForUrl(Config config, URIish url)
			throws URISyntaxException {
		Set<String> remotes = config.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION);
		for (String remote : remotes) {
			String remoteUrl = config.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION,
					remote,
					ConfigConstants.CONFIG_KEY_URL);
			URIish existingUrl = new URIish(remoteUrl);
			if (existingUrl.equals(url))
				return true;

			// there may be slight differences in the URLs...
			URIish canonExistingUrl = canonicalizeURL(existingUrl);
			URIish canonUrl = canonicalizeURL(url);
			if (canonExistingUrl.equals(canonUrl))
				return true;
		}
		return false;
	}

	private static URIish canonicalizeURL(URIish existingUrl) {
		// try URLs without user name, since often project sets contain
		// anonymous URLs, and remote URL might be anonymous as well
		URIish newURL = existingUrl.setUser(null);

		// some URLs end with .git, some don't
		String path = existingUrl.getPath();
		if (path.endsWith(".git")) { //$NON-NLS-1$
			newURL = newURL
					.setPath(path.substring(0,
					path.lastIndexOf(".git"))); //$NON-NLS-1$
		}

		return newURL;
	}

	private List<IProject> importProjects(final Set<ProjectReference> projects,
			final IPath workDir, final File repositoryPath,
			final IProgressMonitor monitor) throws TeamException {
		try {

			List<IProject> importedProjects = new ArrayList<>();

			// import projects from the current repository to workspace
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final IWorkspaceRoot root = workspace.getRoot();
			SubMonitor progress = SubMonitor.convert(monitor, projects.size());
			for (final ProjectReference projectToImport : projects) {
				SubMonitor subProgress = SubMonitor
						.convert(progress.newChild(1), 3);
				final IPath projectDir = workDir.append(projectToImport
						.getProjectDir());
				final IProjectDescription projectDescription = workspace
						.loadProjectDescription(projectDir
								.append(IProjectDescription.DESCRIPTION_FILE_NAME));
				final IProject project = root.getProject(projectDescription
						.getName());
				if (!project.exists()) {
					project.create(projectDescription, subProgress.newChild(1));
					importedProjects.add(project);
				}
				subProgress.setWorkRemaining(2);
				project.open(subProgress.newChild(1));
				final ConnectProviderOperation connectProviderOperation = new ConnectProviderOperation(
						project, repositoryPath);
				connectProviderOperation.execute(subProgress.newChild(1));
			}

			return importedProjects;

		} catch (final CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	private static TeamException getTeamException(final Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null)
			current = current.getCause();
		return new TeamException(current.getMessage(), current);
	}
}
