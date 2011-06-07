/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import static org.eclipse.egit.core.project.RepositoryMapping.findRepositoryMapping;
import static org.eclipse.jgit.lib.FileMode.MISSING;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 *
 */
public class RemoveFromIndexOperation implements IEGitOperation {

	private final Repository repo;

	private final Collection<String> paths;

	/**
	 * @param repo
	 *            repository in with given files should be removed from index
	 * @param paths
	 *            list repository relative path of files/folders that should be
	 *            removed from index
	 */
	public RemoveFromIndexOperation(Repository repo, Collection<String> paths) {
		this.repo = repo;
		this.paths = paths;
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;

		final DirCache dirCache;
		final DirCacheEditor edit;
		try {
			dirCache = repo.lockDirCache();
			edit = dirCache.editor();
		} catch (IOException e) {
			throw new CoreException(Activator.error(
					CoreText.RemoveFromIndexOperation_failed, e));
		}

		try {
			for (String path : paths) {
				updateDirCache(path, edit);
				monitor.worked(20);
			}
			try {
				edit.commit();
			} catch (IOException e) {
				throw new CoreException(Activator.error(
						CoreText.RemoveFromIndexOperation_failed, e));
			}
		} finally {
			dirCache.unlock();
			monitor.done();
			findRepositoryMapping(repo).fireRepositoryChanged();
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	private void updateDirCache(String path, final DirCacheEditor edit)
			throws CoreException {
		RevCommit headRev = getHeadRev();
		boolean isContainer = new File(repo.getWorkTree(), path).isDirectory();
		try {
			final TreeWalk tw = TreeWalk.forPath(repo, path, headRev.getTree());

			if (tw != null) {
				if (isContainer)
					tw.addTree(new DirCacheIterator(edit.getDirCache()));
				do {
					if (tw.isSubtree())
						tw.enterSubtree();
					else if (tw.getRawMode(0) == MISSING.getBits())
						edit.add(new DirCacheEditor.DeletePath(tw.getPathString()));
					else {
						final FileMode fileMode = tw.getFileMode(0);
						final ObjectId objectId = tw.getObjectId(0);
						edit.add(new DirCacheEditor.PathEdit(tw.getPathString()) {
							@Override
							public void apply(DirCacheEntry ent) {
								ent.setFileMode(fileMode);
								ent.setObjectId(objectId);
								// for index & working tree compare
								ent.setLastModified(0);
							}
						});
					}
				} while (tw.next());
			} else {
				if (isContainer)
					edit.add(new DirCacheEditor.DeleteTree(path));
				else
					edit.add(new DirCacheEditor.DeletePath(path));
			}
		} catch (IOException e) {
			throw new CoreException(Activator.error(
					CoreText.RemoveFromIndexOperation_failed, e));
		}
	}

	private RevCommit getHeadRev() throws CoreException {
		try {
			final Ref head = repo.getRef(Constants.HEAD);
			return new RevWalk(repo).parseCommit(head.getObjectId());
		} catch (IOException e) {
			throw new CoreException(Activator.error(
					CoreText.RemoveFromIndexOperation_failed, e));
		}
	}

}
