/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.WorkbenchStyledLabelProvider;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;

/**
 * Table displaying push operation results.
 */
class PushResultTable {
	private static final int TEXT_PREFERRED_HEIGHT = 100;

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String SPACE = " "; //$NON-NLS-1$

	private final TreeViewer treeViewer;

	private final Composite root;

	private final Image deleteImage;

	private ObjectReader reader;

	private Repository repo;

	PushResultTable(final Composite parent) {
		root = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(root);

		treeViewer = new TreeViewer(root);
		treeViewer.setAutoExpandLevel(2);

		addToolbar(root);

		ColumnViewerToolTipSupport.enableFor(treeViewer);
		final Tree table = treeViewer.getTree();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		table.setLinesVisible(true);

		deleteImage = UIIcons.ELCL16_DELETE.createImage();
		UIUtils.hookDisposal(root, deleteImage);

		root.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (reader != null)
					reader.release();
			}
		});

		treeViewer.setComparer(new IElementComparer() {
			// we need this to keep refresh() working while having custom
			// equals() in PushOperationResult
			public boolean equals(Object a, Object b) {
				return a == b;
			}

			public int hashCode(Object element) {
				return element.hashCode();
			}
		});
		final IStyledLabelProvider styleProvider = new WorkbenchStyledLabelProvider() {

			public StyledString getStyledText(Object element) {
				// TODO Replace with use of IWorkbenchAdapter3 when 3.6 is no
				// longer supported
				if (element instanceof RefUpdateElement)
					return ((RefUpdateElement) element).getStyledText(element);
				if (element instanceof RepositoryCommit)
					return ((RepositoryCommit) element).getStyledText(element);

				return super.getStyledText(element);
			}

		};

		treeViewer.setSorter(new ViewerSorter() {

			public int compare(Viewer viewer, Object e1, Object e2) {

				if (e1 instanceof RefUpdateElement
						&& e2 instanceof RefUpdateElement) {
					RefUpdateElement r1 = (RefUpdateElement) e1;
					RefUpdateElement r2 = (RefUpdateElement) e2;

					// Put rejected refs first
					if (r1.isRejected() && !r2.isRejected())
						return -1;
					if (!r1.isRejected() && r2.isRejected())
						return 1;

					// Put new refs next
					if (r1.isAdd() && !r2.isAdd())
						return -1;
					if (!r1.isAdd() && r2.isAdd())
						return 1;

					// Put branches before tags
					if (!r1.isTag() && r2.isTag())
						return -1;
					if (r1.isTag() && !r2.isTag())
						return 1;

					Status s1 = r1.getStatus();
					Status s2 = r2.getStatus();
					// Put up to date refs last
					if (s1 != Status.UP_TO_DATE && s2 == Status.UP_TO_DATE)
						return -1;
					if (s1 == Status.UP_TO_DATE && s2 != Status.UP_TO_DATE)
						return 1;

					String ref1 = r1.getDstRefName();
					String ref2 = r2.getDstRefName();
					if (ref1 != null && ref2 != null)
						return ref1.compareToIgnoreCase(ref2);
				}

				// Don't alter commit ordering
				if (e1 instanceof RepositoryCommit
						&& e2 instanceof RepositoryCommit)
					return 0;

				return super.compare(viewer, e1, e2);
			}

		});
		treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				styleProvider));
		treeViewer.setContentProvider(new RefUpdateContentProvider());
		// detail message
		Group messageGroup = new Group(root, SWT.NONE);
		messageGroup.setText(UIText.PushResultTable_MesasgeText);
		GridLayoutFactory.swtDefaults().applyTo(messageGroup);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
				.applyTo(messageGroup);

		final SpellcheckableMessageArea text = new SpellcheckableMessageArea(
				messageGroup, EMPTY_STRING, true, SWT.MULTI | SWT.BORDER | SWT.WRAP
						| SWT.V_SCROLL) {

			protected void createMarginPainter() {
				// Disabled intentionally
			}

		};
		GridDataFactory.fillDefaults().grab(true, true)
				.hint(SWT.DEFAULT, TEXT_PREFERRED_HEIGHT).applyTo(text);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (!(selection instanceof IStructuredSelection)) {
					text.setText(EMPTY_STRING);
					return;
				}
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				if (structuredSelection.size() != 1) {
					text.setText(EMPTY_STRING);
					return;
				}
				Object selected = structuredSelection.getFirstElement();
				if (selected instanceof RefUpdateElement)
					text.setText(getResult((RefUpdateElement) selected));
			}
		});

		treeViewer.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection)
					for (Object element : ((IStructuredSelection) selection)
							.toArray())
						if (element instanceof RepositoryCommit)
							CommitEditor.openQuiet((RepositoryCommit) element);
			}
		});
	}

	private void addToolbar(Composite parent) {
		ToolBar toolbar = new ToolBar(parent, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(toolbar);
		UIUtils.addExpansionItems(toolbar, treeViewer);
	}

	private String getResult(RefUpdateElement element) {
		StringBuilder result = new StringBuilder(EMPTY_STRING);
		PushOperationResult pushOperationResult = element
				.getPushOperationResult();
		final URIish uri = element.getUri();
		result.append(UIText.PushResultTable_repository);
		result.append(SPACE);
		result.append(uri.toString());
		result.append(Text.DELIMITER);
		result.append(Text.DELIMITER);
		String message = element.getRemoteRefUpdate().getMessage();
		if (message != null)
			result.append(message).append(Text.DELIMITER);
		StringBuilder messagesBuffer = new StringBuilder(pushOperationResult
				.getPushResult(uri).getMessages());
		trim(messagesBuffer);
		if (messagesBuffer.length() > 0)
			result.append(messagesBuffer).append(Text.DELIMITER);
		trim(result);
		return result.toString();
	}

	private static void trim(StringBuilder s) {
		// remove leading line breaks
		while (s.length() > 0 && (s.charAt(0) == '\n' || s.charAt(0) == '\r'))
			s.deleteCharAt(0);
		// remove trailing line breaks
		while (s.length() > 0
				&& (s.charAt(s.length() - 1) == '\n' || s
						.charAt(s.length() - 1) == '\r'))
			s.deleteCharAt(s.length() - 1);
	}

	void setData(final Repository localDb, final PushOperationResult result) {
		reader = localDb.newObjectReader();
		repo = localDb;

		// Set empty result for a while.
		treeViewer.setInput(null);

		if (result == null) {
			root.layout();
			return;
		}

		final List<RefUpdateElement> results = new ArrayList<RefUpdateElement>();

		for (URIish uri : result.getURIs())
			if (result.isSuccessfulConnection(uri))
				for (RemoteRefUpdate update : result.getPushResult(uri)
						.getRemoteUpdates())
					results.add(new RefUpdateElement(result, update, uri,
							reader, repo));

		treeViewer.setInput(results.toArray());
		// select the first row of table to get the details of the first
		// push result shown in the Text control
		Tree table = treeViewer.getTree();
		if (table.getItemCount() > 0)
			treeViewer.setSelection(new StructuredSelection(table.getItem(0)
					.getData()));
		root.layout();
	}

	Control getControl() {
		return root;
	}

}
