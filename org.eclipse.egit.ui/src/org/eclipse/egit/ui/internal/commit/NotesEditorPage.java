/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

/**
 * Editor page class to display commit notes.
 */
public class NotesEditorPage extends FormPage {

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public NotesEditorPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/**
	 * @param editor
	 */
	public NotesEditorPage(FormEditor editor) {
		this(editor, "notePage", UIText.NotesEditorPage_Title); //$NON-NLS-1$
	}

	/**
	 * Get commit being displayed
	 *
	 * @return commit
	 */
	protected RepositoryCommit getCommit() {
		return Adapters.adapt(getEditor(), RepositoryCommit.class);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(body);

		NotesBlock block = new NotesBlock(getCommit());
		block.createContent(managedForm, body);
		block.selectFirstNote();
	}
}
