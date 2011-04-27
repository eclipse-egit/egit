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

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.egit.ui.internal.job.JobUtil;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jgit.api.LineHistoryCommand;
import org.eclipse.jgit.blame.Line;
import org.eclipse.jgit.blame.RevisionContainer;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.RevisionAnnotationController;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * Show annotation action handler
 */
public class ShowAnnotationHandler extends RepositoryActionHandler {

	private class AnnotationRevision extends Revision {

		int start;

		int lines = 1;

		org.eclipse.jgit.blame.Revision revision;

		public Object getHoverInfo() {
			StringBuilder hover = new StringBuilder();
			RevCommit commit = revision.getCommit();
			hover.append(MessageFormat.format(
					UIText.ShowAnnotationHandler_HoverCommit, commit.name()));
			PersonIdent author = commit.getAuthorIdent();
			if (author != null) {
				hover.append(MessageFormat.format(
						UIText.ShowAnnotationHandler_HoverAuthor,
						author.getName(), author.getEmailAddress(),
						author.getWhen()));
			}
			PersonIdent committer = commit.getCommitterIdent();
			if (committer != null && !committer.equals(author)) {
				hover.append(MessageFormat.format(
						UIText.ShowAnnotationHandler_HoverCommitter,
						committer.getName(), committer.getEmailAddress(),
						committer.getWhen()));
			}
			hover.append("<pre>"); //$NON-NLS-1$
			hover.append(commit.getFullMessage());
			hover.append("</pre>"); //$NON-NLS-1$
			return hover.toString();
		}

		public RGB getColor() {
			return null;
		}

		public String getId() {
			return revision.getCommit().name();
		}

		public Date getDate() {
			return revision.getCommit().getAuthorIdent().getWhen();
		}

		private AnnotationRevision register() {
			addRange(new LineRange(start, lines));
			return this;
		}

		private AnnotationRevision reset(int number) {
			start = number;
			lines = 1;
			return this;
		}

	}

	private class AnnotateOperation implements IEGitOperation {

		private IFile file;

		private Shell shell;

		private IWorkbenchPage page;

		private AnnotateOperation(IFile file, Shell shell, IWorkbenchPage page) {
			this.file = file;
			this.shell = shell;
			this.page = page;
		}

		public void execute(IProgressMonitor monitor) throws CoreException {
			final RevisionInformation info = new RevisionInformation();
			LineHistoryCommand command = new LineHistoryCommand(getRepository());
			RepositoryMapping map = RepositoryMapping.getMapping(file
					.getProject());
			command.setFilePath(map.getRepoRelativePath(file));
			final RevisionContainer container = command.call();
			Map<Integer, AnnotationRevision> revisions = new HashMap<Integer, AnnotationRevision>();
			monitor.beginTask("", container.getLast().getLineCount()); //$NON-NLS-1$
			AnnotationRevision previous = null;
			for (Line line : container.getLast()) {
				Integer start = Integer.valueOf(line.getStart());
				AnnotationRevision revision = revisions.get(start);
				if (revision == null) {
					revision = new AnnotationRevision();
					revision.revision = container.getRevision(line.getStart());
					revisions.put(start, revision);
					info.addRevision(revision);
				}
				if (previous != null)
					if (previous == revision)
						previous.lines++;
					else {
						previous.register();
						previous = revision.reset(line.getNumber());
					}
				else
					previous = revision.reset(line.getNumber());
				monitor.worked(1);
			}
			if (previous != null)
				previous.addRange(new LineRange(previous.start, previous.lines));
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						AbstractDecoratedTextEditor editor = RevisionAnnotationController
								.openEditor(page, file);
						if (editor != null)
							editor.showRevisionInformation(info,
									"org.eclipse.egit.ui.internal.decorators.GitQuickDiffProvider"); //$NON-NLS-1$
					} catch (PartInitException e) {
						Activator.handleError("Error displaying annotation", e, //$NON-NLS-1$
								false);
					}
				}
			});
		}

		public ISchedulingRule getSchedulingRule() {
			return null;
		}
	}

	/** @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent) */
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		IResource[] selected = getSelectedResources();
		if (selected.length == 0 || !(selected[0] instanceof IFile))
			return null;
		JobUtil.scheduleUserJob(
				new AnnotateOperation((IFile) selected[0], HandlerUtil
						.getActiveShell(event), HandlerUtil
						.getActiveSite(event).getPage()),
				UIText.ShowAnnotationHandler_JobName, null);
		return null;
	}
}
