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
package org.eclipse.egit.ui.internal.blame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jgit.api.LineHistoryCommand;
import org.eclipse.jgit.blame.Line;
import org.eclipse.jgit.blame.Revision;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.RevisionAnnotationController;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * Annotate operation
 */
public class AnnotateOperation implements IEGitOperation {

	private Repository repository;

	private IFile file;

	private Shell shell;

	private IWorkbenchPage page;

	/**
	 * Create annotate operation
	 *
	 * @param repository
	 * @param file
	 * @param shell
	 * @param page
	 */
	public AnnotateOperation(Repository repository, IFile file, Shell shell,
			IWorkbenchPage page) {
		this.repository = repository;
		this.file = file;
		this.shell = shell;
		this.page = page;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		final RevisionInformation info = new RevisionInformation();
		info.setHoverControlCreator(new AnnotationInformationControlCreator(
				false));
		info.setInformationPresenterControlCreator(new AnnotationInformationControlCreator(
				true));
		LineHistoryCommand command = new LineHistoryCommand(repository);
		RepositoryMapping map = RepositoryMapping.getMapping(file.getProject());
		command.setFilePath(map.getRepoRelativePath(file));
		final List<Revision> allRevisions = command.call();
		if (allRevisions.isEmpty())
			return;
		Map<Integer, AnnotationRevision> revisions = new HashMap<Integer, AnnotationRevision>();
		Revision latestRevision = allRevisions.get(allRevisions.size() - 1);
		monitor.beginTask("", latestRevision.getLineCount()); //$NON-NLS-1$
		AnnotationRevision previous = null;
		for (Line line : latestRevision.getLines()) {
			Integer start = Integer.valueOf(line.getStart());
			AnnotationRevision revision = revisions.get(start);
			if (revision == null) {
				revision = new AnnotationRevision();
				revision.setRepository(repository);
				revision.setRevision(allRevisions.get(line.getStart()));
				revisions.put(start, revision);
				info.addRevision(revision);
			}
			if (previous != null)
				if (previous == revision)
					previous.addLine();
				else {
					previous.register();
					previous = revision.reset(line.getNumber());
				}
			else
				previous = revision.reset(line.getNumber());
			monitor.worked(1);
		}
		if (previous != null)
			previous.register();

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