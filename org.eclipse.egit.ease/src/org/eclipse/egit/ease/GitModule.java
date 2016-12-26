package org.eclipse.egit.ease;

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

		File folder = resolveFolder(localLocation);
		if (folder != null) {
			CloneCommand cloneCommand = Git.cloneRepository();
			cloneCommand.setURI(remoteLocation);
			cloneCommand.setDirectory(folder);

			if ((user != null) && (pass != null))
				cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, pass));

			if (branch != null)
				cloneCommand.setBranchesToClone(Arrays.asList(branch));

			Git result = cloneCommand.call();
			addToEGit(result.getRepository().getDirectory());
			return result;

		} else
			throw new RuntimeException("invalid local folder detected: " + localLocation);
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
		if (location instanceof Git)
			return (Git) location;

		File folder = resolveFolder(location);
		if (folder != null)
			return Git.open(folder);

		else
			throw new RuntimeException("Invalid folder location: " + location);
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
		File folder = resolveFolder(location);
		if (folder != null) {
			if (!folder.exists())
				folder.mkdirs();

			Git result = Git.init().setDirectory(folder).setBare(bare).call();
			addToEGit(result.getRepository().getDirectory());
			return result;

		} else
			throw new RuntimeException("Invalid folder location: " + location);
	}

	/**
	 * Add git repository to EGit repositories view, if available.
	 *
	 * @param directory
	 *            .git folder of a local git repository
	 */
	private static void addToEGit(final File directory) {
		// FIXME needs EGit master branch (4.4) as previously there was no public API available
		// try {
		// org.eclipse.egit.core.Activator.getDefault().getRepositoryUtil().addConfiguredRepository(directory);
		// } catch (NoClassDefFoundError e) {
		// // seems EGit is not available, ignore
		// }
	}

	/**
	 * Commit to a repository.
	 *
	 * @param repository
	 *            repository instance or location (local) to pull
	 * @return commit result
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@WrapToScript
	public RevCommit commit(final Object repository, final String message, final String author, @ScriptParameter(defaultValue = "false") final boolean amend)
			throws IOException, GitAPIException {
		Git repo = openRepository(repository);
		if (repo != null) {

			// parse author
			String authorName = "";
			String authorEmail = "";
			if (author != null) {
				String[] authorTokens = author.split("|");
				if (authorTokens.length > 0)
					authorName = authorTokens[0].trim();

				if (authorTokens.length > 1)
					authorEmail = authorTokens[1].trim();
			}

			return repo.commit().setMessage(message).setAuthor(authorName, authorEmail).setAmend(amend).call();

		} else
			throw new RuntimeException("No repository found at: " + repository);
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
		Git repo = openRepository(repository);
		if (repo != null) {
			return repo.add().addFilepattern(filepattern).call();

		} else
			throw new RuntimeException("No repository found at: " + repository);
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
		Git repo = openRepository(repository);
		if (repo != null) {
			return repo.status().call();

		} else
			throw new RuntimeException("No repository found at: " + repository);
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
		Git repo = openRepository(repository);
		if (repo != null) {
			return repo.push().call();

		} else
			throw new RuntimeException("No repository found at: " + repository);
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
		Git repo = openRepository(repository);
		if (repo != null) {
			return repo.pull().call();

		} else
			throw new RuntimeException("No repository found at: " + repository);
	}

	private final File resolveFolder(final Object location) {
		Object folder = ResourceTools.resolveFolder(location, getScriptEngine(), false);
		if (folder instanceof IContainer)
			// convert workspace resource to local file
			folder = ((IContainer) folder).getRawLocation().makeAbsolute().toFile();

		if ((folder instanceof File) && (!((File) folder).exists() || (((File) folder).isDirectory())))
			return (File) folder;

		return null;
	}
}
