/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import static org.eclipse.jgit.lib.ObjectId.zeroId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.variants.IResourceVariant;

class GitRemoteFolder extends GitRemoteResource {

	/**
	 *
	 * @param repo
	 * @param revCommit
	 * @param objectId
	 *            given object sha-1 id or {@code null} if this object doesn't
	 *            exist in remote
	 * @param path
	 */
	GitRemoteFolder(Repository repo, RevCommit revCommit, ObjectId objectId,
			String path) {
		super(repo, revCommit, objectId, path);
	}

	public boolean isContainer() {
		return true;
	}

	public boolean equals(Object object) {
		if (object == this)
			return true;

		if (object instanceof GitRemoteFolder) {
			GitRemoteFolder that = (GitRemoteFolder) object;

			return getPath().equals(that.getPath())
					&& getObjectId().equals(that.getObjectId());
		}

		return false;
	}

	public int hashCode() {
		return getObjectId().hashCode() ^ getPath().hashCode();
	}

	GitRemoteResource[] members(IProgressMonitor monitor) throws IOException {
		Repository repo = getRepo();
		TreeWalk tw = new TreeWalk(repo);
		int nth = tw.addTree(getObjectId());

		IProgressMonitor m = SubMonitor.convert(monitor);
		m.beginTask(NLS.bind(CoreText.GitFolderResourceVariant_fetchingMembers,
				this), tw.getTreeCount());

		int i = 0;
		List<IResourceVariant> result = new ArrayList<IResourceVariant>();
		try {
			while (tw.next()) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();

				ObjectId newObjectId = tw.getObjectId(nth);
				String path = getPath() + "/" + tw.getPathString(); //$NON-NLS-1$

				if (!zeroId().equals(newObjectId))
					if (tw.isSubtree())
						result.add(new GitRemoteFolder(repo, getCommit(),
								newObjectId, path));
					else
						result.add(new GitRemoteFile(repo, getCommit(),
								newObjectId, path));

				if (i % 10 == 0)
					m.worked(10);

				i++;
			}
		} finally {
			m.done();
		}

		return result.toArray(new GitRemoteResource[result.size()]);
	}

}
