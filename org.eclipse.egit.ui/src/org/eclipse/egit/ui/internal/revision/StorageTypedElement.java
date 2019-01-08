/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.revision;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Abstract Storage-based element
 */
abstract class StorageTypedElement implements ITypedElement,
		IEncodedStreamContentAccessor, IAdaptable {

	private IStorage bufferedContents;

	private final String localEncoding;

	private ISharedDocumentAdapter sharedDocumentAdapter;

	/**
	 * @param localEncoding
	 */
	public StorageTypedElement(String localEncoding) {
		this.localEncoding = localEncoding;
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (bufferedContents == null) {
			cacheContents(new NullProgressMonitor());
		}
		if (bufferedContents != null) {
			return bufferedContents.getContents();
		}
		return null;
	}

	/**
	 * Cache the contents for the remote resource in a local buffer. This method
	 * should be invoked before {@link #getContents()} to ensure that a round
	 * trip is not made in that method.
	 *
	 * @param monitor
	 *            a progress monitor.
	 * @throws CoreException
	 */
	public void cacheContents(IProgressMonitor monitor) throws CoreException {
		bufferedContents = fetchContents(monitor);
	}

	/**
	 * @param monitor
	 * @return a storage for the element
	 * @throws CoreException
	 * @throws TeamException
	 */
	abstract protected IStorage fetchContents(IProgressMonitor monitor)
			throws CoreException;

	/**
	 * @return the {@link IStorage} that has been buffered for this element
	 */
	public IStorage getBufferedStorage() {
		return bufferedContents;
	}

	@Override
	public Image getImage() {
		return CompareUI.getImage(getType());
	}

	@Override
	public String getType() {
		String name = getName();
		if (name != null) {
			int index = name.lastIndexOf('.');
			if (index == -1)
				return ""; //$NON-NLS-1$
			if (index == (name.length() - 1))
				return ""; //$NON-NLS-1$
			return name.substring(index + 1);
		}
		return ITypedElement.FOLDER_TYPE;
	}

	@Override
	public String getCharset() throws CoreException {
		if (localEncoding != null)
			return localEncoding;
		if (bufferedContents == null) {
			cacheContents(new NullProgressMonitor());
		}
		if (bufferedContents instanceof IEncodedStorage) {
			String charset = ((IEncodedStorage) bufferedContents).getCharset();
			return charset;
		}
		return null;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			synchronized (this) {
				if (sharedDocumentAdapter == null) {
					sharedDocumentAdapter = createSharedDocumentAdapter();
				}
				return sharedDocumentAdapter;
			}
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
				return StorageTypedElement.this.getDocumentKey(element);
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
	 * @return the document key
	 */
	protected abstract IEditorInput getDocumentKey(Object element);

	/**
	 * @return the encoding
	 */
	public String getLocalEncoding() {
		return localEncoding;
	}
}
