/*******************************************************************************
 *  Copyright (c) 2011, 2020 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - turn it into a real text editor
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.core.resources.IMarker;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;

/**
 * A {@link DiffEditor} wrapped as an {@link IFormPage} and specialized to
 * showing a unified diff of a whole commit. The editor has an associated
 * {@link DiffEditorOutlinePage}.
 */
public class DiffEditorPage extends DiffEditor implements IFormPage {

	private FormEditor formEditor;

	private String title;

	private String pageId;

	private int pageIndex = -1;

	/**
	 * Creates a new {@link DiffEditorPage} with the given id and title, which
	 * is shown on the page's tab.
	 *
	 * @param editor
	 *            containing this page
	 * @param id
	 *            of the page
	 * @param title
	 *            of the page
	 */
	public DiffEditorPage(FormEditor editor, String id, String title) {
		super();
		pageId = id;
		this.title = title;
		setPartName(title);
		initialize(editor);
	}

	/**
	 * Creates a new {@link DiffEditorPage} with default id and title.
	 *
	 * @param editor
	 *            containing the page
	 */
	public DiffEditorPage(FormEditor editor) {
		this(editor, "diffPage", UIText.DiffEditor_Title); //$NON-NLS-1$
	}

	@Override
	public void dispose() {
		// Nested editors are responsible for disposing their outline pages
		// themselves.
		DiffEditorOutlinePage outlinePage = getOutlinePage();
		if (outlinePage != null) {
			outlinePage.dispose();
			outlinePage = null;
		}
		super.dispose();
	}

	@Override
	protected void setSelectionAndActivate(ISelection selection,
			boolean activate) {
		if (activate) {
			FormEditor editor = getEditor();
			editor.getSite().getPage().activate(editor);
			editor.setActivePage(getId());
		}
		doSetSelection(selection);
	}

	@Override
	public String getTitle() {
		// We don't want the title to be overridden by the DiffEditorInput
		return title;
	}

	// FormPage specifics:

	@Override
	public void initialize(FormEditor editor) {
		formEditor = editor;
	}

	@Override
	public FormEditor getEditor() {
		return formEditor;
	}

	@Override
	public IManagedForm getManagedForm() {
		return null;
	}

	@Override
	public void setActive(boolean active) {
		if (active) {
			setFocus();
		}
	}

	@Override
	public boolean isActive() {
		return this.equals(formEditor.getActivePageInstance());
	}

	@Override
	public boolean canLeaveThePage() {
		return true;
	}

	@Override
	public Control getPartControl() {
		return getSourceViewer().getTextWidget();
	}

	@Override
	public String getId() {
		return pageId;
	}

	@Override
	public int getIndex() {
		return pageIndex;
	}

	@Override
	public void setIndex(int index) {
		pageIndex = index;
	}

	@Override
	public boolean isEditor() {
		return true;
	}

	@Override
	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
			return true;
		} else if (object instanceof ISelection) {
			doSetSelection((ISelection) object);
			return true;
		}
		return false;
	}
}
