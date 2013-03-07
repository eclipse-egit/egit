/*******************************************************************************
 * Copyright (C) 2010, 2012 Jens Baumgart <jens.baumgart@sap.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.dialogs.CheckoutConflictDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Dialog for displaying a MergeResult
 *
 */
public class MergeResultDialog extends Dialog {

	private static final String SPACE = " "; //$NON-NLS-1$

	private final MergeResult mergeResult;

	private final Repository repository;

	private ObjectReader objectReader;

	/**
	 * @param parentShell
	 * @param repository
	 * @param mergeResult
	 * @return the created dialog
	 */
	public static Dialog getDialog(Shell parentShell, Repository repository, MergeResult mergeResult) {
		if(mergeResult.getMergeStatus() == MergeStatus.CHECKOUT_CONFLICT)
			return new CheckoutConflictDialog(parentShell, repository, mergeResult.getCheckoutConflicts());
		else
			return new MergeResultDialog(parentShell, repository, mergeResult);
	}

	/**
	 * @param parentShell
	 * @param repository
	 * @param mergeResult
	 */
	public MergeResultDialog(Shell parentShell, Repository repository,
			MergeResult mergeResult) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.repository = repository;
		this.mergeResult = mergeResult;
		objectReader = repository.newObjectReader();
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
	}

	@Override
	public Control createDialogArea(final Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		// result
		Label resultLabel = new Label(composite, SWT.NONE);
		resultLabel.setText(UIText.MergeResultDialog_result);
		resultLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
				false));
		Text resultText = new Text(composite, SWT.READ_ONLY);
		resultText.setText(mergeResult.getMergeStatus().toString());
		resultText.setSelection(resultText.getCaretPosition());
		resultText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (mergeResult.getMergeStatus() == MergeStatus.FAILED) {
			resultText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));

			StringBuilder paths = new StringBuilder();
			Label pathsLabel = new Label(composite, SWT.NONE);
			pathsLabel.setText(UIText.MergeResultDialog_failed);
			pathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			Text pathsText = new Text(composite, SWT.READ_ONLY);
			pathsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			Set<Entry<String, MergeFailureReason>> failedPaths = mergeResult.getFailingPaths().entrySet();
			int n = 0;
			for (Map.Entry<String, MergeFailureReason> e : failedPaths) {
				if (n > 0)
					paths.append(Text.DELIMITER);
				paths.append(e.getValue());
				paths.append("\t"); //$NON-NLS-1$
				paths.append(e.getKey());
				n++;
				if (n > 10 && failedPaths.size() > 15)
					break;
			}
			if (n < failedPaths.size()) {
				paths.append(Text.DELIMITER);
				paths.append(MessageFormat.format(UIText.MergeResultDialog_nMore, Integer.valueOf(n - failedPaths.size())));
			}
			pathsText.setText(paths.toString());
		}

		if (mergeResult.getMergeStatus() != MergeStatus.FAILED) {
			// new head
			Label newHeadLabel = new Label(composite, SWT.NONE);
			newHeadLabel.setText(UIText.MergeResultDialog_newHead);
			newHeadLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
					false));
			Text newHeadText = new Text(composite, SWT.READ_ONLY);
			ObjectId newHead = mergeResult.getNewHead();
			if (newHead != null)
				newHeadText.setText(getCommitMessage(newHead) + SPACE
						+ abbreviate(mergeResult.getNewHead(), true));
			newHeadText
					.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		// Merge Input
		Label mergeInputLabel = new Label(composite, SWT.NONE);
		mergeInputLabel.setText(UIText.MergeResultDialog_mergeInput);
		GridDataFactory.fillDefaults().align(SWT.LEAD, SWT.CENTER).span(2, 1)
				.applyTo(mergeInputLabel);
		TableViewer viewer = new TableViewer(composite);
		viewer.setContentProvider(new IStructuredContentProvider() {

			public void dispose() {
				// empty
			}

			public void inputChanged(Viewer theViewer, Object oldInput,
					Object newInput) {
				// empty
			}

			public Object[] getElements(Object inputElement) {
				return getCommits(mergeResult.getMergedCommits());
			}
		});
		Table table = viewer.getTable();
		table.setLinesVisible(true);
		final IStyledLabelProvider styleProvider = new IStyledLabelProvider() {

			private final WorkbenchLabelProvider wrapped = new WorkbenchLabelProvider();

			public void removeListener(ILabelProviderListener listener) {
				// Empty
			}

			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			public void dispose() {
				wrapped.dispose();
			}

			public void addListener(ILabelProviderListener listener) {
				// Empty
			}

			public StyledString getStyledText(Object element) {
				// TODO Replace with use of IWorkbenchAdapter3 when is no longer
				// supported
				if (element instanceof RepositoryCommit)
					return ((RepositoryCommit) element).getStyledText(element);

				return new StyledString(wrapped.getText(element));
			}

			public Image getImage(Object element) {
				return wrapped.getImage(element);
			}
		};
		viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				styleProvider));
		applyDialogFont(composite);
		GridDataFactory.fillDefaults().grab(true, true)
				.align(SWT.FILL, SWT.FILL).span(2, 1)
				.applyTo(viewer.getControl());
		viewer.setInput(mergeResult);

		new OpenAndLinkWithEditorHelper(viewer) {
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

		return composite;
	}

	private RepositoryCommit[] getCommits(final ObjectId[] merges) {
		final List<RepositoryCommit> commits = new ArrayList<RepositoryCommit>();
		final RevWalk walk = new RevWalk(objectReader);
		walk.setRetainBody(true);
		for (ObjectId merge : merges)
			try {
				commits.add(new RepositoryCommit(repository, walk
						.parseCommit(merge)));
			} catch (IOException e) {
				Activator.logError(MessageFormat.format(
						UIText.MergeResultDialog_couldNotFindCommit,
						merge.name()), e);
			}
		return commits.toArray(new RepositoryCommit[commits.size()]);
	}

	private String getCommitMessage(ObjectId id) {
		RevCommit commit;
		try {
			commit = new RevWalk(objectReader).parseCommit(id);
		} catch (IOException e) {
			Activator.logError(UIText.MergeResultDialog_couldNotFindCommit, e);
			return UIText.MergeResultDialog_couldNotFindCommit;
		}
		return commit.getShortMessage();
	}

	private String abbreviate(ObjectId id, boolean addBrackets) {
		StringBuilder result = new StringBuilder();
		if (addBrackets)
			result.append("["); //$NON-NLS-1$
		try {
			result.append(objectReader.abbreviate(id).name());
		} catch (IOException e) {
			result.append(id.name());
		}
		if (addBrackets)
			result.append("]"); //$NON-NLS-1$
		return result.toString();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.MergeAction_MergeResultTitle);
		newShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (objectReader != null)
					objectReader.release();
			}
		});
	}

	protected IDialogSettings getDialogBoundsSettings() {
		return UIUtils.getDialogBoundSettings(getClass());
	}
}
