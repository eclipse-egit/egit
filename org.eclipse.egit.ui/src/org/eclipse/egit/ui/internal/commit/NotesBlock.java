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

import java.text.MessageFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Notes master details block class.
 */
public class NotesBlock extends MasterDetailsBlock {

	private RepositoryCommit commit;

	private IManagedForm form;

	private IFormPart part;

	private TableViewer refsViewer;

	/**
	 * Create notes block
	 *
	 * @param commit
	 */
	public NotesBlock(RepositoryCommit commit) {
		this.commit = commit;
	}

	@Override
	public void createContent(IManagedForm managedForm, Composite parent) {
		super.createContent(managedForm, parent);
		sashForm.setWeights(new int[] { 25, 75 });
	}

	@Override
	protected void createMasterPart(final IManagedForm managedForm,
			Composite parent) {
		form = managedForm;
		FormToolkit toolkit = managedForm.getToolkit();
		Composite masterArea = toolkit.createComposite(parent);
		GridLayoutFactory.swtDefaults().applyTo(masterArea);

		RepositoryCommitNote[] notes = commit.getNotes();

		Section refsSection = toolkit.createSection(masterArea,
				ExpandableComposite.TITLE_BAR);
		refsSection.setText(MessageFormat.format(
				UIText.NotesBlock_NotesSection, Integer.valueOf(notes.length)));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(refsSection);

		Composite refsArea = toolkit.createComposite(refsSection);
		refsSection.setClient(refsArea);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(refsArea);
		toolkit.paintBordersFor(refsArea);

		Table refsTable = toolkit.createTable(refsArea, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.SINGLE);
		refsViewer = new TableViewer(refsTable);
		refsViewer.setComparator(new ViewerComparator());
		refsTable.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(refsTable);
		refsViewer.setContentProvider(ArrayContentProvider.getInstance());
		refsViewer.setLabelProvider(new WorkbenchLabelProvider());
		refsViewer.setInput(notes);

		part = new SectionPart(refsSection);
		refsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(part, event.getSelection());
			}
		});
	}

	@Override
	protected void registerPages(DetailsPart dPart) {
		dPart.setPageProvider(new NoteDetailsPage());
	}

	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
		// No toolbar actions add by this block
	}

	/**
	 * Select first note ref in table
	 */
	public void selectFirstNote() {
		if (refsViewer.getTable().getItemCount() > 0) {
			refsViewer.getTable().setSelection(0);
			form.fireSelectionChanged(part, refsViewer.getSelection());
		}
	}
}
