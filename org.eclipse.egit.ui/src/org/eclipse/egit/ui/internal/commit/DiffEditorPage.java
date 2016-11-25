/*******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Diff editor page class for displaying a {@link DiffViewer}.
 */
public class DiffEditorPage extends FormPage {

	private static class TextViewerAction extends Action implements IUpdate {

		private int code = -1;

		private ITextOperationTarget target;

		public TextViewerAction(ITextViewer viewer, int operationCode) {
			code = operationCode;
			target = viewer.getTextOperationTarget();
			update();
		}

		@Override
		public void update() {
			if (code == ITextOperationTarget.REDO)
				return;

			boolean wasEnabled = isEnabled();
			boolean isEnabled = target.canDoOperation(code);
			setEnabled(isEnabled);

			if (wasEnabled != isEnabled)
				firePropertyChange(ENABLED, wasEnabled ? Boolean.TRUE
						: Boolean.FALSE, isEnabled ? Boolean.TRUE
						: Boolean.FALSE);
		}

		@Override
		public void run() {
			if (code != -1)
				target.doOperation(code);
		}
	}

	private DiffViewer viewer;

	private DiffStyleRangeFormatter formatter;

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public DiffEditorPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/**
	 * @param editor
	 */
	public DiffEditorPage(FormEditor editor) {
		this(editor, "diffPage", UIText.DiffEditorPage_Title); //$NON-NLS-1$
	}

	/**
	 * @param commit
	 * @return diffs for changes of of a commit
	 */
	protected FileDiff[] getDiffs(RepositoryCommit commit) {
		List<FileDiff> diffResult = new ArrayList<>();

		diffResult.addAll(asList(commit.getDiffs()));

		if (commit.getRevCommit().getParentCount() > 2) {
			RevCommit untrackedCommit = commit.getRevCommit().getParent(
					StashEditorPage.PARENT_COMMIT_UNTRACKED);
			diffResult.addAll(asList(new RepositoryCommit(commit
					.getRepository(), untrackedCommit).getDiffs()));
		}
		Collections.sort(diffResult, FileDiff.PATH_COMPARATOR);
		return diffResult.toArray(new FileDiff[0]);
	}

	private void formatDiff() {
		final DiffDocument document = new DiffDocument();
		formatter = new DiffStyleRangeFormatter(document);

		Job job = new Job(UIText.DiffEditorPage_TaskGeneratingDiff) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				RepositoryCommit commit = AdapterUtils.adapt(getEditor(),
						RepositoryCommit.class);
				if (commit == null) {
					return Status.CANCEL_STATUS;
				}
				FileDiff diffs[] = getDiffs(commit);
				monitor.beginTask("", diffs.length); //$NON-NLS-1$
				Repository repository = commit.getRepository();
				for (FileDiff diff : diffs) {
					if (monitor.isCanceled())
						break;
					monitor.setTaskName(diff.getPath());
					try {
						formatter.write(repository, diff);
					} catch (IOException ignore) {
						// Ignored
					}
					monitor.worked(1);
				}
				monitor.done();
				new UIJob(UIText.DiffEditorPage_TaskUpdatingViewer) {

					@Override
					public IStatus runInUIThread(IProgressMonitor uiMonitor) {
						if (UIUtils.isUsable(viewer)) {
							document.connect(formatter);
							viewer.setDocument(document);
						}
						return Status.OK_STATUS;
					}
				}.schedule();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/**
	 * Add editor actions to menu manager
	 *
	 * @param manager
	 */
	protected void addEditorActions(MenuManager manager) {
		final TextViewerAction copyAction = new TextViewerAction(viewer,
				ITextOperationTarget.COPY);
		copyAction.setText(UIText.SpellCheckingMessageArea_copy);
		copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

		final TextViewerAction selectAllAction = new TextViewerAction(viewer,
				ITextOperationTarget.SELECT_ALL);
		selectAllAction.setText(UIText.SpellCheckingMessageArea_selectAll);
		selectAllAction
				.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

		manager.add(copyAction);
		manager.add(selectAllAction);
		manager.add(new Separator());

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				copyAction.update();
				selectAllAction.update();
			}
		});
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(body);

		viewer = new DiffViewer(body, new CompositeRuler(), SWT.V_SCROLL
				| SWT.H_SCROLL, true);
		viewer.setEditable(false);
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(viewer.getControl());

		MenuManager manager = new MenuManager();
		addEditorActions(manager);
		Menu menu = manager.createContextMenu(viewer.getTextWidget());
		IEditorSite site = getEditorSite();
		site.setSelectionProvider(viewer);
		site.registerContextMenu(
				AbstractTextEditor.COMMON_EDITOR_CONTEXT_MENU_ID, manager,
				viewer, true);
		site.registerContextMenu(
				AbstractTextEditor.DEFAULT_EDITOR_CONTEXT_MENU_ID, manager,
				viewer, true);
		viewer.getTextWidget().setMenu(menu);

		formatDiff();
	}
}
