/*******************************************************************************
 * Copyright (C) 2011, 2019 Dariusz Luksza <dariusz@luksza.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation of some methods
 *     Thomas Wolf <thomas.wolf@paranor.ch> - Bugs 474981, 481682, 543495
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.events.IndexChangedEvent;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Specialized resource node for non-workspace files
 */
@SuppressWarnings("restriction")
public class LocalNonWorkspaceTypedElement extends LocalResourceTypedElement {

	@NonNull
	private final IPath path;

	@NonNull
	private final Repository repository;

	private final boolean isSymlink;

	private final boolean isFile;

	private boolean exists;

	private boolean fDirty;

	private long timestamp;

	private boolean useSharedDocument = true;

	private EditableSharedDocumentAdapter sharedDocumentAdapter;

	private EditableSharedDocumentAdapter.ISharedDocumentAdapterListener sharedDocumentListener;

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	// We cannot rely on the implementation in BufferedContent and just override
	// createStream() because that fake IResource we pass to our super class
	// doesn't exist, which confuses the super implementations of related
	// operations.
	private byte[] modifiedContent;

	/**
	 * @param repository
	 *            the file belongs to
	 * @param path
	 *            absolute path to non-workspace file
	 */
	public LocalNonWorkspaceTypedElement(@NonNull Repository repository,
			@NonNull IPath path) {
		super(ROOT.getFile(path));
		this.path = path;
		this.repository = repository;

		File file = path.toFile();
		isSymlink = Files.isSymbolicLink(file.toPath());
		isFile = file.isFile();
		exists = isFile || isSymlink || file.exists();
		if (exists) {
			timestamp = file.lastModified();
		}
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (modifiedContent != null) {
			return new ByteArrayInputStream(modifiedContent);
		}
		return createStream();
	}

	@Override
	public void setContent(byte[] contents) {
		fDirty = true;
		modifiedContent = contents;
		fireContentChanged();
	}

	@Override
	public byte[] getContent() {
		if (modifiedContent == null) {
			try {
				InputStream is = createStream();
				modifiedContent = Utilities.readBytes(is);
			} catch (CoreException e) {
				Activator.handleStatus(e.getStatus(), false);
			}
		}
		return modifiedContent;
	}

	@Override
	public void discardBuffer() {
		super.discardBuffer();
		if (sharedDocumentAdapter != null) {
			sharedDocumentAdapter.releaseBuffer();
		}
		modifiedContent = null;
	}

