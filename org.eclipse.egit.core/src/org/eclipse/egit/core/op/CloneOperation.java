/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;

/**
 * Clones a repository from a remote location to a local location.
 */
public class CloneOperation {
	private final URIish uri;

	private final boolean allSelected;

	private final Collection<Ref> selectedBranches;

	private final File workdir;

	private final File gitdir;

	private final String refName;

	private final String remoteName;

	private final int timeout;

	private CredentialsProvider credentialsProvider;

	private List<PostCloneTask> postCloneTasks;

	/**
	 * Create a new clone operation.
	 *
	 * @param uri
	 *            remote we should fetch from.
	 * @param allSelected
	 *            true when all branches have to be fetched (indicates wildcard
	 *            in created fetch refspec), false otherwise.
	 * @param selectedBranches
	 *            collection of branches to fetch. Ignored when allSelected is
	 *            true.
	 * @param workdir
	 *            working directory to clone to. The directory may or may not
	 *            already exist.
	 * @param refName
	 *            full name of ref to be checked out after clone, e.g.
	 *            refs/heads/master, or null for no checkout
	 * @param remoteName
	 *            name of created remote config as source remote (typically
	 *            named "origin").
	 * @param timeout
	 *            timeout in seconds
	 */
	public CloneOperation(final URIish uri, final boolean allSelected,
			final Collection<Ref> selectedBranches, final File workdir,
			final String refName, final String remoteName, int timeout) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.selectedBranches = selectedBranches;
		this.workdir = workdir;
		this.gitdir = new File(workdir, Constants.DOT_GIT);
		this.refName = refName;
		this.remoteName = remoteName;
		this.timeout = timeout;
	}

	/**
	 * Sets a credentials provider
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @param pm
	 *            the monitor to be used for reporting progress and responding
	 *            to cancellation. The monitor is never <code>null</code>
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public void run(final IProgressMonitor pm)
			throws InvocationTargetException, InterruptedException {
		final IProgressMonitor monitor;
		if (pm == null)
			monitor = new NullProgressMonitor();
		else
			monitor = pm;

		EclipseGitProgressTransformer gitMonitor = new EclipseGitProgressTransformer(
				monitor);
		Repository repository = null;
		try {
			monitor.beginTask(NLS.bind(CoreText.CloneOperation_title, uri),
					5000);
			CloneCommand cloneRepository = Git.cloneRepository();
			cloneRepository.setCredentialsProvider(credentialsProvider);
			if (refName != null)
				cloneRepository.setBranch(refName);
			cloneRepository.setDirectory(workdir);
			cloneRepository.setProgressMonitor(gitMonitor);
			cloneRepository.setRemote(remoteName);
			cloneRepository.setURI(uri.toString());
			cloneRepository.setTimeout(timeout);
			cloneRepository.setCloneAllBranches(allSelected);
			if (selectedBranches != null) {
				List<String> branches = new ArrayList<String>();
				for (Ref branch : selectedBranches)
					branches.add(branch.getName());
				cloneRepository.setBranchesToClone(branches);
			}
			Git git = cloneRepository.call();
			repository = git.getRepository();
			synchronized (this) {
				if (postCloneTasks != null)
					for (PostCloneTask task : postCloneTasks)
						task.execute(git.getRepository(), monitor);
			}
		} catch (final Exception e) {
			try {
				if (repository != null)
					repository.close();
				FileUtils.delete(workdir, FileUtils.RECURSIVE);
			} catch (IOException ioe) {
				throw new InvocationTargetException(ioe);
			}
			if (monitor.isCanceled())
				throw new InterruptedException();
			else
				throw new InvocationTargetException(e);
		} finally {
			monitor.done();
			if (repository != null)
				repository.close();
		}
	}


	/**
	 * @return The git directory which will contain the repository
	 */
	public File getGitDir() {
		return gitdir;
	}

	/**
	 * @param task to be performed after clone
	 */
	public synchronized void addPostCloneTask(PostCloneTask task) {
		if (postCloneTasks == null)
			postCloneTasks = new ArrayList<PostCloneTask>();
		postCloneTasks.add(task);
	}

	/**
	 * A task which can be added to be performed after clone
	 */
	public interface PostCloneTask  {

		/**
		 * Executes the task
		 * @param repository the cloned git repository
		 *
		 * @param monitor
		 *            a progress monitor, or <code>null</code> if progress reporting
		 *            and cancellation are not desired
		 * @throws CoreException
		 */
		void execute(Repository repository, IProgressMonitor monitor) throws CoreException;
	}
}
