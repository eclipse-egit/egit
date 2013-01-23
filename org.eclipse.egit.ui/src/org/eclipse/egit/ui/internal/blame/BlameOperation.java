/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.revisions.IRevisionRulerColumnExtension;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.history.RevisionAnnotationController;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * Blame operation
 */
public class BlameOperation implements IEGitOperation {

	private static class BlameHistoryPageInput extends HistoryPageInput
			implements IAdaptable {

		private final RevCommit commit;

		private BlameHistoryPageInput(Repository repository, RevCommit commit,
				File file) {
			super(repository, new File[] { file });
			this.commit = commit;
		}

		private BlameHistoryPageInput(Repository repository, RevCommit commit,
				IResource file) {
			super(repository, new IResource[] { file });
			this.commit = commit;
		}

		private BlameHistoryPageInput(Repository repository, RevCommit commit) {
			super(repository);
			this.commit = commit;
		}

		public Object getAdapter(Class adapter) {
			if (RevCommit.class == adapter)
				return commit;
			return Platform.getAdapterManager().getAdapter(this, adapter);
		}
	}

	private static class RevisionSelectionHandler implements
			ISelectionChangedListener {

		private IFile resourceFile;

		private File nonResourceFile;

		private RevisionSelectionHandler(Repository repository, String path,
				IStorage storage) {
			if (storage instanceof IFile)
				resourceFile = (IFile) storage;
			else if (!repository.isBare())
				nonResourceFile = new File(repository.getWorkTree(), path);
		}

		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = event.getSelection();
			if (selection.isEmpty()
					|| !(selection instanceof IStructuredSelection))
				return;
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (!(first instanceof BlameRevision))
				return;

			IHistoryView part = (IHistoryView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(IHistoryView.VIEW_ID);
			if (part == null)
				return;

			BlameRevision revision = (BlameRevision) first;
			BlameHistoryPageInput input;
			if (resourceFile != null)
				input = new BlameHistoryPageInput(revision.getRepository(),
						revision.getCommit(), resourceFile);
			else if (nonResourceFile != null)
				input = new BlameHistoryPageInput(revision.getRepository(),
						revision.getCommit(), nonResourceFile);
			else
				input = new BlameHistoryPageInput(revision.getRepository(),
						revision.getCommit());
			part.showHistoryFor(input);
		}

	}

	private Repository repository;

	private IStorage storage;

	private String path;

	private AnyObjectId startCommit;

	private Shell shell;

	private IWorkbenchPage page;

	/**
	 * Create annotate operation
	 *
	 * @param repository
	 * @param storage
	 * @param path
	 * @param startCommit
	 * @param shell
	 * @param page
	 */
	public BlameOperation(Repository repository, IStorage storage, String path,
			AnyObjectId startCommit, Shell shell, IWorkbenchPage page) {
		this.repository = repository;
		this.storage = storage;
		this.path = path;
		this.startCommit = startCommit;
		this.shell = shell;
		this.page = page;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		final RevisionInformation info = new RevisionInformation();
		info.setHoverControlCreator(new BlameInformationControlCreator(false));
		info.setInformationPresenterControlCreator(new BlameInformationControlCreator(
				true));

		final BlameCommand command = new BlameCommand(repository)
				.setFollowFileRenames(true).setFilePath(path);
		if (startCommit != null)
			command.setStartCommit(startCommit);
		if (Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.BLAME_IGNORE_WHITESPACE))
			command.setTextComparator(RawTextComparator.WS_IGNORE_ALL);

		BlameResult result;
		try {
			result = command.call();
		} catch (Exception e1) {
			Activator.error(e1.getMessage(), e1);
			return;
		}
		if (result == null)
			return;

		Map<RevCommit, BlameRevision> revisions = new HashMap<RevCommit, BlameRevision>();
		int lineCount = result.getResultContents().size();
		BlameRevision previous = null;
		for (int i = 0; i < lineCount; i++) {
			RevCommit commit = result.getSourceCommit(i);
			if (commit == null) {
				// Unregister the current revision
				if (previous != null) {
					previous.register();
					previous = null;
				}
				continue;
			}
			BlameRevision revision = revisions.get(commit);
			if (revision == null) {
				revision = new BlameRevision();
				revision.setRepository(repository);
				revision.setCommit(commit);
				revisions.put(commit, revision);
				info.addRevision(revision);
			}
			if (previous != null)
				if (previous == revision)
					previous.addLine();
				else {
					previous.register();
					previous = revision.reset(i);
				}
			else
				previous = revision.reset(i);
		}
		if (previous != null)
			previous.register();

		shell.getDisplay().asyncExec(new Runnable() {
			public void run() {
				openEditor(info);
			}
		});
	}

	private void openEditor(final RevisionInformation info) {
		AbstractDecoratedTextEditor editor;
		try {
			if (storage instanceof IFile)
				editor = RevisionAnnotationController.openEditor(page,
						(IFile) storage);
			else
				editor = RevisionAnnotationController.openEditor(page, storage,
						storage);
		} catch (PartInitException e) {
			Activator.handleError("Error displaying blame annotations", e, //$NON-NLS-1$
					false);
			return;
		}
		if (editor == null)
			return;

		// Show history view for path
		try {
			IHistoryView part = (IHistoryView) page.showView(
					IHistoryView.VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
			HistoryPageInput input;
			if (storage instanceof IFile)
				input = new HistoryPageInput(repository,
						new IResource[] { (IResource) storage });
			else if (!repository.isBare())
				input = new HistoryPageInput(repository, new File[] { new File(
						repository.getWorkTree(), path) });
			else
				input = new HistoryPageInput(repository);
			part.showHistoryFor(input);
		} catch (PartInitException e) {
			Activator.handleError("Error displaying blame annotations", e, //$NON-NLS-1$
					false);
		}

		editor.showRevisionInformation(info,
				"org.eclipse.egit.ui.internal.decorators.GitQuickDiffProvider"); //$NON-NLS-1$

		IRevisionRulerColumn revisionRuler = AdapterUtils.adapt(editor,
				IRevisionRulerColumn.class);
		if (revisionRuler instanceof IRevisionRulerColumnExtension)
			((IRevisionRulerColumnExtension) revisionRuler)
					.getRevisionSelectionProvider()
					.addSelectionChangedListener(
							new RevisionSelectionHandler(repository, path,
									storage));
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}