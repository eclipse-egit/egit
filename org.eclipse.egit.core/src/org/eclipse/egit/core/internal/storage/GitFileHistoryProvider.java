/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

/**
 * A {@link FileHistoryProvider} for Git. This class has methods for retrieving
 * specific versions of a tracked resource.
 */
public class GitFileHistoryProvider extends FileHistoryProvider {
	@Override
	public IFileHistory getFileHistoryFor(IResource resource, int flags,
			IProgressMonitor monitor) {
		return new GitFileHistory(resource, flags, monitor);
	}

	@Override
	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		return new WorkspaceFileRevision(resource);
	}

	@Override
	public IFileHistory getFileHistoryFor(IFileStore store, int flags,
			IProgressMonitor monitor) {
		// TODO: implement getFileHistoryFor(IFileStore ...)
		return null;
	}
}
