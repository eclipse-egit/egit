/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.ide.IDE;

/**
 * Editor class to view a commit in a form editor.
 */
public class CommitEditor extends SharedHeaderFormEditor {

	/**
	 * ID - editor id
	 */
	public static final String ID = "org.eclipse.egit.ui.commitEditor"; //$NON-NLS-1$

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @throws PartInitException
	 */
	public static final void open(RepositoryCommit commit)
			throws PartInitException {
		CommitEditorInput input = new CommitEditorInput(commit);
		IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage(), input, ID);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	protected void addPages() {
		try {
			addPage(new CommitEditorPage(this));
			addPage(new DiffEditorPage(this));
		} catch (PartInitException e) {
			Activator.error("Error adding page", e); //$NON-NLS-1$
		}
	}

	/**
	 * @see org.eclipse.ui.forms.editor.SharedHeaderFormEditor#createHeaderContents(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createHeaderContents(IManagedForm headerForm) {
		RepositoryCommit commit = getCommit();
		ScrolledForm form = headerForm.getForm();
		form.setText(MessageFormat.format(UIText.CommitEditor_TitleHeader,
				commit.getRepositoryName(), commit.abbreviate()));
		form.setToolTipText(commit.getRevCommit().name());
		getToolkit().decorateFormHeading(form.getForm());
	}

	private RepositoryCommit getCommit() {
		return (RepositoryCommit) getAdapter(RepositoryCommit.class);
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (RepositoryCommit.class == adapter)
			return getEditorInput().getAdapter(adapter);

		return super.getAdapter(adapter);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input.getAdapter(RepositoryCommit.class) == null) {
			throw new PartInitException(
					"Input could not be adapted to commit object"); //$NON-NLS-1$
		}
		super.init(site, input);
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		// Save not supported
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		// Save as not supported
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

}
