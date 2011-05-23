/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Note details page that display the text content of a note in a
 * {@link StyledText} widget.
 */
public class NoteDetailsPage implements IDetailsPage, IDetailsPageProvider {

	private SpellcheckableMessageArea notesText;

	private FormToolkit toolkit;

	private RepositoryCommitNote selectedNote;

	public void initialize(IManagedForm form) {
		toolkit = form.getToolkit();
	}

	public void dispose() {
		// Does nothing
	}

	public boolean isDirty() {
		return false;
	}

	public void commit(boolean onSave) {
		// Update notes not currently supported
	}

	public boolean setFormInput(Object input) {
		return true;
	}

	public void setFocus() {
		notesText.setFocus();
	}

	public boolean isStale() {
		return false;
	}

	public void refresh() {
		if (selectedNote != null)
			notesText.setText(selectedNote.getNoteText());
		else
			notesText.setText(""); //$NON-NLS-1$
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		Object first = ((IStructuredSelection) selection).getFirstElement();
		if (first instanceof RepositoryCommitNote)
			selectedNote = (RepositoryCommitNote) first;
		else
			selectedNote = null;
		refresh();
	}

	public void createContents(Composite parent) {
		GridLayoutFactory.swtDefaults().applyTo(parent);
		Section notesSection = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR);
		notesSection.setText(UIText.NoteDetailsPage_ContentSection);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(notesSection);

		Composite notesArea = toolkit.createComposite(notesSection);
		toolkit.paintBordersFor(notesArea);
		notesSection.setClient(notesArea);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(notesArea);

		notesText = new SpellcheckableMessageArea(notesArea, "", SWT.NONE) { //$NON-NLS-1$

			protected void createMarginPainter() {
				// Disabled intentionally
			}

		};
		notesText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(notesText);
	}

	public Object getPageKey(Object object) {
		return this;
	}

	public IDetailsPage getPage(Object key) {
		return this;
	}

}
