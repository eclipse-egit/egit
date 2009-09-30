/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;

/**
 * Content provided for push result table viewer.
 * <p>
 * Input of this provided must be {@link PushOperationResult} instance, while
 * returned elements are instances of {@link RefUpdateElement}. Null input is
 * allowed, resulting in no elements.
 *
 * @see PushOperationResult
 * @see RefUpdateElement
 */
class RefUpdateContentProvider implements IStructuredContentProvider {
	public Object[] getElements(final Object inputElement) {
		if (inputElement == null)
			return new RefUpdateElement[0];

		final PushOperationResult result = (PushOperationResult) inputElement;

		final SortedMap<String, String> dstToSrc = new TreeMap<String, String>();
		for (final URIish uri : result.getURIs()) {
			if (result.isSuccessfulConnection(uri)) {
				for (final RemoteRefUpdate rru : result.getPushResult(uri)
						.getRemoteUpdates())
					dstToSrc.put(rru.getRemoteName(), rru.getSrcRef());
				// Assuming that each repository received the same ref updates,
				// we need only one to get these ref names.
				break;
			}
		}

		// Transforming PushOperationResult model to row-wise one.
		final RefUpdateElement elements[] = new RefUpdateElement[dstToSrc
				.size()];
		int i = 0;
		for (final Entry<String, String> entry : dstToSrc.entrySet())
			elements[i++] = new RefUpdateElement(result, entry.getValue(),
					entry.getKey());
		return elements;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// nothing to do
	}

	public void dispose() {
		// nothing to dispose
	}
}