	@Override
	protected InputStream createStream() throws CoreException {
		if (exists) {
			try {
				File file = path.toFile();
				timestamp = file.lastModified();
				if (isSymlink) {
					String symLink = FileUtils.readSymLink(file);
					return new ByteArrayInputStream(Constants.encode(symLink));
				}
				if (file.isDirectory()) {
					// submodule
					Repository sub = ResourceUtil.getRepository(path);
					if (sub != null && sub != repository) {
						RevCommit headCommit = Activator.getDefault()
								.getRepositoryUtil().parseHeadCommit(sub);
						if (headCommit == null) {
							return null;
						}
						return new ByteArrayInputStream(Constants
								.encode(headCommit.name()));
					}
				}
				return Files.newInputStream(file.toPath());
			} catch (IOException | UnsupportedOperationException e) {
				Activator.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEditable() {
		return exists && (isFile || isSymlink);
	}

	@Override
	public long getModificationDate() {
		return timestamp;
	}

	@Override
	public boolean isSynchronized() {
		return path.toFile().lastModified() == timestamp;
	}

	/** {@inheritDoc} */
	@Override
	public void update() {
		exists = path.toFile().exists();
	}

	/** {@inheritDoc} */
	@Override
	public boolean exists() {
		return exists;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isSharedDocumentsEnable() {
		return useSharedDocument && !isSymlink && isEditable();
	}

	/** {@inheritDoc} */
	@Override
	public void enableSharedDocument(boolean enablement) {
		this.useSharedDocument = enablement;
	}

	private void refreshTimestamp() {
		timestamp = path.toFile().lastModified();
	}

	/** {@inheritDoc} */
	@Override
	public void commit(IProgressMonitor monitor) throws CoreException {
		if (isDirty()) {
			if (isConnected()) {
				super.commit(monitor);
			} else {
				File file = path.toFile();
				try {
					byte[] contents = getContent();
					if (isSymlink) {
						String sp = new String(contents,
								StandardCharsets.UTF_8)
								.trim();
						if (sp.indexOf('\n') > 0) {
							sp = sp.substring(0, sp.indexOf('\n')).trim();
						}
						if (!sp.isEmpty()) {
							boolean wasBrokenLink = !file.exists();
							java.nio.file.Path link = FileUtils
									.createSymLink(file, sp);
							// If link state changes, Eclipse can't realize this
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=290318
							updateLinkResource(wasBrokenLink, link);
						}
					} else {
						try (OutputStream out = Files
								.newOutputStream(file.toPath())) {
							out.write(contents);
						}
					}
					fDirty = false;
				} catch (IOException e) {
					throw new CoreException(
							new Status(
									IStatus.ERROR,
									Activator.getPluginId(),
									UIText.LocalNonWorkspaceTypedElement_errorWritingContents,
									e));
				} finally {
					fireContentChanged();
					updateGitState();
				}
			}
			refreshTimestamp();
		}
	}

	private void updateLinkResource(boolean wasBroken,
			java.nio.file.Path link) {
		boolean brokenNow = !Files.exists(link);
		if (brokenNow == wasBroken) {
			// If the state doesn't change, we don't care, either Eclipse
			// doesn's see broken link and we can't do anything or it is not
			// broken and Eclipse handles the change
			return;
		}
		// refresh the parent if either the link was broken before or broken
		// just now
		IPath parentPath = path.removeLastSegments(1);
		@SuppressWarnings("null")
		final IContainer parent = ResourceUtil
				.getContainerForLocation(parentPath, true);
		if (parent != null) {
			WorkspaceJob job = new WorkspaceJob("Refreshing " + parentPath) { //$NON-NLS-1$
				@Override
				public IStatus runInWorkspace(IProgressMonitor m)
						throws CoreException {
					parent.refreshLocal(IResource.DEPTH_ONE, m);
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}

	private void updateGitState() {
		// external file change must be reported explicitly, see bug 481682
		Repository myRepository = repository;
		boolean updated = false;
		if (!myRepository.isBare()) {
			updated = refreshRepositoryState(myRepository);
		}
		if (!updated) {
			RepositoryMapping mapping = RepositoryMapping.getMapping(path);
			if (mapping != null) {
				mapping.getRepository().fireEvent(new IndexChangedEvent(true));
			}
		}
	}

	private boolean refreshRepositoryState(@NonNull Repository repo) {
		IPath repositoryRoot = new Path(repo.getWorkTree().getPath());
		IPath relativePath = path.makeRelativeTo(repositoryRoot);
		IndexDiffCacheEntry indexDiffCacheForRepository = org.eclipse.egit.core.Activator
				.getDefault().getIndexDiffCache().getIndexDiffCacheEntry(repo);
		if (indexDiffCacheForRepository != null) {
			indexDiffCacheForRepository.refreshFiles(
					Collections.singleton(relativePath.toString()));
			return true;
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized boolean isDirty() {
		return fDirty || (sharedDocumentAdapter != null && sharedDocumentAdapter.hasBufferedContents());
	}

	@Override
	public boolean isConnected() {
		return sharedDocumentAdapter != null
				&& sharedDocumentAdapter.isConnected();
	}

	@Override
	public boolean saveDocument(boolean overwrite, IProgressMonitor monitor)
			throws CoreException {
		if (isConnected()) {
			IEditorInput input = sharedDocumentAdapter.getDocumentKey(this);
			sharedDocumentAdapter.saveDocument(input, overwrite, monitor);
			updateGitState();
			refreshTimestamp();
			return true;
		}
		return false;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			if (isSharedDocumentsEnable()) {
				return getSharedDocumentAdapter();
			}
			return null;
		}
		return Platform.getAdapterManager().getAdapter(this,
				(Class<?>) adapter);
	}

	@Override
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
		if (sharedDocumentAdapter == null) {
			sharedDocumentAdapter = new EditableSharedDocumentAdapter(
					new EditableSharedDocumentAdapter.ISharedDocumentAdapterListener() {
						@Override
						public void handleDocumentConnected() {
							refreshTimestamp();
							if (sharedDocumentListener != null) {
								sharedDocumentListener
										.handleDocumentConnected();
							}
						}

						@Override
						public void handleDocumentFlushed() {
							fireContentChanged();
							if (sharedDocumentListener != null) {
								sharedDocumentListener.handleDocumentFlushed();
							}
						}

						@Override
						public void handleDocumentDeleted() {
							update();
							if (sharedDocumentListener != null) {
								sharedDocumentListener.handleDocumentDeleted();
							}
						}

						@Override
						public void handleDocumentSaved() {
							updateGitState();
							refreshTimestamp();
							if (sharedDocumentListener != null) {
								sharedDocumentListener.handleDocumentSaved();
							}
						}

						@Override
						public void handleDocumentDisconnected() {
							if (sharedDocumentListener != null) {
								sharedDocumentListener
										.handleDocumentDisconnected();
							}
						}
					}) {

				@Override
				public IEditorInput getDocumentKey(Object element) {
					if (element == LocalNonWorkspaceTypedElement.this) {
						IFileStore store = EFS.getLocalFileSystem().getStore(
								LocalNonWorkspaceTypedElement.this.path);
						if (store != null) {
							return new FakeResourceFileStoreEditorInput(store,
									LocalNonWorkspaceTypedElement.this
											.getResource());
						}
					}
					return super.getDocumentKey(element);
				}

				@Override
				public void connect(IDocumentProvider provider,
						IEditorInput documentKey) throws CoreException {
					if (documentKey instanceof FakeResourceFileStoreEditorInput) {
						// When we connect, our editor input shouldn't adapt to
						// that (non-existing) resource, otherwise we'll confuse
						// other parts of Eclipse.
						FakeResourceFileStoreEditorInput input = (FakeResourceFileStoreEditorInput) documentKey;
						try {
							input.setResource(null);
							super.connect(provider, input);
						} finally {
							// Once we _are_ connected, there are other places
							// where SharedDocumentAdapter.getDocumentProvider()
							// is called again during the life of the document,
							// so the documentKey must again adapt to IFile.
							input.setResource(LocalNonWorkspaceTypedElement.this
									.getResource());
						}
					} else {
						super.connect(provider, documentKey);
					}
				}
			};
		}
		return sharedDocumentAdapter;
	}

	private static class FakeResourceFileStoreEditorInput
			extends FileStoreEditorInput {

		// This class and the connect() override above are a work-around for bug
		// 544315: the file extension is used to find the document provider only
		// if the editor input adapts to IFile.

		private IResource resource;

		public FakeResourceFileStoreEditorInput(IFileStore store, IResource resource) {
			super(store);
			this.resource = resource;
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IFile.class || adapter == IResource.class) {
				if (resource != null && adapter.isInstance(resource)) {
					return adapter.cast(resource);
				}
			}
			return super.getAdapter(adapter);
		}

		public void setResource(IResource resource) {
			this.resource = resource;
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return super.equals(o);
		}
	}
}
