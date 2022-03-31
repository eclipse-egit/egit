/******************************************************************************
 *  Copyright (c) 2011, 2021 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - don't use RevisionAnnotationController
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.EditorVisibilityTracker;
import org.eclipse.egit.ui.internal.decorators.GitQuickDiffProvider;
import org.eclipse.egit.ui.internal.history.GitHistoryPage;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.egit.ui.internal.revision.FileRevisionEditorInput;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.revisions.IRevisionRulerColumnExtension;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.events.RefsChangedListener;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.history.RevisionAnnotationController;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Blame operation
 */
public class BlameOperation implements IEGitOperation {

	private static final String QUICKDIFF_PROVIDER_ID = GitQuickDiffProvider.class
			.getName();

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
		public <T> T getAdapter(Class<T> adapter) {
			if (RevCommit.class == adapter) {
				return adapter.cast(commit);
			}
			return Platform.getAdapterManager().getAdapter(this, adapter);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			BlameHistoryPageInput other = (BlameHistoryPageInput) obj;
			return fieldsEqual(other) && Objects.equals(commit, other.commit);
		}

		@Override
		public int hashCode() {
			return super.hashCode() * 31 + Objects.hashCode(commit);
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

			if (!GitHistoryPage.isLinkingEnabled()) {
				return;
			}

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
			IHistoryPage currentPage = part.getHistoryPage();
			if (currentPage instanceof GitHistoryPage
					&& input.baseEquals(currentPage.getInput())) {
				// Already showing a git history for this -- just refresh and
				// select the commit to avoid adding a new history navigation
				// entry.
				((GitHistoryPage) currentPage).refresh(revision.getCommit());
			} else {
				part.showHistoryFor(input);
			}
		}

	}

	private Repository repository;

	private CommitFileRevision fileRevision;

	private IStorage storage;

	private String path;

	private RevCommit startCommit;

	private Shell shell;

	private IWorkbenchPage page;

	private int lineNumberToReveal = -1;

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
	public BlameOperation(Repository repository, IFile storage, String path,
			RevCommit startCommit, Shell shell, IWorkbenchPage page) {
		this.repository = repository;
		this.storage = storage;
		this.path = path;
		this.startCommit = startCommit;
		this.shell = shell;
		this.page = page;
		this.lineNumberToReveal = -1;
	}

	/**
	 * @param revision
	 * @param shell
	 * @param page
	 */
	public BlameOperation(CommitFileRevision revision, Shell shell,
			IWorkbenchPage page) {
		this(revision, shell, page, -1);
	}

	/**
	 * @param revision
	 * @param shell
	 * @param page
	 * @param lineNumberToReveal
	 */
	public BlameOperation(CommitFileRevision revision, Shell shell,
			IWorkbenchPage page, int lineNumberToReveal) {
		this.fileRevision = revision;
		this.repository = revision.getRepository();
		this.path = revision.getGitPath();
		this.startCommit = revision.getRevCommit();
		this.shell = shell;
		this.page = page;
		this.lineNumberToReveal = lineNumberToReveal;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		RevisionInformation info;
		ObjectId currentHead = null;
		if (startCommit != null) {
			info = computeRevisions(repository, startCommit, path,
					progress.newChild(2));
		} else {
			try {
				currentHead = repository.resolve(Constants.HEAD);
			} catch (IOException e) {
				Activator
						.error("Error resolving HEAD for showing annotations in repository: " + repository, e); //$NON-NLS-1$
				return;
			}
			info = computeRevisions(repository, currentHead, path,
					progress.newChild(2));
		}
		if (info == null) {
			return;
		}
		if (shell.isDisposed()) {
			return;
		}
		if (fileRevision != null) {
			storage = fileRevision.getStorage(progress.newChild(1));
		} else {
			progress.worked(1);
		}
		ObjectId headId = currentHead;
		shell.getDisplay().asyncExec(() -> openEditor(info, headId));
	}

	private static RevisionInformation computeRevisions(Repository repo,
			ObjectId start, String path, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		RevisionInformation info = new RevisionInformation();
		BlameCommand command = new BlameCommand(repo).setFollowFileRenames(true)
				.setFilePath(path).setStartCommit(start);
		if (Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.BLAME_IGNORE_WHITESPACE))
			command.setTextComparator(RawTextComparator.WS_IGNORE_ALL);

		BlameResult result;
		try {
			result = command.call();
		} catch (Exception e1) {
			Activator.error(e1.getMessage(), e1);
			return null;
		}
		progress.worked(1);
		if (result == null) {
			return null;
		}
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
				revision.setRepository(repo);
				revision.setCommit(commit);
				revision.setSourcePath(sourcePath);
				revisions.put(commit, revision);
				info.addRevision(revision);
			}
			revision.addSourceLine(i, result.getSourceLine(i));
			if (previous != null)
				if (previous == revision) {
					previous.addLine();
				} else {
					previous.register();
					previous = revision.reset(i);
				}
			else {
				previous = revision.reset(i);
			}
		}
		if (previous != null) {
			previous.register();
		}
		return info;
	}

	private void openEditor(RevisionInformation info, ObjectId currentHead) {
		IEditorPart editorPart;
		try {
			if (storage instanceof IFile) {
				editorPart = RevisionAnnotationController.openEditor(page,
						(IFile) storage);
			} else {
				FileRevisionEditorInput editorInput = new FileRevisionEditorInput(
						fileRevision, storage);
				editorPart = EgitUiEditorUtils.openEditor(page, editorInput);
				if (editorPart instanceof MultiPageEditorPart) {
					MultiPageEditorPart multiEditor = (MultiPageEditorPart) editorPart;
					for (IEditorPart part : multiEditor
							.findEditors(editorInput)) {
						if (part instanceof AbstractDecoratedTextEditor) {
							multiEditor.setActiveEditor(part);
							editorPart = part;
							break;
						}
					}
				}
			}
		} catch (CoreException e) {
			Activator.handleError("Error displaying blame annotations", e, //$NON-NLS-1$
					false);
			return;
		}
		if (!(editorPart instanceof AbstractDecoratedTextEditor)) {
			return;
		}
		AbstractDecoratedTextEditor editor = (AbstractDecoratedTextEditor) editorPart;
		// IRevisionRulerColumn would also be possible but using
		// IVerticalRulerInfo seems to work in more situations.
		IVerticalRulerInfo rulerInfo = Adapters.adapt(editor,
				IVerticalRulerInfo.class);

		HoverCreators provider = new HoverCreators(rulerInfo);
		IInformationControlCreator creator = provider.hoverCreator();
		IInformationControlCreator presenter = provider.stickyHoverCreator();
		info.setHoverControlCreator(creator);
		info.setInformationPresenterControlCreator(presenter);

		editor.showRevisionInformation(info, QUICKDIFF_PROVIDER_ID);

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

		IRevisionRulerColumn revisionRuler = Adapters.adapt(editor,
				IRevisionRulerColumn.class);
		if (revisionRuler != null) {
			if (revisionRuler instanceof IRevisionRulerColumnExtension) {
				String flagName = getClass().getName() + ".selectionHandler"; //$NON-NLS-1$
				Control control = revisionRuler.getControl();
				Object flag = control.getData(flagName);
				if (flag == null) {
					((IRevisionRulerColumnExtension) revisionRuler)
							.getRevisionSelectionProvider()
							.addSelectionChangedListener(
									new RevisionSelectionHandler(repository,
											path, storage));
					control.setData(flagName, Boolean.TRUE);
				}
			}
			if (currentHead != null && storage instanceof IFile
					&& editor.isChangeInformationShowing()) {
				refreshOnHeadChange(editor, revisionRuler, creator, presenter,
						currentHead);
			}
		}
	}

	private void refreshOnHeadChange(AbstractDecoratedTextEditor editor,
			IRevisionRulerColumn ruler,
			IInformationControlCreator hoverPopupCreator,
			IInformationControlCreator hoverPresenter,
			ObjectId currentHead) {
		String flagName = getClass().getName() + ".editorHooks"; //$NON-NLS-1$
		Control control = ruler.getControl();
		Object flag = control.getData(flagName);
		if (flag != null) {
			// Editor already hooked
			return;
		}
		EditorVisibilityTracker visibilityTracker = new EditorVisibilityTracker(
				editor);
		IPartService partService = editor.getEditorSite()
				.getService(IPartService.class);
		partService.addPartListener(visibilityTracker);
		RefsChangedListener refsTracker = new RefsChangedListener() {

			private ObjectId lastHead = currentHead;

			@Override
			public void onRefsChanged(RefsChangedEvent event) {
				try {
					ObjectId head = event.getRepository()
							.resolve(Constants.HEAD);
					if (head != null && !head.equals(lastHead)) {
						lastHead = head;
						Display display = PlatformUI.getWorkbench()
								.getDisplay();
						if (display != null && !display.isDisposed()) {
							display.asyncExec(() -> {
								if (editor.isChangeInformationShowing()) {
									visibilityTracker.runWhenVisible(
											() -> updateBlame(head, ruler,
													hoverPopupCreator,
													hoverPresenter, editor));
								}
							});
						}
					}
				} catch (IOException e) {
					Activator.logError(e.getLocalizedMessage(), e);
				}
			}
		};
		ListenerHandle handle = repository.getListenerList()
				.addRefsChangedListener(refsTracker);
		control.setData(flagName, Boolean.TRUE);
		// We don't get any event when revision information is hidden. So we
		// have to hack a bit and replace the action to be able to remove our
		// listeners when revisions are hidden.
		IAction existingAction = editor
				.getAction(ITextEditorActionConstants.REVISION_HIDE_INFO);
		Action newAction = new Action(existingAction.getText()) {

			@Override
			public void run() {
				control.setData(flagName, null);
				handle.remove();
				partService.removePartListener(visibilityTracker);
				editor.setAction(ITextEditorActionConstants.REVISION_HIDE_INFO,
						existingAction);
				existingAction.run();
			}
		};
		newAction.setToolTipText(existingAction.getToolTipText());
		newAction.setDescription(existingAction.getDescription());
		newAction.setImageDescriptor(existingAction.getImageDescriptor());
		editor.setAction(ITextEditorActionConstants.REVISION_HIDE_INFO,
				newAction);
		// Remove the listeners when the editor is disposed.
		ruler.getControl().addDisposeListener(event -> {
			handle.remove();
			partService.removePartListener(visibilityTracker);
		});
	}

	private void updateBlame(ObjectId head, IRevisionRulerColumn ruler,
			IInformationControlCreator hoverPopupCreator,
			IInformationControlCreator hoverPresenter,
			AbstractDecoratedTextEditor editor) {
		Job blamer = new Job(UIText.ShowBlameHandler_JobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					RevisionInformation info = computeRevisions(repository,
							head, path, monitor);
					Control control = ruler.getControl();
					Display display = control.getDisplay();
					display.asyncExec(() -> {
						if (!control.isDisposed()) {
							info.setHoverControlCreator(hoverPopupCreator);
							info.setInformationPresenterControlCreator(
									hoverPresenter);
							if (editor.isChangeInformationShowing()) {
								editor.showRevisionInformation(info,
										QUICKDIFF_PROVIDER_ID);
							}
						}
					});
					return Status.OK_STATUS;
				} catch (SWTException e) {
					// Already disposed?
					return Status.CANCEL_STATUS;
				} catch (RuntimeException e) {
					return Activator.createErrorStatus(e.getLocalizedMessage(),
							e);
				} finally {
					monitor.done();
				}
			}

			@Override
			public boolean belongsTo(Object family) {
				return JobFamilies.BLAME == family || super.belongsTo(family);
			}
		};
		blamer.setUser(false);
		blamer.setSystem(true);
		blamer.schedule();
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	/**
	 * Making a hover sticky via F2 resets its size. So we tell the sticky hover
	 * creator the size of the previously visible non-sticky hover, and use that
	 * to compute the sticky hover's size <em>constraints</em>. This ensures
	 * that the framework doesn't use its small fixed size constraints set in
	 * org.eclipse.ui.internal.texteditor.FocusedInformationPresenter (100 x 12
	 * characters) but the size of the non-sticky hover, and thus the hover size
	 * remains the same between non-sticky and sticky hovers. The framework does
	 * compute a slightly different location for the sticky hover, though, and
	 * moves it a few pixels to the right.
	 *
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=575197">bug
	 *      575197</a>
	 */
	private static class HoverCreators {

		private final IInformationControlCreator nonStickyCreator;

		private final IInformationControlCreator stickyCreator;

		private Point hoverSize;

		HoverCreators(IVerticalRulerInfo rulerInfo) {
			this.nonStickyCreator = parentShell -> new BlameInformationControl(
					parentShell, rulerInfo) {

				@Override
				public Point computeSizeHint() {
					Point size = super.computeSizeHint();
					hoverSize = Geometry.copy(size);
					return size;
				}
			};
			this.stickyCreator = parentShell -> new BlameInformationControl(
					parentShell, rulerInfo, true) {

				@Override
				public Point computeSizeConstraints(int widthInChars,
						int heightInChars) {
					Point size = super.computeSizeConstraints(widthInChars,
							heightInChars);
					if (hoverSize != null) {
						size = Geometry.max(size, hoverSize);
					}
					return size;
				}
			};
		}

		public IInformationControlCreator hoverCreator() {
			return nonStickyCreator;
		}

		public IInformationControlCreator stickyHoverCreator() {
			return stickyCreator;
		}
	}
}
