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
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;
import java.text.MessageFormat;

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
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Compare with previous revision action handler.
 */
public class CompareWithPreviousActionHandler extends RepositoryActionHandler {

	private static class CompareWithPreviousOperation implements IEGitOperation {

		private ExecutionEvent event;

		private Repository repository;

		private IResource resource;

		private CompareWithPreviousOperation(ExecutionEvent event,
				Repository repository, IResource resource) {
			this.event = event;
			this.repository = repository;
			this.resource = resource;
		}

		private String getRepositoryPath() {
			return RepositoryMapping.getMapping(resource.getProject())
					.getRepoRelativePath(resource);
		}

		public void execute(IProgressMonitor monitor) throws CoreException {
			RevCommit previous = findPreviousCommit();
			if (previous != null)
				if (resource instanceof IFile) {
					final ITypedElement base = SaveableCompareEditorInput
							.createFileElement((IFile) resource);
					ITypedElement next = CompareUtils
							.getFileRevisionTypedElement(getRepositoryPath(),
									previous, repository);
					CompareEditorInput input = new GitCompareFileRevisionEditorInput(
							base, next, null);
					CompareUI.openCompareEditor(input);
				} else
					openCompareTreeView(previous);
			else
				showNotFoundDialog();
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

		private RevCommit findPreviousCommit() {
			RevWalk rw = new RevWalk(repository);
			try {
				String path = getRepositoryPath();
				if (path.length() > 0)
					rw.setTreeFilter(FollowFilter.create(path, rw.getObjectReader().getPathEncoding()));
				RevCommit headCommit = rw.parseCommit(repository.getRef(
						Constants.HEAD).getObjectId());
				rw.markStart(headCommit);
				headCommit = rw.next();
				if (headCommit != null)
					return rw.next();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			} finally {
				rw.dispose();
			}
			return null;
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
}
