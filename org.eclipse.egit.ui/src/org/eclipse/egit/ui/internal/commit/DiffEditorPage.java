/*******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - turn it into a real text editor
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static java.util.Arrays.asList;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffAddBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffRemoveBackgroundColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.DiffRegion;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.FileDiffRegion;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.themes.IThemeManager;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * A {@link TextEditor} wrapped as an {@link IFormPage} and specialized to
 * showing a unified diff of a whole commit. The editor has an associated
 * {@link DiffEditorOutlinePage}.
 */
public class DiffEditorPage extends TextEditor
		implements IFormPage, IShowInSource, IShowInTargetList {

	private static final String ADD_ANNOTATION_TYPE = "org.eclipse.egit.ui.commitEditor.diffAdded"; //$NON-NLS-1$

	private static final String REMOVE_ANNOTATION_TYPE = "org.eclipse.egit.ui.commitEditor.diffRemoved"; //$NON-NLS-1$

	private FormEditor formEditor;

	private String title;

	private String pageId;

	private int pageIndex = -1;

	private Control textControl;

	private DiffEditorOutlinePage outlinePage;

	private Annotation[] currentFoldingAnnotations;

	private Annotation[] currentOverviewAnnotations;

	/** An {@link IPreferenceStore} for the annotation colors.*/
	private ThemePreferenceStore overviewStore;

	private FileDiffRegion currentFileDiffRange;

	private OldNewLogicalLineNumberRulerColumn lineNumberColumn;

	private boolean plainLineNumbers = false;

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
		if (overviewStore != null) {
			overviewStore.dispose();
			overviewStore = null;
		}
		super.dispose();
	}

	// TextEditor specifics:

	@Override
	protected ISourceViewer createSourceViewer(Composite parent,
			IVerticalRuler ruler, int styles) {
		DiffViewer viewer = new DiffViewer(parent, ruler, getOverviewRuler(),
				isOverviewRulerVisible(), styles) {
			@Override
			protected void setFont(Font font) {
				// Don't do anything; AbstractTextEditor handles this.
			}
		};
		getSourceViewerDecorationSupport(viewer);
		ProjectionSupport projector = new ProjectionSupport(viewer,
				getAnnotationAccess(), getSharedColors());
		projector.install();
		viewer.getTextWidget().addCaretListener(event -> {
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
	protected IVerticalRulerColumn createLineNumberRulerColumn() {
		lineNumberColumn = new OldNewLogicalLineNumberRulerColumn(
				plainLineNumbers);
		initializeLineNumberRulerColumn(lineNumberColumn);
		return lineNumberColumn;
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		overviewStore = new ThemePreferenceStore();
		setPreferenceStore(new ChainedPreferenceStore(new IPreferenceStore[] {
				overviewStore, EditorsUI.getPreferenceStore() }));
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
			setOverviewAnnotations();
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

	@Override
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		// AbstractDecoratedTextEditor's menu presumes a
		// LineNumberChangeRulerColumn, which we don't have.
		IContributionItem showLineNumbers = menu
				.find(ITextEditorActionConstants.LINENUMBERS_TOGGLE);
		boolean isShowingLineNumbers = EditorsUI.getPreferenceStore()
				.getBoolean(
						AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER);
		if (showLineNumbers instanceof ActionContributionItem) {
			((ActionContributionItem) showLineNumbers).getAction()
					.setChecked(isShowingLineNumbers);
		}
		if (isShowingLineNumbers) {
			// Add an action to toggle between physical and logical line numbers
			boolean plain = lineNumberColumn.isPlain();
			IAction togglePlain = new Action(
					UIText.DiffEditorPage_ToggleLineNumbers,
					IAction.AS_CHECK_BOX) {

				@Override
				public void run() {
					plainLineNumbers = !plain;
					lineNumberColumn.setPlain(!plain);
				}
			};
			togglePlain.setChecked(!plain);
			menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS,
					togglePlain);
		}
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
		return new String[] { IHistoryView.VIEW_ID, RepositoriesView.VIEW_ID };
	}

	// Diff specifics:

	private void setFolding() {
		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();
		if (viewer == null) {
			return;
		}
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
			currentFoldingAnnotations = null;
		}
	}

	private void setOverviewAnnotations() {
		IDocumentProvider documentProvider = getDocumentProvider();
		IDocument document = documentProvider.getDocument(getEditorInput());
		if (!(document instanceof DiffDocument)) {
			return;
		}
		IAnnotationModel annotationModel = documentProvider
				.getAnnotationModel(getEditorInput());
		if (annotationModel == null) {
			return;
		}
		DiffRegion[] diffs = ((DiffDocument) document).getRegions();
		if (diffs == null || diffs.length == 0) {
			return;
		}
		Map<Annotation, Position> newAnnotations = new HashMap<>();
		for (DiffRegion region : diffs) {
			if (DiffRegion.Type.ADD.equals(region.getType())) {
				newAnnotations.put(
						new Annotation(ADD_ANNOTATION_TYPE, true, null),
						new Position(region.getOffset(), region.getLength()));
			} else if (DiffRegion.Type.REMOVE.equals(region.getType())) {
				newAnnotations.put(
						new Annotation(REMOVE_ANNOTATION_TYPE, true, null),
						new Position(region.getOffset(), region.getLength()));
			}
		}
		if (annotationModel instanceof IAnnotationModelExtension) {
			((IAnnotationModelExtension) annotationModel).replaceAnnotations(
					currentOverviewAnnotations, newAnnotations);
		} else {
			if (currentOverviewAnnotations != null) {
				for (Annotation existing : currentOverviewAnnotations) {
					annotationModel.removeAnnotation(existing);
				}
			}
			for (Map.Entry<Annotation, Position> entry : newAnnotations
					.entrySet()) {
				annotationModel.addAnnotation(entry.getKey(), entry.getValue());
			}
		}
		currentOverviewAnnotations = newAnnotations.keySet()
				.toArray(new Annotation[newAnnotations.size()]);
	}

	private FileDiffRegion getFileDiffRange(int widgetOffset) {
		DiffViewer viewer = (DiffViewer) getSourceViewer();
		if (viewer != null) {
			int offset = viewer.widgetOffset2ModelOffset(widgetOffset);
			IDocument document = getDocumentProvider()
					.getDocument(getEditorInput());
			if (document instanceof DiffDocument) {
				return ((DiffDocument) document).findFileRegion(offset);
			}
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
		FileDiff[] result = diffResult.toArray(new FileDiff[diffResult.size()]);
		Arrays.sort(result, FileDiff.PATH_COMPARATOR);
		return result;
	}

	/**
	 * Asynchronously gets the diff of the commit set on our
	 * {@link CommitEditorInput}, formats it into a {@link DiffDocument}, and
	 * then re-sets this editors's input to a {@link DiffEditorInput} which will
	 * cause this document to be shown.
	 */
	private void formatDiff() {
		RepositoryCommit commit = AdapterUtils.adapt(getEditor(),
				RepositoryCommit.class);
		if (commit == null) {
			return;
		}
		if (!commit.isStash() && commit.getRevCommit().getParentCount() > 1) {
			setInput(new DiffEditorInput(commit, null));
			return;
		}

		Job job = new Job(UIText.DiffEditorPage_TaskGeneratingDiff) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				FileDiff diffs[] = getDiffs(commit);
				DiffDocument document = new DiffDocument();
				try (DiffRegionFormatter formatter = new DiffRegionFormatter(
						document)) {
					SubMonitor progress = SubMonitor.convert(monitor,
							diffs.length);
					for (FileDiff diff : diffs) {
						if (progress.isCanceled()) {
							break;
						}
						progress.subTask(diff.getPath());
						try {
							formatter.write(diff);
						} catch (IOException ignore) {
							// Ignored
						}
						progress.worked(1);
					}
					document.connect(formatter);
				}
				new UIJob(UIText.DiffEditorPage_TaskUpdatingViewer) {

					@Override
					public IStatus runInUIThread(IProgressMonitor uiMonitor) {
						if (UIUtils.isUsable(getPartControl())) {
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

		public DiffEditorInput(RepositoryCommit commit, IDocument diff) {
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
					&& Objects.equals(document,
							((DiffEditorInput) obj).document);
		}

		@Override
		public int hashCode() {
			return super.hashCode() ^ Objects.hashCode(document);
		}
	}

	/**
	 * A document provider that knows about {@link DiffEditorInput}.
	 */
	private static class DiffDocumentProvider extends AbstractDocumentProvider {

		@Override
		public IStatus getStatus(Object element) {
			if (element instanceof CommitEditorInput) {
				RepositoryCommit commit = ((CommitEditorInput) element)
						.getCommit();
				if (commit != null && !commit.isStash()
						&& commit.getRevCommit() != null
						&& commit.getRevCommit().getParentCount() > 1) {
					return Activator.createErrorStatus(
							UIText.DiffEditorPage_WarningNoDiffForMerge);
				}
			}
			return Status.OK_STATUS;
		}

		@Override
		protected IDocument createDocument(Object element)
				throws CoreException {
			if (element instanceof DiffEditorInput) {
				IDocument document = ((DiffEditorInput) element).getDocument();
				if (document != null) {
					return document;
				}
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

	/**
	 * An ephemeral {@link PreferenceStore} that sets the annotation colors
	 * based on the theme-dependent colors defined for the line backgrounds in a
	 * unified diff. This ensures that the annotation colors are always
	 * consistent with the line backgrounds. Plus the user doesn't have to
	 * configure several related colors, and the annotations update when the
	 * theme changes.
	 */
	private static class ThemePreferenceStore extends PreferenceStore {

		// The colors defined in plugin.xml for these annotations are not taken
		// into account. We always use auto-computed colors based on the current
		// settings for the line background colors.

		private static final String ADD_ANNOTATION_COLOR_PREFERENCE = "org.eclipse.egit.ui.commitEditor.diffAddedColor"; //$NON-NLS-1$

		private static final String REMOVE_ANNOTATION_COLOR_PREFERENCE = "org.eclipse.egit.ui.commitEditor.diffRemovedColor"; //$NON-NLS-1$

		private final IPropertyChangeListener listener = event -> {
			String property = event.getProperty();
			if (IThemeManager.CHANGE_CURRENT_THEME.equals(property)) {
				setColorRegistry();
				initColors();
			} else if (THEME_DiffAddBackgroundColor.equals(property)
					|| THEME_DiffRemoveBackgroundColor.equals(property)) {
				initColors();
			}
		};

		private ColorRegistry currentColors;

		public ThemePreferenceStore() {
			super();
			setColorRegistry();
			initColors();
			PlatformUI.getWorkbench().getThemeManager()
					.addPropertyChangeListener(listener);
		}

		private void setColorRegistry() {
			if (currentColors != null) {
				currentColors.removeListener(listener);
			}
			currentColors = PlatformUI.getWorkbench().getThemeManager()
					.getCurrentTheme().getColorRegistry();
			currentColors.addListener(listener);
		}

		private void initColors() {
			// The overview ruler tones down colors. Since our background colors
			// usually are already rather pale, let's saturate them more and
			// brighten them, otherwise the annotations will be barely visible.
			RGB rgb = adjust(currentColors.getRGB(THEME_DiffAddBackgroundColor),
					4.0);
			setValue(ADD_ANNOTATION_COLOR_PREFERENCE,
					StringConverter.asString(rgb));
			rgb = adjust(currentColors.getRGB(THEME_DiffRemoveBackgroundColor),
					4.0);
			setValue(REMOVE_ANNOTATION_COLOR_PREFERENCE,
					StringConverter.asString(rgb));
		}

		/**
		 * Increases the saturation (simple multiplier), and brightens dark
		 * colors.
		 *
		 * @param rgb
		 *            to modify
		 * @param saturation
		 *            multiplier
		 * @return A new {@link RGB} for the new saturated and possibly
		 *         brightened color
		 */
		private RGB adjust(RGB rgb, double saturation) {
			float[] hsb = rgb.getHSB();
			// We also brighten the color because otherwise the color
			// manipulations in OverviewRuler result in a fill color barely
			// discernible from a dark background.
			return new RGB(hsb[0], (float) Math.min(hsb[1] * saturation, 1.0),
					hsb[2] < 0.5 ? hsb[2] * 2 : hsb[2]);
		}

		public void dispose() {
			PlatformUI.getWorkbench().getThemeManager()
					.removePropertyChangeListener(listener);
			if (currentColors != null) {
				currentColors.removeListener(listener);
				currentColors = null;
			}
		}
	}
}
