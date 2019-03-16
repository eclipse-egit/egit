/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.history.IFileRevision;

/** An {@link IFileRevision} for the current version in the working tree */
public class WorkingTreeFileRevision extends GitFileRevision {

	private final File file;

	/**
	 * @param file
	 */
	public WorkingTreeFileRevision(final File file) {
		super(file.getPath());
		this.file = file;
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		return new IStorage() {

			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public String getName() {
				return file.getName();
			}

			@Override
			public IPath getFullPath() {
				return new Path(file.getAbsolutePath());
			}

			@Override
			public InputStream getContents() throws CoreException {
				try {
					return Files.newInputStream(file.toPath());
				} catch (IOException e) {
					throw new CoreException(Activator.error(e.getMessage(), e));
				}
			}
		};
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	@Override
	public String getAuthor() {
		return "";  //$NON-NLS-1$
	}

	@Override
	public long getTimestamp() {
		return -1;
	}

	@Override
	public String getComment() {
		return "";  //$NON-NLS-1$
	}

	@Override
	public String getContentIdentifier() {
		return WORKING_TREE;
	}

	@Override
	public Repository getRepository() {
		return ResourceUtil
				.getRepository(Path.fromOSString(file.getAbsolutePath()));
	}
}
