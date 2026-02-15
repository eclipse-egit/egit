/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.bitbucket.BitbucketClient;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Represents a file from Bitbucket for use in compare editors.
 * <p>
 * This implementation extends the pattern used by EGit's
 * {@code StorageTypedElement} to provide proper integration with Eclipse's
 * compare framework, including support for Java Source Compare and other
 * language-specific compare viewers.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Implements {@link IAdaptable} to support adapter queries</li>
 * <li>Provides {@link ISharedDocumentAdapter} for editor integration</li>
 * <li>Returns proper file type icons via {@link CompareUI#getImage(String)}</li>
 * <li>Supports charset/encoding detection</li>
 * </ul>
 */
public class BitbucketFileTypedElement
		implements ITypedElement, IEncodedStreamContentAccessor, IAdaptable {

	private final String projectKey;

	private final String repoSlug;

	private final String commitId;

	private final String path;

	private final String name;

	private final BitbucketClient client;

	private IStorage bufferedStorage;

	private ISharedDocumentAdapter sharedDocumentAdapter;

	/**
	 * Creates a new BitbucketFileTypedElement
	 *
	 * @param client
	 *            the Bitbucket client to use
	 * @param projectKey
	 *            the project key
	 * @param repoSlug
	 *            the repository slug
	 * @param commitId
	 *            the commit ID to fetch the file from
	 * @param path
	 *            the file path
	 */
	public BitbucketFileTypedElement(BitbucketClient client, String projectKey,
			String repoSlug, String commitId, String path) {
		this.client = client;
		this.projectKey = projectKey;
		this.repoSlug = repoSlug;
		this.commitId = commitId;
		this.path = path;
		// Extract file name from path
		int lastSlash = path.lastIndexOf('/');
		this.name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Image getImage() {
		// Return proper file type icon based on extension
		return CompareUI.getImage(getType());
	}

	@Override
	public String getType() {
		// Determine type from file extension
		int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0 && lastDot < name.length() - 1) {
			return name.substring(lastDot + 1);
		}
		return ITypedElement.TEXT_TYPE;
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (bufferedStorage == null) {
			cacheContents(new NullProgressMonitor());
		}
		if (bufferedStorage != null) {
			return bufferedStorage.getContents();
		}
		return null;
	}

	@Override
	public String getCharset() throws CoreException {
		if (bufferedStorage == null) {
			cacheContents(new NullProgressMonitor());
		}
		if (bufferedStorage instanceof IEncodedStorage) {
			return ((IEncodedStorage) bufferedStorage).getCharset();
		}
		// Default to UTF-8 for source files
		return "UTF-8"; //$NON-NLS-1$
	}

	/**
	 * Cache the contents for the remote resource in a local buffer. This method
	 * should be invoked before {@link #getContents()} to ensure that a round
	 * trip is not made in that method.
	 *
	 * @param monitor
	 *            a progress monitor.
	 * @throws CoreException
	 *             if fetching fails
	 */
	public void cacheContents(IProgressMonitor monitor) throws CoreException {
		bufferedStorage = fetchContents(monitor);
	}

	/**
	 * Fetches the file contents from Bitbucket.
	 *
	 * @param monitor
	 *            progress monitor
	 * @return storage containing the file contents
	 * @throws CoreException
	 *             if fetching fails
	 */
	protected IStorage fetchContents(IProgressMonitor monitor)
			throws CoreException {
		try {
			byte[] content = client.getFileContent(projectKey, repoSlug,
					commitId, path);
			return new BitbucketFileStorage(name, path, content);
		} catch (IOException e) {
			throw new CoreException(Status.error(
					"Failed to fetch file content from Bitbucket: " //$NON-NLS-1$
							+ e.getMessage(),
					e));
		}
	}

	/**
	 * @return the {@link IStorage} that has been buffered for this element
	 */
	public IStorage getBufferedStorage() {
		return bufferedStorage;
	}

	/**
	 * Get the file path
	 *
	 * @return the file path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the commit ID this file is from
	 *
	 * @return the commit ID
	 */
	public String getCommitId() {
		return commitId;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			synchronized (this) {
				if (sharedDocumentAdapter == null) {
					sharedDocumentAdapter = createSharedDocumentAdapter();
				}
				return (T) sharedDocumentAdapter;
			}
		}
		if (adapter == IStorage.class) {
			return (T) bufferedStorage;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 * Creates an {@link ISharedDocumentAdapter} for the element.
	 *
	 * @return the {@link ISharedDocumentAdapter}
	 */
	protected ISharedDocumentAdapter createSharedDocumentAdapter() {
		return new SharedDocumentAdapter() {

			@Override
			public IEditorInput getDocumentKey(Object element) {
				return BitbucketFileTypedElement.this.getDocumentKey(element);
			}

			@Override
			public void flushDocument(IDocumentProvider provider,
					IEditorInput documentKey, IDocument document,
					boolean overwrite) throws CoreException {
				// The document is read-only
			}
		};
	}

	/**
	 * Method called from the shared document adapter to get the document key.
	 *
	 * @param element
	 *            the element
	 * @return the document key (editor input)
	 */
	protected IEditorInput getDocumentKey(Object element) {
		if (element == this && bufferedStorage != null) {
			return new BitbucketFileEditorInput(this, bufferedStorage);
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commitId == null) ? 0 : commitId.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((projectKey == null) ? 0 : projectKey.hashCode());
		result = prime * result + ((repoSlug == null) ? 0 : repoSlug.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		BitbucketFileTypedElement other = (BitbucketFileTypedElement) obj;
		if (commitId == null) {
			if (other.commitId != null) {
				return false;
			}
		} else if (!commitId.equals(other.commitId)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.equals(other.path)) {
			return false;
		}
		if (projectKey == null) {
			if (other.projectKey != null) {
				return false;
			}
		} else if (!projectKey.equals(other.projectKey)) {
			return false;
		}
		if (repoSlug == null) {
			if (other.repoSlug != null) {
				return false;
			}
		} else if (!repoSlug.equals(other.repoSlug)) {
			return false;
		}
		return true;
	}

	/**
	 * Internal storage implementation for Bitbucket file contents.
	 */
	private static class BitbucketFileStorage implements IEncodedStorage {

		private final String name;

		private final String fullPath;

		private final byte[] content;

		BitbucketFileStorage(String name, String fullPath, byte[] content) {
			this.name = name;
			this.fullPath = fullPath;
			this.content = content;
		}

		@Override
		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(content);
		}

		@Override
		public IPath getFullPath() {
			return new Path(fullPath);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return null;
		}

		@Override
		public String getCharset() throws CoreException {
			// Default to UTF-8 for source files
			return "UTF-8"; //$NON-NLS-1$
		}
	}
}
