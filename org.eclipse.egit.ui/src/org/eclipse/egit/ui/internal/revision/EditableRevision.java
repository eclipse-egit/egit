/*******************************************************************************
 * Copyright (C) 2009, 2019 Yann Simon <yann.simon.fr@gmail.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.egit.ui.internal.revision;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.SafeRunnable;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Editable revision which supports listening to content changes by adding
 * {@link IContentChangeListener}. Used for comparisons with editable index.
 */
public class EditableRevision extends FileRevisionTypedElement implements
		IEditableContent, IContentChangeNotifier {

	private final CopyOnWriteArrayList<IContentChangeListener> listeners = new CopyOnWriteArrayList<>();

	private byte[] modifiedContent;

	private IStorageEditorInput input;

	/**
	 * @param fileRevision
	 * @param encoding the file encoding
	 */
	public EditableRevision(IFileRevision fileRevision, String encoding) {
		super(fileRevision, encoding);
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		return null;
	}

	@Override
	public InputStream getContents() throws CoreException {
		if (modifiedContent != null) {
			return new ByteArrayInputStream(modifiedContent);
		}
		return super.getContents();
	}

	@Override
	public void setContent(byte[] newContent) {
		modifiedContent = newContent;
		fireContentChanged();
	}

	/**
	 * @return The modified content for reading. The data is owned by this
	 *         class, do not modify it.
	 */
	public byte[] getModifiedContent() {
		return modifiedContent;
	}

	@Override
	public IEditorInput getDocumentKey(Object element) {
		if (element == this) {
			if (input == null) {
				input = new IStorageEditorInput() {

					@Override
					public boolean exists() {
						return true;
					}

					@Override
					public ImageDescriptor getImageDescriptor() {
						return null;
					}

					@Override
					public String getName() {
						return EditableRevision.this.getName();
					}

					@Override
					public IPersistableElement getPersistable() {
						return null;
					}

					@Override
					public String getToolTipText() {
						return EditableRevision.this.getName();
					}

					@Override
					public <T> T getAdapter(Class<T> adapter) {
						return null;
					}

					private IStorage storage;

					@Override
					public IStorage getStorage() throws CoreException {
						if (storage == null) {
							storage = new IEncodedStorage() {

								@Override
								public <T> T getAdapter(Class<T> adapter) {
									return null;
								}

								@Override
								public boolean isReadOnly() {
									return false;
								}

								@Override
								public String getName() {
									return EditableRevision.this.getName();
								}

								@Override
								public IPath getFullPath() {
									return null;
								}

								@Override
								public InputStream getContents()
										throws CoreException {
									return EditableRevision.this.getContents();
								}

								@Override
								public String getCharset()
										throws CoreException {
									return EditableRevision.this.getCharset();
								}
							};
						}
						return storage;
					}
				};
			}
			return input;
		}
		return super.getDocumentKey(element);
	}

	@Override
	protected ISharedDocumentAdapter createSharedDocumentAdapter() {
		return new EditableRevisionSharedDocumentAdapter(this);
	}

	@Override
	public void addContentChangeListener(IContentChangeListener listener) {
		listeners.addIfAbsent(listener);
	}

	@Override
	public void removeContentChangeListener(IContentChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifies all registered <code>IContentChangeListener</code>s of a content
	 * change.
	 */
	protected void fireContentChanged() {
		if (listeners.isEmpty()) {
			return;
		}
		// Legacy listeners may expect to be notified in the UI thread.
		Runnable runnable = () -> {
			for (IContentChangeListener listener : listeners) {
				SafeRunnable.run(
						() -> listener.contentChanged(EditableRevision.this));
			}
		};
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(runnable);
		} else {
			runnable.run();
		}
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@SuppressWarnings("restriction")
	private static class EditableRevisionSharedDocumentAdapter
			extends
			org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter {

		private final EditableRevision editable;

		public EditableRevisionSharedDocumentAdapter(
				EditableRevision editable) {
			super(new ISharedDocumentAdapterListener() {

				@Override
				public void handleDocumentConnected() {
					// Nothing
				}

				@Override
				public void handleDocumentDisconnected() {
					// Nothing
				}

				@Override
				public void handleDocumentFlushed() {
					// Nothing
				}

				@Override
				public void handleDocumentDeleted() {
					// Nothing
				}

				@Override
				public void handleDocumentSaved() {
					// Nothing
				}
			});
			this.editable = editable;
		}

		@Override
		public void flushDocument(IDocumentProvider provider,
				IEditorInput documentKey, IDocument document, boolean overwrite)
				throws CoreException {
			super.flushDocument(provider, documentKey, document, overwrite);
			if (document != null && editable.input != null) {
				try {
					String charset = editable.getCharset();
					if (charset == null) {
						editable.setContent(document.get().getBytes());
					} else {
						editable.setContent(document.get().getBytes(charset));
					}
					// We _know_ that the document provider _cannot_ really save
					// the IStorage. Nevertheless calling its save operation is
					// necessary because otherwise an internal "modified" flag
					// inside the document provider is not re-set. As a
					// consequence the dirty state in the merge viewer would
					// become inconsistent with the dirty state of the document,
					// and subsequent changes would not be saved.
					saveDocument(documentKey, overwrite, null);
				} catch (UnsupportedEncodingException e) {
					throw new CoreException(
							Activator.createErrorStatus(
									MessageFormat.format(
											UIText.EditableRevision_CannotSave,
											editable.getName(),
											editable.getContentIdentifier()),
									e));
				}
			}
		}

		@Override
		public IEditorInput getDocumentKey(Object element) {
			return editable.getDocumentKey(element);
		}
	}

}
