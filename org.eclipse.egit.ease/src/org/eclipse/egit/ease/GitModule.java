/*******************************************************************************
 * Copyright (c) 2016 Christian Pontesegger and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     Max Hohenegger - Bug 510827
 *******************************************************************************/
package org.eclipse.egit.ease;

import static org.eclipse.egit.ease.internal.UIText.invalidFolderLocation;
import static org.eclipse.egit.ease.internal.UIText.noRepositoryFoundAt;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.core.resources.IContainer;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.ease.tools.ResourceTools;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * EASE Module for scripting basic JGit operations.
 */
public class GitModule extends AbstractScriptModule {

	/**
	 * Clone a git repository.
	 *
	 * @param remoteLocation
	 *            location to fetch repository from
	 * @param localLocation
	 *            local path to be used
	 * @param user
	 *            username for the remote repository
	 * @param pass
	 *            password for the remote repository
	 * @param branch
	 *            branch to checkout (<code>null</code> for all branches)
	 * @return GIT API instance
	 * @throws InvalidRemoteException
	 *             when command was called with an invalid remote
	 * @throws TransportException
	 *             when transport operation failed
	 * @throws GitAPIException
	 */
	@WrapToScript
	public Git clone(final String remoteLocation, final Object localLocation, @ScriptParameter(defaultValue = ScriptParameter.NULL) final String user,
			@ScriptParameter(defaultValue = ScriptParameter.NULL) final String pass, @ScriptParameter(defaultValue = ScriptParameter.NULL) final String branch)
			throws InvalidRemoteException, TransportException, GitAPIException {

		final File folder = resolveFolder(localLocation);
		if (folder == null) {
			throw new IllegalArgumentException(invalidFolderLocation + localLocation);
		}
		final CloneCommand cloneCommand = Git.cloneRepository();
		cloneCommand.setURI(remoteLocation);
		cloneCommand.setDirectory(folder);

		if ((user != null) && (pass != null)) {
			cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));
		}

		if (branch != null) {
			cloneCommand.setBranchesToClone(Arrays.asList(branch));
		}

		final Git result = cloneCommand.call();
		return result;
	}

	/**
	 * Open a local repository.
	 *
	 * @param location
	 *            local repository root folder
	 * @return GIT API instance
	 * @throws IOException
	 */
	@WrapToScript
	public Git openRepository(final Object location) throws IOException {
		if (location instanceof Git) {
			return (Git) location;
		}

		final File folder = resolveFolder(location);
		if (folder == null) {
			throw new IllegalArgumentException(invalidFolderLocation + location);
		}
		return Git.open(folder);
	}

	/**
	 * Initialize a fresh repository.
	 *
	 * @param location
	 *            repository location
	 * @param bare
	 *            <code>true</code> for bare repositories
	 * @return GIT API instance
	 * @throws IllegalStateException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public Git initRepository(final Object location, @ScriptParameter(defaultValue = "false") final boolean bare)
			throws IllegalStateException, GitAPIException {
		final File folder = resolveFolder(location);
		if (folder == null) {
			throw new IllegalArgumentException(invalidFolderLocation + location);
		}

		if (!folder.exists()) {
			folder.mkdirs();
		}

		return Git.init().setDirectory(folder).setBare(bare).call();
	}

	/**
	 * Commit to a repository.
	 *
	 * @param repository
	 *            repository instance or location (local) to pull
	 * @return RevCommit result
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public RevCommit commit(final Object repository, final String message, final String author, @ScriptParameter(defaultValue = "false") final boolean amend)
			throws IOException, GitAPIException {
		final Git repo = openRepository(repository);
		if (repo == null) {
			throw new IllegalArgumentException(noRepositoryFoundAt + repository);
		}

		// parse author
		String authorName = ""; //$NON-NLS-1$
		String authorEmail = ""; //$NON-NLS-1$
		if (author != null) {
			final String[] authorTokens = author.split("|"); //$NON-NLS-1$
			if (authorTokens.length > 0) {
				authorName = authorTokens[0].trim();
			}

			if (authorTokens.length > 1) {
				authorEmail = authorTokens[1].trim();
			}
		}

		return repo.commit().setMessage(message).setAuthor(authorName, authorEmail).setAmend(amend).call();
	}

	/**
	 * Get repository status.
	 *
	 * @param repository
	 *            repository instance or location (local) to get status from
	 * @return add result
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public DirCache add(final Object repository, final String filepattern) throws IOException, GitAPIException {
		final Git repo = openRepository(repository);
		if (repo == null) {
			throw new IllegalArgumentException(noRepositoryFoundAt + repository);
		}
		return repo.add().addFilepattern(filepattern).call();
	}

	/**
	 * Get repository status.
	 *
	 * @param repository
	 *            repository instance or location (local) to get status from
	 * @return repository status
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public Status getStatus(final Object repository) throws IOException, GitAPIException {
		final Git repo = openRepository(repository);
		if (repo == null) {
			throw new IllegalArgumentException(noRepositoryFoundAt + repository);
		}
		return repo.status().call();
	}

	/**
	 * Push a repository.
	 *
	 * @param repository
	 *            repository instance or location (local) to pull
	 * @return push result
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public Iterable<PushResult> push(final Object repository) throws IOException, GitAPIException {
		final Git repo = openRepository(repository);
		if (repo == null) {
			throw new IllegalArgumentException(noRepositoryFoundAt + repository);
		}
		return repo.push().call();
	}

	/**
	 * Pull a repository.
	 *
	 * @param repository
	 *            repository instance or location (local) to pull
	 * @return pull result
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public PullResult pull(final Object repository) throws IOException, GitAPIException {
		final Git repo = openRepository(repository);
		if (repo == null) {
			throw new IllegalArgumentException(noRepositoryFoundAt + repository);
		}
		return repo.pull().call();
	}

	private final File resolveFolder(final Object location) {
		Object folder = ResourceTools.resolveFolder(location, getScriptEngine(), false);
		if (folder instanceof IContainer) {
			folder = ((IContainer) folder).getRawLocation().makeAbsolute().toFile();
		}

		if ((folder instanceof File) && (!((File) folder).exists() || (((File) folder).isDirectory()))) {
			return (File) folder;
		}

		return null;
	}
}
