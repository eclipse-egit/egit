/*******************************************************************************
 * Copyright (C) 2008, 2015 Marek Zawirski <marek.zawirski@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.WorkbenchStyledLabelProvider;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.model.IWorkbenchAdapter3;

/**
 * Table displaying push operation results.
 */
class PushResultTable {

	private final static class SpellcheckableMessageAreaExtension
			extends SpellcheckableMessageArea {
		private SpellcheckableMessageAreaExtension(Composite parent,
				String initialText, boolean readOnly, int styles) {
			super(parent, initialText, readOnly, styles);
		}

		@Override
		protected void createMarginPainter() {
			// Disabled intentionally
		}
	}

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String SPACE = " "; //$NON-NLS-1$

	private static final String SASH_WEIGHTS_SETTING = "sashWeights"; //$NON-NLS-1$

	private final TreeViewer treeViewer;

	private final SashForm root;

	private final Image deleteImage;

	private ObjectReader reader;

	private Repository repo;

	PushResultTable(final Composite parent) {
		this(parent, null);
	}

	PushResultTable(final Composite parent,
			final IDialogSettings dialogSettings) {
		root = new SashForm(parent, SWT.VERTICAL);

		Composite treeContainer = new Composite(root, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(treeContainer);
		treeViewer = new TreeViewer(treeContainer);
		treeViewer.setAutoExpandLevel(2);

		addToolbar(treeContainer);

		ColumnViewerToolTipSupport.enableFor(treeViewer);
		final Tree table = treeViewer.getTree();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		deleteImage = UIIcons.ELCL16_DELETE.createImage();
		UIUtils.hookDisposal(root, deleteImage);

		root.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				saveDialogSettings(root, dialogSettings);
				if (reader != null)
					reader.close();
			}
		});

		treeViewer.setComparer(new IElementComparer() {
			// we need this to keep refresh() working while having custom
			// equals() in PushOperationResult
			@Override
			public boolean equals(Object a, Object b) {
				return a == b;
			}

			@Override
			public int hashCode(Object element) {
				return element.hashCode();
			}
		});
		final IStyledLabelProvider styleProvider = new WorkbenchStyledLabelProvider() {

			@Override
			public StyledString getStyledText(Object element) {
				if (element instanceof IWorkbenchAdapter3)
					return ((IWorkbenchAdapter3) element).getStyledText(element);

				return super.getStyledText(element);
			}

		};

		treeViewer.setComparator(new ViewerComparator() {

			@Override
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

		final SpellcheckableMessageArea text = new SpellcheckableMessageAreaExtension(messageGroup, EMPTY_STRING, true,
				SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(text);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
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

		initializeSashWeights(root, new int[] { 3, 2 }, dialogSettings);

		new OpenAndLinkWithEditorHelper(treeViewer) {
			@Override
			protected void linkToEditor(ISelection selection) {
				// Not supported
			}
			@Override
			protected void open(ISelection selection, boolean activate) {
				handleOpen(selection, OpenStrategy.activateOnOpen());
			}
			@Override
			protected void activate(ISelection selection) {
				handleOpen(selection, true);
			}
			private void handleOpen(ISelection selection, boolean activateOnOpen) {
				if (selection instanceof IStructuredSelection)
					for (Object element : ((IStructuredSelection) selection)
							.toArray())
						if (element instanceof RepositoryCommit)
							CommitEditor.openQuiet((RepositoryCommit) element, activateOnOpen);
			}
		};

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

	private static void saveDialogSettings(SashForm sashForm,
			IDialogSettings dialogSettings) {
		if (dialogSettings != null) {
			int[] weights = sashForm.getWeights();
			String[] weightStrings = new String[weights.length];
			for (int i = 0; i < weights.length; i++) {
				weightStrings[i] = String.valueOf(weights[i]);
			}
			dialogSettings.put(SASH_WEIGHTS_SETTING, weightStrings);
		}
	}

	private static void initializeSashWeights(SashForm sashForm,
			int[] defaultValues, IDialogSettings dialogSettings) {
		if (dialogSettings != null) {
			String[] weightStrings = dialogSettings
					.getArray(SASH_WEIGHTS_SETTING);
			if (weightStrings != null
					&& weightStrings.length == defaultValues.length) {
				try {
					int[] weights = new int[weightStrings.length];
					for (int i = 0; i < weights.length; i++) {
						weights[i] = Integer.parseInt(weightStrings[i]);
					}
					sashForm.setWeights(weights);
					return;
				} catch (NumberFormatException ignore) { // bad settings
					dialogSettings.put(SASH_WEIGHTS_SETTING, (String[]) null);
				}
			}
		}
		sashForm.setWeights(defaultValues);
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

		final List<RefUpdateElement> results = new ArrayList<>();

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
