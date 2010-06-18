/*******************************************************************************
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.Iterator;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

class CommitFileDiffViewer extends TableViewer {
	private TreeWalk walker;

	private Clipboard clipboard;

	CommitFileDiffViewer(final Composite parent) {
		super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.FULL_SELECTION);

		final Table rawTable = getTable();
		rawTable.setHeaderVisible(true);
		rawTable.setLinesVisible(true);

		final TableLayout layout = new TableLayout();
		rawTable.setLayout(layout);
		createColumns(rawTable, layout);

		setLabelProvider(new FileDiffLabelProvider());
		setContentProvider(new FileDiffContentProvider());
		addOpenListener(new IOpenListener() {
			public void open(final OpenEvent event) {
				final ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection))
					return;
				final IStructuredSelection iss = (IStructuredSelection) s;
				final FileDiff d = (FileDiff) iss.getFirstElement();
				if (walker != null && d.blobs.length == 2)
					showTwoWayFileDiff(d);
			}
		});

		clipboard = new Clipboard(rawTable.getDisplay());
		rawTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				clipboard.dispose();
			}
		});
	}

	void showTwoWayFileDiff(final FileDiff d) {
		final GitCompareFileRevisionEditorInput in;

		final Repository db = walker.getRepository();
		final String p = d.path;
		final RevCommit c = d.commit;
		final ITypedElement base;
		final ITypedElement next;

		base = CompareUtils.getFileRevisionTypedElement(p, c.getParent(0), db, d.blobs[0]);
		next = CompareUtils.getFileRevisionTypedElement(p, c, db, d.blobs[1]);

		in = new GitCompareFileRevisionEditorInput(base, next, null);
		CompareUI.openCompareEditor(in);
	}

	TreeWalk getTreeWalk() {
		return walker;
	}

	void setTreeWalk(final TreeWalk walk) {
		walker = walk;
	}

	void doSelectAll() {
		final IStructuredContentProvider cp;
		final Object in = getInput();
		if (in == null)
			return;

		cp = ((IStructuredContentProvider) getContentProvider());
		final Object[] el = cp.getElements(in);
		if (el == null || el.length == 0)
			return;
		setSelection(new StructuredSelection(el));
	}

	void doCopy() {
		final ISelection s = getSelection();
		if (s.isEmpty() || !(s instanceof IStructuredSelection))
			return;
		final IStructuredSelection iss = (IStructuredSelection) s;
		final Iterator<FileDiff> itr = iss.iterator();
		final StringBuilder r = new StringBuilder();
		while (itr.hasNext()) {
			final FileDiff d = itr.next();
			if (r.length() > 0)
				r.append("\n"); //$NON-NLS-1$
			r.append(d.path);
		}

		clipboard.setContents(new Object[] { r.toString() },
				new Transfer[] { TextTransfer.getInstance() }, DND.CLIPBOARD);
	}

	private void createColumns(final Table rawTable, final TableLayout layout) {
		final TableColumn mode = new TableColumn(rawTable, SWT.NONE);
		mode.setResizable(true);
		mode.setText(""); //$NON-NLS-1$
		mode.setWidth(5);
		layout.addColumnData(new ColumnWeightData(1, true));

		final TableColumn path = new TableColumn(rawTable, SWT.NONE);
		path.setResizable(true);
		path.setText(UIText.HistoryPage_pathnameColumn);
		path.setWidth(250);
		layout.addColumnData(new ColumnWeightData(20, true));
	}
}
