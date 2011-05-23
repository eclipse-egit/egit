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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.commit.command.CreateBranchHandler;
import org.eclipse.egit.ui.internal.commit.command.CreateTagHandler;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.SharedHeaderFormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.progress.UIJob;

/**
 * Editor class to view a commit in a form editor.
 */
public class CommitEditor extends SharedHeaderFormEditor implements
		RefsChangedListener {

	/**
	 * ID - editor id
	 */
	public static final String ID = "org.eclipse.egit.ui.commitEditor"; //$NON-NLS-1$

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @return opened editor part
	 * @throws PartInitException
	 */
	public static final IEditorPart open(RepositoryCommit commit)
			throws PartInitException {
		CommitEditorInput input = new CommitEditorInput(commit);
		return IDE.openEditor(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage(), input, ID);
	}

	/**
	 * Open commit in editor
	 *
	 * @param commit
	 * @return opened editor part or null if opening fails
	 */
	public static final IEditorPart openQuiet(RepositoryCommit commit) {
		try {
			return open(commit);
		} catch (PartInitException e) {
			Activator.logError(e.getMessage(), e);
			return null;
		}
	}

	private CommitEditorPage commitPage;

	private DiffEditorPage diffPage;

	private NotesEditorPage notePage;

	private ListenerHandle refListenerHandle;

	/**
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	protected void addPages() {
		try {
			commitPage = new CommitEditorPage(this);
			addPage(commitPage);
			if (getCommit().getRevCommit().getParentCount() == 1) {
				diffPage = new DiffEditorPage(this);
				addPage(diffPage);
			}
			notePage = new NotesEditorPage(this);
			addPage(notePage);
		} catch (PartInitException e) {
			Activator.error("Error adding page", e); //$NON-NLS-1$
		}
		refListenerHandle = Repository.getGlobalListenerList()
				.addRefsChangedListener(this);
	}

	private CommandContributionItem createCommandContributionItem(
			String commandId) {
		CommandContributionItemParameter parameter = new CommandContributionItemParameter(
				getSite(), commandId, commandId,
				CommandContributionItem.STYLE_PUSH);
		return new CommandContributionItem(parameter);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.SharedHeaderFormEditor#createHeaderContents(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createHeaderContents(IManagedForm headerForm) {
		RepositoryCommit commit = getCommit();
		ScrolledForm form = headerForm.getForm();
		form.setText(MessageFormat.format(UIText.CommitEditor_TitleHeader,
				commit.getRepositoryName(), commit.getRevCommit().name()));
		form.setToolTipText(commit.getRevCommit().name());
		getToolkit().decorateFormHeading(form.getForm());

		IToolBarManager toolbar = form.getToolBarManager();
		toolbar.add(createCommandContributionItem(CreateTagHandler.ID));
		toolbar.add(createCommandContributionItem(CreateBranchHandler.ID));
		toolbar.update(true);
	}

	private RepositoryCommit getCommit() {
		return (RepositoryCommit) getAdapter(RepositoryCommit.class);
	}

	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (RepositoryCommit.class == adapter)
			return getEditorInput().getAdapter(adapter);

		return super.getAdapter(adapter);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (input.getAdapter(RepositoryCommit.class) == null) {
			throw new PartInitException(
					"Input could not be adapted to commit object"); //$NON-NLS-1$
		}
		super.init(site, input);
		setPartName(input.getName());
		setTitleToolTip(input.getToolTipText());
	}

	public void dispose() {
		refListenerHandle.remove();
		super.dispose();
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		// Save not supported
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		// Save as not supported
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	public void onRefsChanged(RefsChangedEvent event) {
		if (getCommit().getRepository().getDirectory()
				.equals(event.getRepository().getDirectory())) {
			UIJob job = new UIJob("Refreshing editor") { //$NON-NLS-1$

				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (!getContainer().isDisposed())
						commitPage.refresh();
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}
}
