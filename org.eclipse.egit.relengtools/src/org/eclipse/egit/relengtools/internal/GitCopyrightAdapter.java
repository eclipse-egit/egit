/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation of CVS adapter
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.relengtools.internal;

import java.io.IOException;
import java.util.Calendar;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.releng.tools.RepositoryProviderCopyrightAdapter;

public class GitCopyrightAdapter extends RepositoryProviderCopyrightAdapter {

	private static String filterString = "copyright"; // lowercase //$NON-NLS-1$

	public GitCopyrightAdapter(IResource[] resources) {
		super(resources);
	}

	public int getLastModifiedYear(IFile file, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("Fetching logs from Git", 100); //$NON-NLS-1$
			RepositoryMapping mapping = RepositoryMapping.getMapping(file);
			if (mapping != null) {
				Repository repo = mapping.getRepository();
				if (repo != null) {
					RevWalk walk = null;
					try {
						ObjectId start = repo.resolve(Constants.HEAD);
						walk = new RevWalk(repo);
						walk.setTreeFilter(PathFilter.create(mapping.getRepoRelativePath(file)));
						walk.markStart(walk.lookupCommit(start));
						RevCommit commit = walk.next();
						if (commit != null) {
							if (filterString != null && commit.getFullMessage().toLowerCase().indexOf(filterString) != -1) {
								//the last update was a copyright checkin - ignore
								return 0;
							}
							Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(commit.getCommitTime() * 1000);
							return calendar.get(Calendar.YEAR);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						if (walk != null)
							walk.release();
					}
				}
			}
		} finally {
			monitor.done();
		}

		return -1;
	}

	public void initialize(IProgressMonitor monitor) throws CoreException {
		// TODO We should perform a bulk "log" command to get the last modified year
	}

}
