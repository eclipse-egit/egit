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

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.UIUtils;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.lib.Repository;
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

	private void formatDiff() {
		final IDocument document = new Document();
		formatter = new DiffStyleRangeFormatter(document);
		viewer.setFormatter(formatter);

		Job job = new Job(UIText.DiffEditorPage_TaskGeneratingDiff) {

			protected IStatus run(IProgressMonitor monitor) {
				RepositoryCommit commit = (RepositoryCommit) getEditor()
						.getAdapter(RepositoryCommit.class);
				FileDiff diffs[] = commit.getDiffs();
				monitor.beginTask("", diffs.length); //$NON-NLS-1$
				Repository repository = commit.getRepository();
				for (FileDiff diff : diffs) {
					if (monitor.isCanceled())
						break;
					monitor.setTaskName(diff.getPath());
					if (diff.getBlobs().length == 2)
						try {
							formatter.write(repository, diff);
						} catch (IOException ignore) {
							// Ignored
						}
					monitor.worked(1);
				}
				monitor.done();
				new UIJob(UIText.DiffEditorPage_TaskUpdatingViewer) {

					public IStatus runInUIThread(IProgressMonitor uiMonitor) {
						if (UIUtils.isUsable(viewer)) {
							viewer.setDocument(document);
							viewer.refreshStyleRanges();
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

			public void selectionChanged(SelectionChangedEvent event) {
				copyAction.update();
				selectAllAction.update();
			}
		});
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(body);

		viewer = new DiffViewer(body, new CompositeRuler(), SWT.V_SCROLL
				| SWT.H_SCROLL);
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
