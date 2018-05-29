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

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
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

	@Override
	public void initialize(IManagedForm form) {
		toolkit = form.getToolkit();
	}

	@Override
	public void dispose() {
		// Does nothing
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void commit(boolean onSave) {
		// Update notes not currently supported
	}

	@Override
	public boolean setFormInput(Object input) {
		return true;
	}

	@Override
	public void setFocus() {
		notesText.setFocus();
	}

	@Override
	public boolean isStale() {
		return false;
	}

	@Override
	public void refresh() {
		if (selectedNote != null)
			notesText.setText(selectedNote.getNoteText());
		else
			notesText.setText(""); //$NON-NLS-1$
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object first = ((IStructuredSelection) selection).getFirstElement();
		if (first instanceof RepositoryCommitNote)
			selectedNote = (RepositoryCommitNote) first;
		else
			selectedNote = null;
		refresh();
	}

	@Override
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

			@Override
			protected void createMarginPainter() {
				// Disabled intentionally
			}

		};
		notesText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		StyledText text = notesText.getTextWidget();
		text.setEditable(false);
		text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		text.setForeground(text.getDisplay().getSystemColor(
				SWT.COLOR_INFO_FOREGROUND));
		text.setBackground(text.getDisplay().getSystemColor(
				SWT.COLOR_INFO_BACKGROUND));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(notesText);
	}

	@Override
	public Object getPageKey(Object object) {
		return this;
	}

	@Override
	public IDetailsPage getPage(Object key) {
		return this;
	}

}
