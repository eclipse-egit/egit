/*******************************************************************************
 *  Copyright (c) 2011, 2012 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    François Rey - gracefully ignore linked resources
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.op.IEGitOperation;
import org.eclipse.egit.ui.internal.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CommitSelectDialog;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare with previous revision action handler.
 */
public class CompareWithPreviousActionHandler extends RepositoryActionHandler {

	private class CompareWithPreviousOperation implements IEGitOperation {

		private ExecutionEvent event;

		private Repository repository;

		private IResource resource;

		private CompareWithPreviousOperation(ExecutionEvent event,
				Repository repository, IResource resource) {
			this.event = event;
			this.repository = repository;
			this.resource = resource;
		}

		public void execute(IProgressMonitor monitor) throws CoreException {
			final List<PreviousCommit> previousList;
			try {
				previousList = findPreviousCommits();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
				return;
			}
			final AtomicReference<PreviousCommit> previous = new AtomicReference<PreviousCommit>();
			if (previousList.size() == 0) {
				showNotFoundDialog();
				return;
			} else if (previousList.size() > 1){
				final List<RevCommit> commits = new ArrayList<RevCommit>();
				for (PreviousCommit pc: previousList)
					commits.add(pc.commit);
				HandlerUtil.getActiveShell(event).getDisplay()
						.syncExec(new Runnable() {
							public void run() {
								CommitSelectDialog dlg = new CommitSelectDialog(
										HandlerUtil.getActiveShell(event),
										commits);
								if (dlg.open() == Window.OK)
									for (PreviousCommit pc: previousList)
										if (pc.commit.equals(dlg.getSelectedCommit())){
											   previous.set(pc);
											   break;
										   }
							}
						});
			}
			else
				previous.set(previousList.get(0));

			if (previous.get() == null)
				return;
			if (resource instanceof IFile) {
				final ITypedElement base = SaveableCompareEditorInput
						.createFileElement((IFile) resource);
				PreviousCommit pc = previous.get();
				ITypedElement next = CompareUtils.getFileRevisionTypedElement(
						pc.path, pc.commit, repository);
				CompareEditorInput input = new GitCompareFileRevisionEditorInput(
						base, next, null);
				CompareUI.openCompareEditor(input);
			} else
				openCompareTreeView(previous.get().commit);
		}

		private void openCompareTreeView(final RevCommit previous) {
			final Shell shell = HandlerUtil.getActiveShell(event);
			shell.getDisplay().asyncExec(new Runnable() {

				public void run() {
					try {
						CompareTreeView view = (CompareTreeView) PlatformUI
								.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().showView(CompareTreeView.ID);
						view.setInput(new IResource[] { resource },
								previous.name());
					} catch (PartInitException e) {
						Activator.handleError(e.getMessage(), e, true);
					}
				}
			});
		}

		private void showNotFoundDialog() {
			final Shell shell = HandlerUtil.getActiveShell(event);
			final String message = MessageFormat
					.format(UIText.CompareWithPreviousActionHandler_MessageRevisionNotFound,
							resource.getName());
			shell.getDisplay().asyncExec(new Runnable() {

				public void run() {
					MessageDialog
							.openWarning(
									shell,
									UIText.CompareWithPreviousActionHandler_TitleRevisionNotFound,
									message);
				}
			});
		}

		public ISchedulingRule getSchedulingRule() {
			return resource;
		}
	}

	/**
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Repository repository = getRepository(true, event);
		if (repository == null)
			return null;

		IResource[] resources = getSelectedResources(event);
		if (resources.length == 1)
			JobUtil.scheduleUserJob(
					new CompareWithPreviousOperation(event, repository,
							resources[0]),
					UIText.CompareWithPreviousActionHandler_TaskGeneratingInput,
					null);
		return null;
	}

	@Override
	public boolean isEnabled() {
		IResource[] selectedResources = getSelectedResources();
		return super.isEnabled() && selectedResources.length == 1 &&
				selectionMapsToSingleRepository();
	}
}
