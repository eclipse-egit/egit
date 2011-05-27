/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.treewalk.TreeOptions;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;

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
		FileRepository tmpRepository = new FileRepository(gitDir);
		tmpRepository.create();
		tmpRepository.close();
		// use repository instance from RepositoryCache!
		repository = Activator.getDefault().getRepositoryCache().lookupRepository(gitDir);
		try {
			workdirPrefix = repository.getWorkTree().getCanonicalPath();
		} catch (IOException err) {
			workdirPrefix = repository.getWorkTree().getAbsolutePath();
		}
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/")) //$NON-NLS-1$
			workdirPrefix += "/"; //$NON-NLS-1$
	}

	/**
	 * Creates a test repository from an existing Repository
	 *
	 * @param repository
	 * @throws IOException
	 */
	public TestRepository(Repository repository) throws IOException {
		this.repository = repository;
		try {
			workdirPrefix = repository.getWorkTree().getCanonicalPath();
		} catch (IOException err) {
			workdirPrefix = repository.getWorkTree().getAbsolutePath();
		}
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/")) //$NON-NLS-1$
			workdirPrefix += "/"; //$NON-NLS-1$
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
		String repoPath = repository.getWorkTree().getAbsolutePath();
		File file = new File(repoPath, "dummy");
		if (!file.exists())
			FileUtils.createNewFile(file);
		track(file);
		return commit(message);
	}

	/**
	 * Create a file or get an existing one
	 *
	 * @param project
	 *            instance of project inside with file will be created
	 * @param name
	 *            name of file
	 * @return nearly created file
	 * @throws IOException
	 */
	public File createFile(IProject project, String name) throws IOException {
		String path = project.getLocation().append(name).toOSString();
		int lastSeparator = path.lastIndexOf(File.separator);
		FileUtils.mkdirs(new File(path.substring(0, lastSeparator)), true);

		File file = new File(path);
		if (!file.exists())
			FileUtils.createNewFile(file);

		return file;
	}

	/**
	 * Track, add to index and finally commit given file
	 *
	 * @param project
	 * @param file
	 * @param commitMessage
	 * @return commit object
	 * @throws Exception
	 */
	public RevCommit addAndCommit(IProject project, File file, String commitMessage)
			throws Exception {
		track(file);
		addToIndex(project, file);

		return commit(commitMessage);
	}

	/**
	 * Appends file content to given file, then track, add to index and finally
	 * commit it.
	 *
	 * @param project
	 * @param file
	 * @param content
	 * @param commitMessage
	 * @return commit object
	 * @throws Exception
	 */
	public RevCommit appendContentAndCommit(IProject project, File file,
			byte[] content, String commitMessage) throws Exception {
		return appendContentAndCommit(project, file, new String(content),
				commitMessage);
	}

	/**
	 * Appends file content to given file, then track, add to index and finally
	 * commit it.
	 *
	 * @param project
	 * @param file
	 * @param content
	 * @param commitMessage
	 * @return commit object
	 * @throws Exception
	 */
	public RevCommit appendContentAndCommit(IProject project, File file,
			String content, String commitMessage) throws Exception {
		appendFileContent(file, content);
		track(file);
		addToIndex(project, file);

		return commit(commitMessage);
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
		String repoPath = getRepoRelativePath(new Path(file.getPath())
				.toString());
		try {
			new Git(repository).add().addFilepattern(repoPath).call();
		} catch (NoFilepatternException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Creates a new branch and immediately checkout it.
	 *
	 * @param refName
	 *            starting point for the new branch
	 * @param newRefName
	 * @throws Exception
	 */
	public void createAndCheckoutBranch(String refName, String newRefName) throws Exception {
		createBranch(refName, newRefName);
		checkoutBranch(newRefName);
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
		startBranch = Repository.shortenRefName(startBranch);
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

	/**
	 * Checkouts branch
	 *
	 * @param refName
	 *            full name of branch
	 * @throws CoreException
	 */
	public void checkoutBranch(String refName) throws CoreException {
		new BranchOperation(repository, refName).execute(null);
	}

	/**
	 * Adds the given file to the index
	 *
	 * @param project
	 * @param file
	 * @throws Exception
	 */
	public void addToIndex(IProject project, File file) throws Exception {
		IFile iFile = getIFile(project, file);
		addToIndex(iFile);
	}


	/**
	 * Adds the given file to the index
	 *
	 * @param file
	 * @throws CoreException
	 * @throws IOException
	 */
	public void addToIndex(IFile file) throws CoreException, IOException {
		String repoPath = getRepoRelativePath(file.getLocation().toOSString());
		try {
			new Git(repository).add().addFilepattern(repoPath).call();
		} catch (NoFilepatternException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Appends content to end of given file.
	 *
	 * @param file
	 * @param content
	 * @throws IOException
	 */
	public void appendFileContent(File file, byte[] content) throws IOException {
		appendFileContent(file, new String(content), true);
	}

	/**
	 * Appends content to end of given file.
	 *
	 * @param file
	 * @param content
	 * @throws IOException
	 */
	public void appendFileContent(File file, String content) throws IOException {
		appendFileContent(file, content, true);
	}

	/**
	 * Appends content to given file.
	 *
	 * @param file
	 * @param content
	 * @param append
	 *            if true, then bytes will be written to the end of the file
	 *            rather than the beginning
	 * @throws IOException
	 */
	public void appendFileContent(File file, byte[] content, boolean append)
			throws IOException {
		appendFileContent(file, new String(content), append);
	}

	/**
	 * Appends content to given file.
	 *
	 * @param file
	 * @param content
	 * @param append
	 *            if true, then bytes will be written to the end of the file
	 *            rather than the beginning
	 * @throws IOException
	 */
	public void appendFileContent(File file, String content, boolean append)
			throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file, append);
			fw.append(content);
		} finally {
			if (fw != null)
				fw.close();
		}
	}

	/**
	 * Checks if a file with the given path exists in the HEAD tree
	 *
	 * @param path
	 * @return true if the file exists
	 * @throws IOException
	 */
	public boolean inHead(String path) throws IOException {
		ObjectId headId = repository.resolve(Constants.HEAD);
		RevWalk rw = new RevWalk(repository);
		TreeWalk tw = null;
		try {
			tw = TreeWalk.forPath(repository, path, rw.parseTree(headId));
			return tw != null;
		} finally {
			rw.release();
			rw.dispose();
			if (tw != null)
				tw.release();
		}
	}

	public boolean inIndex(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS(), new TreeOptions(repository.getConfig()));

		return dc.getEntry(repoPath) != null;
	}

	public long lastModifiedInIndex(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS(), new TreeOptions(repository.getConfig()));

		return dc.getEntry(repoPath).getLastModified();
	}

	public int getDirCacheEntryLength(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS(), new TreeOptions(repository.getConfig()));

		return dc.getEntry(repoPath).getLength();
	}

	public String getRepoRelativePath(String path) {
		final int pfxLen = workdirPrefix.length();
		final int pLen = path.length();
		if (pLen > pfxLen)
			return path.substring(pfxLen);
		else if (path.length() == pfxLen - 1)
			return ""; //$NON-NLS-1$
		return null;
	}

	public IFile getIFile(IProject project, File file) throws CoreException {
		String relativePath = getRepoRelativePath(file.getAbsolutePath());

		String quotedProjectName = Pattern.quote(project.getName());
		relativePath = relativePath.replaceFirst(quotedProjectName, "");

		IFile iFile = project.getFile(relativePath);
		iFile.refreshLocal(0, null);

		return iFile;
	}

	public void dispose() {
		repository.close();
		repository = null;
	}

	/**
	 * Connect a project to this repository
	 *
	 * @param project
	 * @throws CoreException
	 */
	public void connect(IProject project) throws CoreException {
		ConnectProviderOperation op = new ConnectProviderOperation(project,
				this.getRepository().getDirectory());
		op.execute(null);
	}

	/**
	 * Disconnects provider from project
	 *
	 * @param project
	 * @throws CoreException
	 */
	public void disconnect(IProject project) throws CoreException {
		Collection<IProject> projects = Collections.singleton(project
				.getProject());
		DisconnectProviderOperation disconnect = new DisconnectProviderOperation(
				projects);
		disconnect.execute(null);
	}

}
