/*******************************************************************************
 * Copyright (C) 2019, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.egit.ui.internal.DecorationOverlayDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * Adapts {@link FileDiff}s.
 */
public class FileDiffWorkbenchAdapter extends WorkbenchAdapter {

	/**
	 * The singleton instance of {@link FileDiffWorkbenchAdapter}.
	 */
	public static final FileDiffWorkbenchAdapter INSTANCE = new FileDiffWorkbenchAdapter();

	private FileDiffWorkbenchAdapter() {
		// Singleton.
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof FileDiff) {
			FileDiff diff = (FileDiff) object;
			ImageDescriptor base = diff.getBaseImageDescriptor();
			// TODO: move the decoration to an asynchronous decorator?
			ImageDescriptor decoration = diff.getImageDcoration();
			if (decoration != null) {
				return new DecorationOverlayDescriptor(base, decoration,
						IDecoration.BOTTOM_RIGHT);
			}
			return base;
		}
		return null;
	}

	@Override
	public String getLabel(Object object) {
		if (object instanceof FileDiff) {
			return ((FileDiff) object).getPath();
		}
		return null;
	}
}
