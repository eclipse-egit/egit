/*******************************************************************************
 * Copyright (C) 2009, 2013 Yann Simon <yann.simon.fr@gmail.com> and others.
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

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter;

/**
 * Editable revision which supports listening to content changes by adding
 * {@link IContentChangeListener}.
 */
@SuppressWarnings("restriction")
public class EditableRevision extends FileRevisionTypedElement implements
		IEditableContent, IContentChangeNotifier {

	private final static class ContentChangeNotifier implements IContentChangeNotifier {

			private ListenerList fListenerList;
			private final IContentChangeNotifier element;

			public ContentChangeNotifier(IContentChangeNotifier element) {
				this.element = element;
			}

			/* (non-Javadoc)
			 * see IContentChangeNotifier.addChangeListener
			 */
			@Override
			public void addContentChangeListener(IContentChangeListener listener) {
				if (fListenerList == null)
					fListenerList= new ListenerList();
				fListenerList.add(listener);
			}

			/* (non-Javadoc)
			 * see IContentChangeNotifier.removeChangeListener
			 */
			@Override
			public void removeContentChangeListener(IContentChangeListener listener) {
				if (fListenerList != null) {
					fListenerList.remove(listener);
					if (fListenerList.isEmpty())
						fListenerList= null;
				}
			}

			/**
			 * Notifies all registered <code>IContentChangeListener</code>s of a content change.
			 */
			public void fireContentChanged() {
				if (isEmpty()) {
					return;
				}
				// Legacy listeners may expect to be notified in the UI thread.
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						Object[] listeners= fListenerList.getListeners();
						for (int i= 0; i < listeners.length; i++) {
							final IContentChangeListener contentChangeListener = (IContentChangeListener)listeners[i];
							SafeRunner.run(new ISafeRunnable() {
								@Override
								public void run() throws Exception {
									(contentChangeListener).contentChanged(element);
								}
								@Override
								public void handleException(Throwable exception) {
									// Logged by safe runner
								}
							});
						}
					}
				};
				if (Display.getCurrent() == null) {
					Display.getDefault().syncExec(runnable);
				} else {
					runnable.run();
				}
			}

			/**
			 * Return whether this notifier is empty (i.e. has no listeners).
			 * @return whether this notifier is empty
			 */
			public boolean isEmpty() {
				return fListenerList == null || fListenerList.isEmpty();
			}
	}

	private byte[] modifiedContent;

	private ContentChangeNotifier fChangeNotifier;

	private EditableSharedDocumentAdapter sharedDocumentAdapter;

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
	public Object getAdapter(Class adapter) {
		if (adapter == ISharedDocumentAdapter.class) {
			return getSharedDocumentAdapter();
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	private synchronized ISharedDocumentAdapter getSharedDocumentAdapter() {
		if (sharedDocumentAdapter == null)
			sharedDocumentAdapter = new EditableSharedDocumentAdapter(
					new EditableSharedDocumentAdapter.ISharedDocumentAdapterListener() {
						@Override
						public void handleDocumentConnected() {
							// nothing
						}

						@Override
						public void handleDocumentFlushed() {
							// nothing
						}

						@Override
						public void handleDocumentDeleted() {
							// nothing
						}

						@Override
						public void handleDocumentSaved() {
							// nothing
						}

						@Override
						public void handleDocumentDisconnected() {
							// nothing
						}
					});
		return sharedDocumentAdapter;
	}

	@Override
	public void addContentChangeListener(IContentChangeListener listener) {
		if (fChangeNotifier == null)
			fChangeNotifier = new ContentChangeNotifier(this);
		fChangeNotifier.addContentChangeListener(listener);
	}

	@Override
	public void removeContentChangeListener(IContentChangeListener listener) {
		if (fChangeNotifier != null) {
			fChangeNotifier.removeContentChangeListener(listener);
			if (fChangeNotifier.isEmpty())
				fChangeNotifier = null;
		}
	}

	/**
	 * Notifies all registered <code>IContentChangeListener</code>s of a content
	 * change.
	 */
	protected void fireContentChanged() {
		if (fChangeNotifier == null || fChangeNotifier.isEmpty()) {
			return;
		}
		fChangeNotifier.fireContentChanged();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
