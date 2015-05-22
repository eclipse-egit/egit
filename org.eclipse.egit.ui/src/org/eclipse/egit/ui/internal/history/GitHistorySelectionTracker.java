/*******************************************************************************
 * Copyright (C) 2016, Martin Fleck <mfleck@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.internal.ui.history.GenericHistoryView;
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
@SuppressWarnings("restriction")
public class GitHistorySelectionTracker
		implements ISelectionListener, IPartListener, IPartListener2 {

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

	@SuppressWarnings({ "hiding" })
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection == null || part instanceof GenericHistoryView) {
			return;
		}
		IStructuredSelection structuredSelection = SelectionUtils
				.getStructuredSelection(selection);
		System.out.println("selectionChanged[" + part + "]: " //$NON-NLS-1$ //$NON-NLS-2$
				+ structuredSelection.size());
		if (structuredSelection.isEmpty()) {
			return;
		}
		setSelection(structuredSelection);
	}

	/**
	 *
	 * @param editor
	 */
	protected void editorActivated(IEditorPart editor) {
		IEditorInput editorInput = editor.getEditorInput();
		IFile file = ResourceUtil.getFile(editorInput);
		if (file != null) {
			setSelection(new StructuredSelection(file));
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
