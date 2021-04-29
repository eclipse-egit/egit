/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.efs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.efs.EgitFileSystem.UriComponents;
import org.eclipse.jgit.util.StringUtils;

/**
 * An EFS {@link FileStore} for conflicting working tree files, filtered to the
 * pre-merged 'ours' version.
 */
class FilteredWorktreeFileStore extends FileStore {

	private final IFileStore base;

	private final UriComponents uri;

	private final boolean diff3Style;

	private final int conflictMarkerSize;

	/**
	 * Creates a new {@link FilteredWorktreeFileStore}.
	 *
	 * @param uri
	 *            describing the store
	 * @throws URISyntaxException
	 *             if the uri contains unknown arguments
	 */
	public FilteredWorktreeFileStore(UriComponents uri)
			throws URISyntaxException {
		try {
			String args = uri.getArguments();
			if (StringUtils.isEmptyOrNull(args)) {
				throw new URISyntaxException(uri.toString(),
						"Missing selector arguments"); //$NON-NLS-1$
			}
			switch (args.charAt(0)) {
			case 'o':
				diff3Style = false;
				break;
			case 'O':
				diff3Style = true;
				break;
			default:
				throw new URISyntaxException(uri.toString(),
						MessageFormat.format("Unknown selector arguments {0}", //$NON-NLS-1$
								args));
			}
			int markerSize = 7; // Git default
			if (args.length() > 1) {
				try {
					markerSize = Integer.parseUnsignedInt(args.substring(1));
				} catch (NumberFormatException e) {
					throw new URISyntaxException(uri.toString(),
							MessageFormat.format(
									"Unknown selector arguments {0}", //$NON-NLS-1$
									args));
				}
			}
			base = uri.getBaseFile();
			conflictMarkerSize = markerSize;
		} catch (IOException e) {
			URISyntaxException ue = new URISyntaxException(uri.toString(),
					"Cannot determine repository"); //$NON-NLS-1$
			ue.initCause(e);
			throw ue;
		}
		this.uri = uri;
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor)
			throws CoreException {
		return base.childNames(options, monitor);
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor)
			throws CoreException {
		return base.fetchInfo(options, monitor);
	}

	@Override
	public IFileStore getChild(String name) {
		return base.getChild(name);
	}

	@Override
	public String getName() {
		return base.getName();
	}

	@Override
	public IFileStore getParent() {
		// Beware: this means this != this.getParent().getChild(this.getName()).
		return base.getParent();
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor)
			throws CoreException {
		return new OursVersionInputStream(
				base.openInputStream(options, monitor), conflictMarkerSize,
				diff3Style);
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor)
			throws CoreException {
		return base.openOutputStream(options, monitor);
	}

	@Override
	public URI toURI() {
		return uri.toUri();
	}

	@Override
	public void delete(int options, IProgressMonitor monitor)
			throws CoreException {
		base.delete(options, monitor);
	}
}
