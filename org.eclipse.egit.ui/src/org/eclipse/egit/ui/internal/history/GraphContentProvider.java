/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

class GraphContentProvider implements IStructuredContentProvider {
	private SWTCommit[] list;

	@Override
	public void inputChanged(final Viewer newViewer, final Object oldInput,
			final Object newInput) {
		list = (SWTCommit[]) newInput;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		return list;
	}

	@Override
	public void dispose() {
		// Nothing.
	}
}
