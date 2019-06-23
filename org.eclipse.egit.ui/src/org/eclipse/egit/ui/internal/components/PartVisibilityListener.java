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
package org.eclipse.egit.ui.internal.components;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * Maintains a simple boolean flag telling whether the view is currently hidden
 * or visible. Needs to be registered and unregistered in
 * {@link org.eclipse.ui.IPartService}. Assumes the view is visible initially.
 */
public abstract class PartVisibilityListener implements IPartListener2 {

	private final IWorkbenchPart myself;

	private boolean viewVisible = true;

	/**
	 * Creates a new {@link PartVisibilityListener} with an initial state of
	 * "visible"
	 *
	 * @param part
	 */
	public PartVisibilityListener(IWorkbenchPart part) {
		myself = part;
	}

	/**
	 * Whether the the part this {@link PartVisibilityListener} was instantiated
	 * with is currently visible.
	 *
	 * @return {@code true} if the part is visible, {@cod false} otherwise.
	 */
	public boolean isVisible() {
		return viewVisible;
	}

	private void updateHiddenState(IWorkbenchPartReference partRef,
			boolean visible) {
		if (isMe(partRef)) {
			viewVisible = visible;
		}
	}

	/**
	 * Determines whether the {@code partRef} is for the part this
	 * {@link PartVisibilityListener} was instantiated with.
	 *
	 * @param partRef
	 *            to check
	 * @return {@code true} if the {@code partRef} is for the part this
	 *         {@link PartVisibilityListener} was instantiated with,
	 *         {@code false} otherwise
	 */
	protected final boolean isMe(IWorkbenchPartReference partRef) {
		return partRef.getPart(false) == myself;
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		updateHiddenState(partRef, false);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		updateHiddenState(partRef, false);
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		updateHiddenState(partRef, true);
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		updateHiddenState(partRef, true);
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		// Nothing to do
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// Nothing to do
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// Nothing to do
	}

}
