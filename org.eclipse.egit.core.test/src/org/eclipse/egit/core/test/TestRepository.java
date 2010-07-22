/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.JGitInternalException;
import org.eclipse.jgit.api.NoHeadException;
import org.eclipse.jgit.api.NoMessageException;
import org.eclipse.jgit.api.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Helper class for creating and filling a test repository
 *
 */
public class TestRepository {

	Repository repository;
	String workdirPrefix;

	/**
	 * Creates a new test repository
	 *
	 * @param gitDir
	 * @throws IOException
	 */
	public TestRepository(File gitDir) throws IOException {
		repository = new Repository(gitDir);
		repository.create();
		try {
			workdirPrefix = repository.getWorkDir().getCanonicalPath();
		} catch (IOException err) {
			workdirPrefix = repository.getWorkDir().getAbsolutePath();
		}
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/"))  //$NON-NLS-1$
			workdirPrefix += "/";  //$NON-NLS-1$
	}

	/**
	 * Creates a test repository from an existing Repository
	 * @param repository
	 * @throws IOException
	 */
	public TestRepository(Repository repository) throws IOException {
		this.repository = repository;
		try {
			workdirPrefix = repository.getWorkDir().getCanonicalPath();
		} catch (IOException err) {
			workdirPrefix = repository.getWorkDir().getAbsolutePath();
		}
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/"))  //$NON-NLS-1$
			workdirPrefix += "/";  //$NON-NLS-1$
	}

	/**
	 * @return the wrapped repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * create an initial commit containing a file "dummy" in the
	 *
	 * @param message
	 *            commit message
	 * @return commit object
	 * @throws IOException
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws ConcurrentRefUpdateException
	 * @throws JGitInternalException
	 * @throws WrongRepositoryStateException
	 */
	public RevCommit createInitialCommit(String message) throws IOException,
			NoHeadException, NoMessageException, ConcurrentRefUpdateException,
			JGitInternalException, WrongRepositoryStateException {
		String repoPath = repository.getWorkDir().getAbsolutePath();
		File file = new File(repoPath, "dummy");
		file.createNewFile();
		track(file);
		return commit(message);
	}

	/**
	 * Commits the current index
	 *
	 * @param message
	 *            commit message
	 * @return commit object
	 *
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathException
	 * @throws ConcurrentRefUpdateException
	 * @throws JGitInternalException
	 * @throws WrongRepositoryStateException
	 */
	public RevCommit commit(String message) throws NoHeadException,
			NoMessageException, UnmergedPathException,
			ConcurrentRefUpdateException, JGitInternalException,
			WrongRepositoryStateException {
		Git git = new Git(repository);
		CommitCommand commitCommand = git.commit();
		commitCommand.setAuthor("J. Git", "j.git@egit.org");
		commitCommand.setCommitter(commitCommand.getAuthor());
		commitCommand.setMessage(message);
		return commitCommand.call();
	}

	/**
	 * Adds file to version control
	 *
	 * @param file
	 * @throws IOException
	 */
	public void track(File file) throws IOException {
		GitIndex index = repository.getIndex();
		Entry entry = index.add(repository.getWorkDir(), file);
		entry.setAssumeValid(false);
		index.write();
	}

	/**
	 * Creates a new branch
	 *
	 * @param refName
	 *            starting point for the new branch
	 * @param newRefName
	 * @throws IOException
	 */
	public void createBranch(String refName, String newRefName)
			throws IOException {
		RefUpdate updateRef;
		updateRef = repository.updateRef(newRefName);
		Ref startRef = repository.getRef(refName);
		ObjectId startAt = repository.resolve(refName);
		String startBranch;
		if (startRef != null)
			startBranch = refName;
		else
			startBranch = startAt.name();
		startBranch = repository.shortenRefName(startBranch);
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

	/**
	 * Adds the given file to the index
	 * @param file
	 * @throws IOException
	 */
	public void addToIndex(IFile file) throws IOException {
		GitIndex index = repository.getIndex();
		Entry entry = index.getEntry(getRepoRelativePath(file.getLocation().toOSString()));
		assertNotNull(entry);
		if (entry.isModified(repository.getWorkDir()))
			entry.update(new File(repository.getWorkDir(), entry.getName()));
		index.write();
	}

	/**
	 * Checks if a file with the given path exists in the HEAD tree
	 * @param path
	 * @return true if the file exists
	 * @throws IOException
	 */
	public boolean inHead(String path) throws IOException {
		Tree headTree = repository.mapTree(Constants.HEAD);
		String repoPath = getRepoRelativePath(path);
		boolean headExists = headTree.existsBlob(repoPath);
		return headExists;
	}

	public boolean inIndex(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository);

		return dc.getEntry(repoPath) != null;
	}

	public long lastModifiedInIndex(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository);

		return dc.getEntry(repoPath).getLastModified();
	}

	public int getDirCacheEntryLength(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository);

		return dc.getEntry(repoPath).getLength();
	}

	public String getRepoRelativePath(String path) {
		final int pfxLen = workdirPrefix.length();
		final int pLen = path.length();
		if (pLen > pfxLen)
			return path.substring(pfxLen);
		else if (path.length() == pfxLen - 1)
			return "";  //$NON-NLS-1$
		return null;
	}

	public void dispose() {
		repository.close();
		repository = null;
	}

	/**
	 * Connect a project to this repository
	 * @param project
	 * @throws CoreException
	 */
	public void connect(IProject project) throws CoreException {
		ConnectProviderOperation op = new ConnectProviderOperation(project,
				this.getRepository().getDirectory());
		op.execute(null);
	}
}
