/*******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - turn it into a real text editor
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.FileDiffRegion;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * A {@link TextEditor} wrapped as an {@link IFormPage} and specialized to
 * showing a unified diff of a whole commit. The editor has an associated
 * {@link DiffEditorOutlinePage}.
 */
public class DiffEditorPage extends TextEditor
		implements IFormPage, IShowInSource, IShowInTargetList {

	private static final String[] SHOW_IN_TARGETS = {
			IHistoryView.VIEW_ID, RepositoriesView.VIEW_ID };

	private FormEditor formEditor;

	private String title;

	private String pageId;

	private int pageIndex = -1;

	private Control textControl;

	private DiffEditorOutlinePage outlinePage;

	private Annotation[] currentFoldingAnnotations;

	private FileDiffRegion currentFileDiffRange;

	/**
	 * Creates a new {@link DiffEditorPage} with the given id and title, which
	 * is shown on the page's tab.
	 *
	 * @param editor
	 *            containing this page
	 * @param id
	 *            of the page
	 * @param title
	 *            of the page
	 */
	public DiffEditorPage(FormEditor editor, String id, String title) {
		pageId = id;
		this.title = title;
		setPartName(title);
		initialize(editor);
	}

	/**
	 * Creates a new {@link DiffEditorPage} with default id and title.
	 *
	 * @param editor
	 *            containing the page
	 */
	public DiffEditorPage(FormEditor editor) {
		this(editor, "diffPage", UIText.DiffEditorPage_Title); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Switch to generified signature once EGit's base dependency is
		// Eclipse 4.6
		if (IContentOutlinePage.class.equals(adapter)) {
			if (outlinePage == null) {
				outlinePage = createOutlinePage();
				outlinePage.setInput(
						getDocumentProvider().getDocument(getEditorInput()));
				if (currentFileDiffRange != null) {
					outlinePage.setSelection(
							new StructuredSelection(currentFileDiffRange));
				}
			}
			return outlinePage;
		}
		return super.getAdapter(adapter);
	}

	private DiffEditorOutlinePage createOutlinePage() {
		DiffEditorOutlinePage page = new DiffEditorOutlinePage();
		page.addSelectionChangedListener(
				event -> doSetSelection(event.getSelection()));
		page.addOpenListener(event -> {
			FormEditor editor = getEditor();
			editor.getSite().getPage().activate(editor);
			editor.setActivePage(getId());
			doSetSelection(event.getSelection());
		});
		return page;
	}

	@Override
	public void dispose() {
		// Nested editors are responsible for disposing their outline pages
		// themselves.
		if (outlinePage != null) {
			outlinePage.dispose();
			outlinePage = null;
		}
		super.dispose();
	}

	// TextEditor specifics:

	@Override
	protected ISourceViewer createSourceViewer(Composite parent,
			IVerticalRuler ruler, int styles) {
		DiffViewer viewer = new DiffViewer(parent, ruler, getOverviewRuler(),
				isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		ProjectionSupport projector = new ProjectionSupport(viewer,
				getAnnotationAccess(), getSharedColors());
		projector.install();
		viewer.getTextWidget().addCaretListener((event) -> {
			if (outlinePage != null) {
				FileDiffRegion region = getFileDiffRange(event.caretOffset);
				if (region != null && !region.equals(currentFileDiffRange)) {
					currentFileDiffRange = region;
					outlinePage.setSelection(new StructuredSelection(region));
				} else {
					currentFileDiffRange = region;
				}
			}
		});
		return viewer;
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setDocumentProvider(new DiffDocumentProvider());
		setSourceViewerConfiguration(
				new DiffViewer.Configuration(getPreferenceStore()) {
					@Override
					public IReconciler getReconciler(
							ISourceViewer sourceViewer) {
						// Switch off spell-checking
						return null;
					}
				});
	}

	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (input instanceof DiffEditorInput) {
			setFolding();
			FileDiffRegion region = getFileDiffRange(0);
			currentFileDiffRange = region;
		} else if (input instanceof CommitEditorInput) {
			formatDiff();
			currentFileDiffRange = null;
		}
		if (outlinePage != null) {
			outlinePage.setInput(getDocumentProvider().getDocument(input));
			if (currentFileDiffRange != null) {
				outlinePage.setSelection(
						new StructuredSelection(currentFileDiffRange));
			}
		}
	}

	@Override
	protected void doSetSelection(ISelection selection) {
		if (!selection.isEmpty() && selection instanceof StructuredSelection) {
			Object selected = ((StructuredSelection) selection)
					.getFirstElement();
			if (selected instanceof FileDiffRegion) {
				FileDiffRegion newRange = (FileDiffRegion) selected;
				if (!newRange.equals(currentFileDiffRange)) {
					currentFileDiffRange = newRange;
					selectAndReveal(newRange.getOffset(), 0);
				}
				return;
			}
		}
		super.doSetSelection(selection);
	}

	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, ITextEditorActionConstants.GROUP_COPY,
				ITextEditorActionConstants.SELECT_ALL);
		// TextEditor always adds these, even if the document is not editable.
		menu.remove(ITextEditorActionConstants.SHIFT_RIGHT);
		menu.remove(ITextEditorActionConstants.SHIFT_LEFT);
	}

	// FormPage specifics:

	@Override
	public void initialize(FormEditor editor) {
		formEditor = editor;
	}

	@Override
	public FormEditor getEditor() {
		return formEditor;
	}

	@Override
	public IManagedForm getManagedForm() {
		return null;
	}

	@Override
	public void setActive(boolean active) {
		if (active) {
			setFocus();
		}
	}

	@Override
	public boolean isActive() {
		return this.equals(formEditor.getActivePageInstance());
	}

	@Override
	public boolean canLeaveThePage() {
		return true;
	}

	@Override
	public Control getPartControl() {
		return textControl;
	}

	@Override
	public String getId() {
		return pageId;
	}

	@Override
	public int getIndex() {
		return pageIndex;
	}

	@Override
	public void setIndex(int index) {
		pageIndex = index;
	}

	@Override
	public boolean isEditor() {
		return true;
	}

	@Override
	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
			return true;
		} else if (object instanceof ISelection) {
			doSetSelection((ISelection) object);
			return true;
		}
		return false;
	}

	// WorkbenchPart specifics:

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		Control[] children = parent.getChildren();
		textControl = children[children.length - 1];
	}

	// "Show In..." specifics:

	@Override
	public ShowInContext getShowInContext() {
		RepositoryCommit commit = AdapterUtils.adapt(getEditorInput(),
				RepositoryCommit.class);
		if (commit != null) {
			return new ShowInContext(getEditorInput(),
					new StructuredSelection(commit));
		}
		return null;
	}

	@Override
	public String[] getShowInTargetIds() {
		return SHOW_IN_TARGETS;
	}

	// Diff specifics:

	private void setFolding() {
		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
		IDocument document = viewer.getDocument();
		if (document instanceof DiffDocument) {
			FileDiffRegion[] regions = ((DiffDocument) document)
					.getFileRegions();
			if (regions == null || regions.length <= 1) {
				viewer.disableProjection();
				return;
			}
			viewer.enableProjection();
			Map<Annotation, Position> newAnnotations = new HashMap<>();
			for (FileDiffRegion region : regions) {
				newAnnotations.put(new ProjectionAnnotation(),
						new Position(region.getOffset(), region.getLength()));
			}
			viewer.getProjectionAnnotationModel().modifyAnnotations(
					currentFoldingAnnotations, newAnnotations, null);
			currentFoldingAnnotations = newAnnotations.keySet()
					.toArray(new Annotation[newAnnotations.size()]);
		} else {
			viewer.disableProjection();
		}
	}

	private FileDiffRegion getFileDiffRange(int widgetOffset) {
		DiffViewer viewer = (DiffViewer) getSourceViewer();
		int offset = viewer.widgetOffset2ModelOffset(widgetOffset);
		IDocument document = getDocumentProvider()
				.getDocument(getEditorInput());
		if (document instanceof DiffDocument) {
			return ((DiffDocument) document).findFileRegion(offset);
		}
		return null;
	}

	/**
	 * Gets the full unified diff of a {@link RepositoryCommit}.
	 *
	 * @param commit
	 *            to get the diff
	 * @return the diff as a sorted (by file path) array of {@link FileDiff}s
	 */
	protected FileDiff[] getDiffs(RepositoryCommit commit) {
		List<FileDiff> diffResult = new ArrayList<>();

		diffResult.addAll(asList(commit.getDiffs()));

		if (commit.getRevCommit().getParentCount() > 2) {
			RevCommit untrackedCommit = commit.getRevCommit()
					.getParent(StashEditorPage.PARENT_COMMIT_UNTRACKED);
			diffResult
					.addAll(asList(new RepositoryCommit(commit.getRepository(),
							untrackedCommit).getDiffs()));
		}
		Collections.sort(diffResult, FileDiff.PATH_COMPARATOR);
		return diffResult.toArray(new FileDiff[diffResult.size()]);
	}

	/**
	 * Asynchronously gets the diff of the commit set on our
	 * {@link CommitEditorInput}, formats it into a {@link DiffDocument}, and
	 * then re-sets this editors's input to a {@link DiffEditorInput} which will
	 * cause this document to be shown.
	 */
	private void formatDiff() {
		final DiffDocument document = new DiffDocument();
		final DiffRegionFormatter formatter = new DiffRegionFormatter(
				document);

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
						if (UIUtils.isUsable(getPartControl())) {
							document.connect(formatter);
							setInput(new DiffEditorInput(commit, document));
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
	 * An editor input that gives access to the document created by the diff
	 * formatter.
	 */
	private static class DiffEditorInput extends CommitEditorInput {

		private IDocument document;

		public DiffEditorInput(RepositoryCommit commit, DiffDocument diff) {
			super(commit);
			document = diff;
		}

		public IDocument getDocument() {
			return document;
		}

		@Override
		public String getName() {
			return UIText.DiffEditorPage_Title;
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj) && (obj instanceof DiffEditorInput)
					&& document.equals(((DiffEditorInput) obj).document);
		}

		@Override
		public int hashCode() {
			return super.hashCode() ^ document.hashCode();
		}
	}

	/**
	 * A document provider that knows about {@link DiffEditorInput}.
	 */
	private static class DiffDocumentProvider extends AbstractDocumentProvider {

		@Override
		protected IDocument createDocument(Object element)
				throws CoreException {
			if (element instanceof DiffEditorInput) {
				return ((DiffEditorInput) element).getDocument();
			}
			return new Document();
		}

		@Override
		protected IAnnotationModel createAnnotationModel(Object element)
				throws CoreException {
			return new AnnotationModel();
		}

		@Override
		protected void doSaveDocument(IProgressMonitor monitor, Object element,
				IDocument document, boolean overwrite) throws CoreException {
			// Cannot save
		}

		@Override
		protected IRunnableContext getOperationRunner(
				IProgressMonitor monitor) {
			return null;
		}

	}

}
