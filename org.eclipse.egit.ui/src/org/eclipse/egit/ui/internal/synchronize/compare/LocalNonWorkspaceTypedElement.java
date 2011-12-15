/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation of some methods
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;

/**
 * Specialized resource node for non-workspace files
 */
public class LocalNonWorkspaceTypedElement extends LocalResourceTypedElement {

	private final String path;

	private boolean exists;

	private boolean fDirty = false;

	private boolean useSharedDocument = true;

	private EditableSharedDocumentAdapter sharedDocumentAdapter;

	private EditableSharedDocumentAdapter.ISharedDocumentAdapterListener sharedDocumentListener;

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	/**
	 * @param path absolute path to non-workspace file
	 */
	public LocalNonWorkspaceTypedElement(String path) {
		super(ROOT.getFile(new Path(path)));
		this.path = path;

		exists = new File(path).exists();
	}

	@Override
	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(path);
		} catch (FileNotFoundException e) {
			Activator.error(e.getMessage(), e);
		}

		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEditable() {
		IResource resource = getResource();
		return resource.getType() == IResource.FILE && exists;
	}

	/** {@inheritDoc} */
	@Override
	public void update() {
		exists = getResource().getFullPath().toFile().exists();
	}

	/** {@inheritDoc} */
	@Override
	public boolean exists() {
		return exists;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isSharedDocumentsEnable() {
		return useSharedDocument && getResource().getType() == IResource.FILE && exists;
	}

	/** {@inheritDoc} */
	@Override
	public void enableSharedDocument(boolean enablement) {
		this.useSharedDocument = enablement;
	}

	/** {@inheritDoc} */
	@Override
	public void setContent(byte[] contents) {
		fDirty = true;
		super.setContent(contents);
	}

	/** {@inheritDoc} */
	@Override
	public void commit(IProgressMonitor monitor) throws CoreException {
		if (isDirty()) {
			if (isConnected()) {
				super.commit(monitor);
			} else {
				IResource resource = getResource();
				if (resource instanceof IFile) {
					FileOutputStream out = null;
					File file = ((IFile) resource).getFullPath().toFile();
					try {
						if (!file.exists())
							file.createNewFile();
						out = new FileOutputStream(file);
						out.write(getContent());
						fDirty = false;
					} catch (FileNotFoundException e) {
						throw new CoreException(null);
					} catch (IOException e) {
						throw new CoreException(null);
					} finally {
						fireContentChanged();
						RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
						if (mapping != null) {
							mapping.getRepository().fireEvent(new IndexChangedEvent());
						}
						if (out != null)
							try {
								out.close();
							} catch (IOException ex) {
							}
					}
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean isDirty() {
		return fDirty || (sharedDocumentAdapter != null && sharedDocumentAdapter.hasBufferedContents());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			if (isSharedDocumentsEnable())
				return getSharedDocumentAdapter();
			else
				return null;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public void setSharedDocumentListener(
			EditableSharedDocumentAdapter.ISharedDocumentAdapterListener sharedDocumentListener) {
		this.sharedDocumentListener = sharedDocumentListener;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + path.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalNonWorkspaceTypedElement other = (LocalNonWorkspaceTypedElement) obj;
		if (!path.equals(other.path))
			return false;
		return true;
	}

	/*
	 * Returned the shared document adapter for this element. If one does not exist
	 * yet, it will be created.
	 */
	private synchronized ISharedDocumentAdapter getSharedDocumentAdapter() {
		if (sharedDocumentAdapter == null)
			sharedDocumentAdapter = new EditableSharedDocumentAdapter(new EditableSharedDocumentAdapter.ISharedDocumentAdapterListener() {
				public void handleDocumentConnected() {
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentConnected();
				}
				public void handleDocumentFlushed() {
					fireContentChanged();
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentFlushed();
				}
				public void handleDocumentDeleted() {
					LocalNonWorkspaceTypedElement.this.update();
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentDeleted();
				}
				public void handleDocumentSaved() {
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentSaved();
				}
				public void handleDocumentDisconnected() {
					if (sharedDocumentListener != null)
						sharedDocumentListener.handleDocumentDisconnected();
				}
			});
		return sharedDocumentAdapter;
	}

}
