/******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
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
import java.io.IOException;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.revisions.IRevisionRulerColumnExtension;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
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

	static class BlameHistoryPageInput extends HistoryPageInput
			implements IAdaptable {

		private final RevCommit commit;

		BlameHistoryPageInput(Repository repository, RevCommit commit,
				File file) {
			super(repository, new File[] { file });
			this.commit = commit;
		}

		BlameHistoryPageInput(Repository repository, RevCommit commit,
				IResource file) {
			super(repository, new IResource[] { file });
			this.commit = commit;
		}

		BlameHistoryPageInput(Repository repository, RevCommit commit) {
			super(repository);
			this.commit = commit;
		}

		@Override
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

		private boolean firstSelectionChange = true;

		private RevisionSelectionHandler(Repository repository, String path,
				IStorage storage) {
			if (storage instanceof IFile)
				resourceFile = (IFile) storage;
			else if (!repository.isBare())
				nonResourceFile = new File(repository.getWorkTree(), path);
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			// Don't show the commit for the first selection change, as that was
			// not initiated by the user directly. Instead, show the commit the
			// first time the user clicks on a revision or line.
			if (firstSelectionChange) {
				firstSelectionChange = false;
				return;
			}
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

	private RevCommit startCommit;

	private Shell shell;

	private IWorkbenchPage page;

	private int lineNumberToReveal;

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
			RevCommit startCommit, Shell shell, IWorkbenchPage page) {
		this(repository, storage, path, startCommit, shell, page, -1);
	}

	/**
	 * Create annotate operation
	 *
	 * @param repository
	 * @param storage
	 * @param path
	 * @param startCommit
	 * @param shell
	 * @param page
	 * @param lineNumberToReveal
	 *            0-based line number to reveal, -1 for no reveal
	 */
	public BlameOperation(Repository repository, IStorage storage, String path,
			RevCommit startCommit, Shell shell, IWorkbenchPage page,
			int lineNumberToReveal) {
		this.repository = repository;
		this.storage = storage;
		this.path = path;
		this.startCommit = startCommit;
		this.shell = shell;
		this.page = page;
		this.lineNumberToReveal = lineNumberToReveal;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		final RevisionInformation info = new RevisionInformation();

		final BlameCommand command = new BlameCommand(repository)
				.setFollowFileRenames(true).setFilePath(path);
		if (startCommit != null)
			command.setStartCommit(startCommit);
		else {
			try {
				command.setStartCommit(repository.resolve(Constants.HEAD));
			} catch (IOException e) {
				Activator
						.error("Error resolving HEAD for showing annotations in repository: " + repository, e); //$NON-NLS-1$
				return;
			}
		}
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

		Map<RevCommit, BlameRevision> revisions = new HashMap<>();
		int lineCount = result.getResultContents().size();
		BlameRevision previous = null;
		for (int i = 0; i < lineCount; i++) {
			RevCommit commit = result.getSourceCommit(i);
			String sourcePath = result.getSourcePath(i);
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
				revision.setSourcePath(sourcePath);
				revisions.put(commit, revision);
				info.addRevision(revision);
			}
			revision.addSourceLine(i, result.getSourceLine(i));
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

		if (shell.isDisposed()) {
			return;
		}

		shell.getDisplay().asyncExec(new Runnable() {
			@Override
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
			HistoryPageInput input = createHistoryPageInputWhenEditorOpened();
			part.showHistoryFor(input);
		} catch (PartInitException e) {
			Activator.handleError("Error displaying blame annotations", e, //$NON-NLS-1$
					false);
		}

		// IRevisionRulerColumn would also be possible but using
		// IVerticalRulerInfo seems to work in more situations.
		IVerticalRulerInfo rulerInfo = AdapterUtils.adapt(editor,
				IVerticalRulerInfo.class);

		BlameInformationControlCreator creator = new BlameInformationControlCreator(
				rulerInfo);
		info.setHoverControlCreator(creator);
		info.setInformationPresenterControlCreator(creator);

		editor.showRevisionInformation(info,
				"org.eclipse.egit.ui.internal.decorators.GitQuickDiffProvider"); //$NON-NLS-1$

		if (lineNumberToReveal >= 0) {
			IDocument document = editor.getDocumentProvider().getDocument(
					editor.getEditorInput());
			int offset;
			try {
				offset = document.getLineOffset(lineNumberToReveal);
				editor.selectAndReveal(offset, 0);
			} catch (BadLocationException e) {
				Activator.logError(
						"Error revealing line " + lineNumberToReveal, e); //$NON-NLS-1$
			}
		}

		IRevisionRulerColumn revisionRuler = AdapterUtils.adapt(editor,
				IRevisionRulerColumn.class);
		if (revisionRuler instanceof IRevisionRulerColumnExtension)
			((IRevisionRulerColumnExtension) revisionRuler)
					.getRevisionSelectionProvider()
					.addSelectionChangedListener(
							new RevisionSelectionHandler(repository, path,
									storage));
	}

	private HistoryPageInput createHistoryPageInputWhenEditorOpened() {
		if (storage instanceof IFile) {
			IResource resource = (IResource) storage;
			if (startCommit != null) {
				return new BlameHistoryPageInput(repository, startCommit,
						resource);
			} else {
				return new HistoryPageInput(repository,
						new IResource[] { resource });
			}
		} else if (!repository.isBare()) {
			File file = new File(repository.getWorkTree(), path);
			if (startCommit != null) {
				return new BlameHistoryPageInput(repository, startCommit,
						file);
			} else {
				return new HistoryPageInput(repository,
						new File[] { file });
			}
		} else {
			return new HistoryPageInput(repository);
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}