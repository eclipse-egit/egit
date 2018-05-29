/*******************************************************************************
 * Copyright (C) 2017, Martin Fleck <mfleck@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * A tracker to be attached to workbench pages to track the latest selection.
 */
public class GitHistorySelectionTracker
		implements ISelectionListener, IPartListener, IPartListener2,
		IPageChangedListener {

	/** Last, tracked selection. */
	protected IStructuredSelection selection;

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		partActivated(partRef.getPart(false));
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		partBroughtToTop(partRef.getPart(false));
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		partClosed(partRef.getPart(false));
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		partDeactivated(partRef.getPart(false));
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		partOpened(partRef.getPart(false));
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// do nothing
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		// do nothing
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// do nothing
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part instanceof IEditorPart) {
			editorActivated((IEditorPart) part);
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		// do nothing
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		// do nothing
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		// do nothing
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		// do nothing
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		// support for multi-page editors
		if (event.getSelectedPage() instanceof IEditorPart) {
			editorActivated((IEditorPart) event.getSelectedPage());
		}
	}

	@SuppressWarnings({ "restriction" })
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection newSelection) {
		if (newSelection == null
				|| part instanceof org.eclipse.team.internal.ui.history.GenericHistoryView) {
			return;
		}
		IStructuredSelection structuredSelection = SelectionUtils
				.getStructuredSelection(newSelection);
		if (structuredSelection.isEmpty()) {
			return;
		}
		setSelection(structuredSelection);
	}

	/**
	 * Extracts the input from the activated editor and sets it as the last,
	 * tracked selection, if possible.
	 *
	 * @param editor
	 *            activated editor
	 */
	protected void editorActivated(IEditorPart editor) {
		IEditorInput editorInput = editor.getEditorInput();
		IResource resource = ResourceUtil.getResource(editorInput);
		if (resource != null) {
			setSelection(new StructuredSelection(resource));
		}
	}

	/**
	 * Attaches this tracker to the given workbench page. Any attached tracker
	 * should be {@link #detach(IWorkbenchPage) detached}, if no longer needed.
	 *
	 * @param page
	 *            workbench page
	 * @see #detach(IWorkbenchPage)
	 */
	public void attach(IWorkbenchPage page) {
		if (page != null) {
			page.addPartListener((IPartListener2) this);
			page.addSelectionListener(this);
		}
	}

	/**
	 * Detaches this tracker from the given workbench page. If this tracker has
	 * not been {@link #attach(IWorkbenchPage) attached} to the page previously,
	 * this call has no effect.
	 *
	 * @param page
	 *            workbench page
	 * @see #attach(IWorkbenchPage)
	 */
	public void detach(IWorkbenchPage page) {
		if (page != null) {
			page.removePartListener((IPartListener2) this);
			page.removeSelectionListener(this);
		}
	}

	/**
	 * Sets the last, tracked selection to the given selection.
	 *
	 * @param selection
	 *            selection
	 */
	private void setSelection(IStructuredSelection selection) {
		this.selection = selection;
	}

	/**
	 * Returns the last, tracked selection or null, if no selection has been
	 * tracked yet.
	 *
	 * @return last, tracked selection or null.
	 */
	public IStructuredSelection getSelection() {
		return selection;
	}

	/**
	 * Clears the last, tracked selection.
	 */
	public void clearSelection() {
		setSelection(null);
	}
}
