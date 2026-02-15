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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

/**
 * Editor input for Bitbucket file revisions.
 * <p>
 * This class provides the bridge between the compare framework's
 * {@link org.eclipse.compare.ISharedDocumentAdapter} and Eclipse's editor
 * infrastructure. It allows Eclipse to associate proper document providers
 * (e.g., Java document provider) with the file content, enabling features like
 * Java Source Compare.
 * </p>
 */
public class BitbucketFileEditorInput extends PlatformObject
		implements IStorageEditorInput {

	private final BitbucketFileTypedElement element;

	private final IStorage storage;

	/**
	 * Creates a new editor input.
	 *
	 * @param element
	 *            the typed element this input is for
	 * @param storage
	 *            the storage containing the file contents
	 */
	public BitbucketFileEditorInput(BitbucketFileTypedElement element,
			IStorage storage) {
		this.element = element;
		this.storage = storage;
	}

	@Override
	public IStorage getStorage() throws CoreException {
		return storage;
	}

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
		return element.getName() + " [" + element.getCommitId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IPersistableElement getPersistable() {
		// Cannot persist - this is a remote file
		return null;
	}

	@Override
	public String getToolTipText() {
		return element.getPath() + " @ " + element.getCommitId(); //$NON-NLS-1$
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IStorage.class) {
			return adapter.cast(storage);
		}
		return super.getAdapter(adapter);
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof BitbucketFileEditorInput) {
			BitbucketFileEditorInput other = (BitbucketFileEditorInput) obj;
			return element.equals(other.element);
		}
		return false;
	}
}
