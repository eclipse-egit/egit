/*******************************************************************************
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.ContentChangeNotifier;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter;

/**
 * @author simon
 *
 */
public class EditableRevision extends FileRevisionTypedElement implements
		ITypedElement, IEditableContent, IContentChangeNotifier {

	private byte[] modifiedContent;

	private ContentChangeNotifier fChangeNotifier;

	private EditableSharedDocumentAdapter sharedDocumentAdapter;

	/**
	 * @param fileRevision
	 */
	public EditableRevision(IFileRevision fileRevision) {
		super(fileRevision);
	}

	public boolean isEditable() {
		return true;
	}

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

	public void setContent(byte[] newContent) {
		modifiedContent = newContent;
		fireContentChanged();
	}

	/**
	 * @return the modified content
	 */
	public byte[] getModifiedContent() {
		return modifiedContent;
	}

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
						public void handleDocumentConnected() {
						}

						public void handleDocumentFlushed() {
						}

						public void handleDocumentDeleted() {
						}

						public void handleDocumentSaved() {
						}

						public void handleDocumentDisconnected() {
						}
					});
		return sharedDocumentAdapter;
	}

	public void addContentChangeListener(IContentChangeListener listener) {
		if (fChangeNotifier == null)
			fChangeNotifier = new ContentChangeNotifier(this);
		fChangeNotifier.addContentChangeListener(listener);
	}

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

}
