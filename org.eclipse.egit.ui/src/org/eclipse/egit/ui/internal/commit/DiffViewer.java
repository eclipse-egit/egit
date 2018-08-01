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
 *    Tobias Pfeifer (SAP AG) - customizable font and color for the first header line - https://bugs.eclipse.org/397723
 *    Thomas Wolf <thomas.wolf@paranor.ch> - add hyperlinks, and use JFace syntax coloring
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static org.eclipse.egit.ui.UIPreferences.THEME_DiffAddBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffAddForegroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHeadlineBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHeadlineFont;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHeadlineForegroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHunkBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffHunkForegroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffRemoveBackgroundColor;
import static org.eclipse.egit.ui.UIPreferences.THEME_DiffRemoveForegroundColor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.EgitUiEditorUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.DiffRegion;
import org.eclipse.egit.ui.internal.commit.DiffRegionFormatter.FileDiffRegion;
import org.eclipse.egit.ui.internal.dialogs.HyperlinkSourceViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetectorExtension2;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Source viewer to display one or more file differences using standard editor
 * colors and fonts preferences. Should be used together with a
 * {@link DiffDocument} to get proper coloring and hyperlink support.
 */
public class DiffViewer extends HyperlinkSourceViewer {

	private final Map<String, IToken> tokens = new HashMap<>();

	private final Map<String, Color> backgroundColors = new HashMap<>();

	private IPropertyChangeListener themeListener = new IPropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (IThemeManager.CHANGE_CURRENT_THEME.equals(property)
					|| THEME_DiffAddBackgroundColor.equals(property)
					|| THEME_DiffAddForegroundColor.equals(property)
					|| THEME_DiffHunkBackgroundColor.equals(property)
					|| THEME_DiffHunkForegroundColor.equals(property)
					|| THEME_DiffHeadlineBackgroundColor.equals(property)
					|| THEME_DiffHeadlineForegroundColor.equals(property)
					|| THEME_DiffHeadlineFont.equals(property)
					|| THEME_DiffRemoveBackgroundColor.equals(property)
					|| THEME_DiffRemoveForegroundColor.equals(property)) {
				refreshDiffStyles();
				invalidateTextPresentation();
			}
		}
	};

	/**
	 * A configuration to use with a {@link DiffViewer}, setting up the syntax
	 * coloring for a diff and adding the {@link IHyperlinkDetector} for the
	 * links.
	 */
	public static class Configuration
			extends HyperlinkSourceViewer.Configuration {

		/**
		 * Creates a new {@link Configuration} connected to the given
		 * {@link IPreferenceStore}.
		 *
		 * @param preferenceStore
		 *            to connect to
		 */
		public Configuration(IPreferenceStore preferenceStore) {
			super(preferenceStore);
		}

		@Override
		public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
			return SWT.NONE;
		}

		@Override
		protected IHyperlinkDetector[] internalGetHyperlinkDetectors(
				ISourceViewer sourceViewer) {
			Assert.isTrue(sourceViewer instanceof DiffViewer);
			IHyperlinkDetector[] result = { new HyperlinkDetector() };
			return result;
		}

		@Override
		public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
			Assert.isTrue(sourceViewer instanceof DiffViewer);
			DiffViewer viewer = (DiffViewer) sourceViewer;
			return viewer.tokens.keySet()
					.toArray(new String[viewer.tokens.size()]);
		}

		@Override
		public IPresentationReconciler getPresentationReconciler(
				ISourceViewer sourceViewer) {
			Assert.isTrue(sourceViewer instanceof DiffViewer);
			DiffViewer viewer = (DiffViewer) sourceViewer;
			PresentationReconciler reconciler = new PresentationReconciler();
			reconciler.setDocumentPartitioning(
					getConfiguredDocumentPartitioning(viewer));
			for (String contentType : viewer.tokens.keySet()) {
				DefaultDamagerRepairer damagerRepairer = new DefaultDamagerRepairer(
						new SingleTokenScanner(
								() -> viewer.tokens.get(contentType)));
				reconciler.setDamager(damagerRepairer, contentType);
				reconciler.setRepairer(damagerRepairer, contentType);
			}
			return reconciler;
		}
	}

	/**
	 * Creates a new {@link DiffViewer}.
	 *
	 * @param parent
	 *            to contain the viewer
	 * @param ruler
	 *            for the viewer (left side)
	 * @param styles
	 *            for the viewer
	 */
	public DiffViewer(Composite parent, IVerticalRuler ruler, int styles) {
		this(parent, ruler, null, false, styles);
	}

	/**
	 * Creates a new {@link DiffViewer}.
	 *
	 * @param parent
	 *            to contain the viewer
	 * @param ruler
	 *            for the viewer (left side)
	 * @param overviewRuler
	 *            ruler for overview annotations
	 * @param showsAnnotationOverview
	 *            whether to show overview annotations
	 * @param styles
	 *            for the viewer
	 */
	public DiffViewer(Composite parent, IVerticalRuler ruler,
			IOverviewRuler overviewRuler, boolean showsAnnotationOverview,
			int styles) {
		super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
		getTextWidget().setAlwaysShowScrollBars(false);
		setEditable(false);
		setDocument(new Document());
		initListeners();
		getTextWidget()
				.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		refreshDiffStyles();
	}

	@Override
	protected void handleDispose() {
		PlatformUI.getWorkbench().getThemeManager()
				.removePropertyChangeListener(themeListener);
		super.handleDispose();
	}

	@Override
	public void configure(SourceViewerConfiguration config) {
		Assert.isTrue(config instanceof Configuration);
		super.configure(config);
	}

	@Override
	protected Layout createLayout() {
		return new FixedRulerLayout(GAP_SIZE_1);
	}

	private class FixedRulerLayout extends RulerLayout {

		public FixedRulerLayout(int gap) {
			super(gap);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle bounds = composite.getBounds();
			if (bounds.width == 0 || bounds.height == 0) {
				// The overview ruler is laid out wrongly in the DiffEditorPage:
				// it ends up with a negative y-coordinate. This seems to be
				// caused by layout attempts while the page is not visible,
				// which cache that bogus negative offset in RulerLayout, which
				// will re-use it even when the viewer is laid out again when
				// the page is visible. So don't layout if the containing
				// composite has no extent.
				return;
			}
			super.layout(composite, flushCache);
		}
	}

	private void refreshDiffStyles() {
		ColorRegistry col = PlatformUI.getWorkbench().getThemeManager()
				.getCurrentTheme().getColorRegistry();
		FontRegistry reg = PlatformUI.getWorkbench().getThemeManager()
				.getCurrentTheme().getFontRegistry();
		// We do the foreground via syntax coloring and the background via a
		// line background listener. If we did the background also via the
		// TextAttributes, this would take precedence over the line background
		// resulting in strange display if the current line is highlighted:
		// that highlighting would appear only beyond the end of the actual
		// text content (i.e., beyond the end-of-line), while actual text
		// would still get the background from the attribute.
		tokens.put(IDocument.DEFAULT_CONTENT_TYPE, new Token(null));
		tokens.put(DiffDocument.HEADLINE_CONTENT_TYPE,
				new Token(new TextAttribute(
						col.get(THEME_DiffHeadlineForegroundColor), null,
						SWT.NORMAL, reg.get(THEME_DiffHeadlineFont))));
		tokens.put(DiffDocument.HUNK_CONTENT_TYPE, new Token(
				new TextAttribute(col.get(THEME_DiffHunkForegroundColor))));
		tokens.put(DiffDocument.ADDED_CONTENT_TYPE, new Token(
				new TextAttribute(col.get(THEME_DiffAddForegroundColor))));
		tokens.put(DiffDocument.REMOVED_CONTENT_TYPE, new Token(
				new TextAttribute(col.get(THEME_DiffRemoveForegroundColor))));
		backgroundColors.put(DiffDocument.HEADLINE_CONTENT_TYPE,
				col.get(THEME_DiffHeadlineBackgroundColor));
		backgroundColors.put(DiffDocument.HUNK_CONTENT_TYPE,
				col.get(THEME_DiffHunkBackgroundColor));
		backgroundColors.put(DiffDocument.ADDED_CONTENT_TYPE,
				col.get(THEME_DiffAddBackgroundColor));
		backgroundColors.put(DiffDocument.REMOVED_CONTENT_TYPE,
				col.get(THEME_DiffRemoveBackgroundColor));
	}

	private void initListeners() {
		PlatformUI.getWorkbench().getThemeManager()
				.addPropertyChangeListener(this.themeListener);
		getTextWidget().addLineBackgroundListener((event) -> {
			IDocument document = getDocument();
			if (document instanceof DiffDocument) {
				try {
					// We are in SWT land here: we get widget offsets.
					int modelOffset = widgetOffset2ModelOffset(
							event.lineOffset);
					ITypedRegion partition = ((DiffDocument) document)
							.getPartition(modelOffset);
					if (partition != null) {
						Color color = backgroundColors.get(partition.getType());
						if (color != null) {
							event.lineBackground = color;
						}
					}
				} catch (BadLocationException e) {
					// Ignore
				}
			}
		});
	}

	@Override
	protected void handleJFacePreferencesChange(PropertyChangeEvent event) {
		if (JFaceResources.TEXT_FONT.equals(event.getProperty())) {
			setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		} else {
			super.handleJFacePreferencesChange(event);
		}
	}

	private static class SingleTokenScanner implements ITokenScanner {

		private final Supplier<IToken> token;

		private int currentOffset;

		private int end;

		private int tokenStart;

		public SingleTokenScanner(Supplier<IToken> supplier) {
			this.token = supplier;
		}

		@Override
		public void setRange(IDocument document, int offset, int length) {
			currentOffset = offset;
			end = offset + length;
			tokenStart = -1;
		}

		@Override
		public IToken nextToken() {
			tokenStart = currentOffset;
			if (currentOffset < end) {
				currentOffset = end;
				return token.get();
			}
			return Token.EOF;
		}

		@Override
		public int getTokenOffset() {
			return tokenStart;
		}

		@Override
		public int getTokenLength() {
			return currentOffset - tokenStart;
		}

	}

	private static class HyperlinkDetector extends AbstractHyperlinkDetector
			implements IHyperlinkDetectorExtension2 {

		private final Pattern HUNK_LINE_PATTERN = Pattern
				.compile("@@ ([-+]?(\\d+),\\d+) ([-+]?(\\d+),\\d+) @@"); //$NON-NLS-1$

		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			IDocument document = textViewer.getDocument();
			if (!(document instanceof DiffDocument)
					|| document.getLength() == 0) {
				return null;
			}
			DiffDocument diffDocument = (DiffDocument) document;
			DiffRegion[] regions = diffDocument.getRegions();
			FileDiffRegion[] fileRegions = diffDocument.getFileRegions();
			if (regions == null || regions.length == 0 || fileRegions == null
					|| fileRegions.length == 0) {
				return null;
			}
			int start = region.getOffset();
			int end = region.getOffset() + region.getLength();
			DiffRegion key = new DiffRegion(start, 0);
			int i = Arrays.binarySearch(regions, key, (a, b) -> {
				if (a.getOffset() > b.getOffset() + b.getLength()) {
					return 1;
				}
				if (a.getOffset() + a.getLength() < b.getOffset()) {
					return -1;
				}
				return 0;
			});
			List<IHyperlink> links = new ArrayList<>();
			FileDiffRegion fileRange = null;
			for (; i >= 0 && i < regions.length; i++) {
				DiffRegion range = regions[i];
				if (range.getOffset() >= end) {
					break;
				}
				if (range.getOffset() + range.getLength() <= start) {
					continue;
				}
				// Range overlaps region
				switch (range.getType()) {
				case HEADLINE:
					fileRange = findFileRange(diffDocument, fileRange,
							range.getOffset());
					if (fileRange != null) {
						DiffEntry.ChangeType change = fileRange.getDiff()
								.getChange();
						switch (change) {
						case ADD:
						case DELETE:
							break;
						default:
							if (getString(document, range.getOffset(),
									range.getLength()).startsWith("diff")) { //$NON-NLS-1$
								// "diff" is at the beginning
								IRegion linkRegion = new Region(
										range.getOffset(), 4);
								if (TextUtilities.overlaps(region,
										linkRegion)) {
									links.add(new CompareLink(linkRegion,
											fileRange, -1));
								}
							}
							break;
						}
					}
					break;
				case HEADER:
					fileRange = findFileRange(diffDocument, fileRange,
							range.getOffset());
					if (fileRange != null) {
						String line = getString(document, range.getOffset(),
								range.getLength());
						createHeaderLinks((DiffDocument) document, region,
								fileRange, range, line, DiffEntry.Side.OLD,
								links);
						createHeaderLinks((DiffDocument) document, region,
								fileRange, range, line, DiffEntry.Side.NEW,
								links);
					}
					break;
				case HUNK:
					fileRange = findFileRange(diffDocument, fileRange,
							range.getOffset());
					if (fileRange != null) {
						String line = getString(document, range.getOffset(),
								range.getLength());
						Matcher m = HUNK_LINE_PATTERN.matcher(line);
						if (m.find()) {
							int lineOffset = getContextLines(document, range,
									i + 1 < regions.length ? regions[i + 1]
											: null);
							createHunkLinks(region, fileRange, range, m,
									lineOffset, links);
						}
					}
					break;
				default:
					break;
				}
			}
			if (links.isEmpty()) {
				return null;
			}
			return links.toArray(new IHyperlink[links.size()]);
		}

		private String getString(IDocument document, int offset, int length) {
			try {
				return document.get(offset, length);
			} catch (BadLocationException e) {
				return ""; //$NON-NLS-1$
			}
		}

		private int getContextLines(IDocument document, DiffRegion hunk,
				DiffRegion next) {
			if (next != null) {
				try {
					switch (next.getType()) {
					case CONTEXT:
						int nofLines = document.getNumberOfLines(
								next.getOffset(), next.getLength());
						return nofLines - 1;
					case ADD:
					case REMOVE:
						int hunkLine = document
								.getLineOfOffset(hunk.getOffset());
						int diffLine = document
								.getLineOfOffset(next.getOffset());
						return diffLine - hunkLine - 1;
					default:
						break;
					}
				} catch (BadLocationException e) {
					// Ignore
				}
			}
			return 0;
		}

		private FileDiffRegion findFileRange(DiffDocument document,
				FileDiffRegion candidate, int offset) {
			if (candidate != null && TextUtilities.overlaps(candidate,
					new Region(offset, 0))) {
				return candidate;
			}
			return document.findFileRegion(offset);
		}

		private void createHeaderLinks(DiffDocument document, IRegion region,
				FileDiffRegion fileRange, DiffRegion range, String line,
				@NonNull DiffEntry.Side side, List<IHyperlink> links) {
			Pattern p = document.getPathPattern(side);
			if (p == null) {
				return;
			}
			DiffEntry.ChangeType change = fileRange.getDiff().getChange();
			switch (side) {
			case OLD:
				if (change == DiffEntry.ChangeType.ADD) {
					return;
				}
				break;
			default:
				if (change == DiffEntry.ChangeType.DELETE) {
					return;
				}
				break;

			}
			Matcher m = p.matcher(line);
			if (m.find()) {
				IRegion linkRegion = new Region(range.getOffset() + m.start(),
						m.end() - m.start());
				if (TextUtilities.overlaps(region, linkRegion)) {
					if (side == DiffEntry.Side.NEW) {
						File file = new Path(fileRange.getDiff().getRepository()
								.getWorkTree().getAbsolutePath()).append(
										fileRange.getDiff().getNewPath())
										.toFile();
						if (file.exists()) {
							links.add(new FileLink(linkRegion, file, -1));
						}
					}
					links.add(new OpenLink(linkRegion, fileRange, side, -1));
				}
			}
		}

		private void createHunkLinks(IRegion region, FileDiffRegion fileRange,
				DiffRegion range, Matcher m, int lineOffset,
				List<IHyperlink> links) {
			DiffEntry.ChangeType change = fileRange.getDiff().getChange();
			if (change != DiffEntry.ChangeType.ADD) {
				IRegion linkRegion = new Region(range.getOffset() + m.start(1),
						m.end(1) - m.start(1));
				if (TextUtilities.overlaps(linkRegion, region)) {
					int lineNo = Integer.parseInt(m.group(2)) - 1 + lineOffset;
					if (change != DiffEntry.ChangeType.DELETE) {
						links.add(
								new CompareLink(linkRegion, fileRange, lineNo));
					}
					links.add(new OpenLink(linkRegion, fileRange,
							DiffEntry.Side.OLD, lineNo));
				}
			}
			if (change != DiffEntry.ChangeType.DELETE) {
				IRegion linkRegion = new Region(range.getOffset() + m.start(3),
						m.end(3) - m.start(3));
				if (TextUtilities.overlaps(linkRegion, region)) {
					int lineNo = Integer.parseInt(m.group(4)) - 1 + lineOffset;
					if (change != DiffEntry.ChangeType.ADD) {
						links.add(
								new CompareLink(linkRegion, fileRange, lineNo));
					}
					File file = new Path(
							fileRange.getDiff().getRepository().getWorkTree()
							.getAbsolutePath())
									.append(fileRange.getDiff().getNewPath())
									.toFile();
					if (file.exists()) {
						links.add(new FileLink(linkRegion, file, lineNo));
					}
					links.add(new OpenLink(linkRegion, fileRange,
							DiffEntry.Side.NEW, lineNo));
				}
			}
		}

		@Override
		public int getStateMask() {
			return -1;
		}
	}

	private static abstract class RevealLink implements IHyperlink {

		private final IRegion region;

		protected final int lineNo;

		protected RevealLink(IRegion region, int lineNo) {
			this.region = region;
			this.lineNo = lineNo;
		}

		@Override
		public IRegion getHyperlinkRegion() {
			return region;
		}

		@Override
		public String getTypeLabel() {
			return null;
		}

	}

	private static class FileLink extends RevealLink {

		private final File file;

		public FileLink(IRegion region, File file, int lineNo) {
			super(region, lineNo);
			this.file = file;
		}

		@Override
		public String getHyperlinkText() {
			return UIText.DiffViewer_OpenWorkingTreeLinkLabel;
		}

		@Override
		public void open() {
			openFileInEditor(file, lineNo);
		}

	}

	private static class CompareLink extends RevealLink {

		protected final FileDiff fileDiff;

		public CompareLink(IRegion region, FileDiffRegion fileRange,
				int lineNo) {
			super(region, lineNo);
			this.fileDiff = fileRange.getDiff();
		}

		@Override
		public String getHyperlinkText() {
			return UIText.DiffViewer_OpenComparisonLinkLabel;
		}

		@Override
		public void open() {
			// No way to selectAndReveal a line or a diff node in a
			// CompareEditor?
			showTwoWayFileDiff(fileDiff);
		}

	}

	private static class OpenLink extends CompareLink {

		private final DiffEntry.Side side;

		public OpenLink(IRegion region, FileDiffRegion fileRange,
				DiffEntry.Side side, int lineNo) {
			super(region, fileRange, lineNo);
			this.side = side;
		}

		@Override
		public String getHyperlinkText() {
			switch (side) {
			case OLD:
				return UIText.DiffViewer_OpenPreviousLinkLabel;
			default:
				return UIText.DiffViewer_OpenInEditorLinkLabel;
			}
		}

		@Override
		public void open() {
			openInEditor(fileDiff, side, lineNo);
		}

	}

	/**
	 * Opens the file, if it exists, in an editor.
	 *
	 * @param file
	 *            to open
	 * @param lineNoToReveal
	 *            if >= 0, select and reveals the given line
	 */
	public static void openFileInEditor(File file, int lineNoToReveal) {
		if (!file.exists()) {
			Activator.showError(
					NLS.bind(UIText.DiffViewer_FileDoesNotExist,
							file.getPath()),
					null);
			return;
		}
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = EgitUiEditorUtils.openEditor(file, page);
		EgitUiEditorUtils.revealLine(editor, lineNoToReveal);
	}

	/**
	 * Opens either the new or the old version of a {@link FileDiff} in an
	 * editor.
	 *
	 * @param d
	 *            the {@link FileDiff}
	 * @param side
	 *            to show
	 * @param lineNoToReveal
	 *            if >= 0, select and reveals the given line
	 */
	public static void openInEditor(FileDiff d, DiffEntry.Side side,
			int lineNoToReveal) {
		ObjectId[] blobs = d.getBlobs();
		switch (side) {
		case OLD:
			openInEditor(d.getRepository(), d.getOldPath(),
					d.getCommit().getParent(0),
					blobs[0], lineNoToReveal);
			break;
		default:
			openInEditor(d.getRepository(), d.getNewPath(), d.getCommit(),
					blobs[blobs.length - 1], lineNoToReveal);
			break;
		}
	}

	private static void openInEditor(Repository repository, String path,
			RevCommit commit, ObjectId blob, int reveal) {
		try {
			IFileRevision rev = CompareUtils.getFileRevision(path, commit,
					repository, blob);
			if (rev == null) {
				String message = NLS.bind(
						UIText.DiffViewer_notContainedInCommit, path,
						commit.getName());
				Activator.showError(message, null);
				return;
			}
			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();
			IEditorPart editor = EgitUiEditorUtils.openEditor(page, rev,
					new NullProgressMonitor());
			EgitUiEditorUtils.revealLine(editor, reveal);
		} catch (IOException | CoreException e) {
			Activator.handleError(UIText.GitHistoryPage_openFailed, e, true);
		}
	}

	/**
	 * Shows a two-way diff between the old and new versions of a
	 * {@link FileDiff} in a compare editor.
	 *
	 * @param d
	 *            the {@link FileDiff} to show
	 */
	public static void showTwoWayFileDiff(FileDiff d) {
		String np = d.getNewPath();
		String op = d.getOldPath();
		RevCommit c = d.getCommit();
		ObjectId[] blobs = d.getBlobs();

		// extract commits
		final RevCommit oldCommit;
		final ObjectId oldObjectId;
		if (!d.getChange().equals(ChangeType.ADD)) {
			oldCommit = c.getParent(0);
			oldObjectId = blobs[0];
		} else {
			// Initial import
			oldCommit = null;
			oldObjectId = null;
		}

		final RevCommit newCommit;
		final ObjectId newObjectId;
		if (d.getChange().equals(ChangeType.DELETE)) {
			newCommit = null;
			newObjectId = null;
		} else {
			newCommit = c;
			newObjectId = blobs[blobs.length - 1];
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		Repository repository = d.getRepository();
		if (oldCommit != null && newCommit != null && repository != null) {
			IFile file = np != null
					? ResourceUtil.getFileForLocation(repository, np, false)
					: null;
			try {
				if (file != null) {
					CompareUtils.compare(file, repository, np, op,
							newCommit.getName(), oldCommit.getName(), false,
							page);
				} else {
					CompareUtils.compareBetween(repository, np, op,
							newCommit.getName(), oldCommit.getName(),
							page);
				}
			} catch (IOException e) {
				Activator.handleError(UIText.GitHistoryPage_openFailed, e,
						true);
			}
			return;
		}

		// still happens on initial commits
		final ITypedElement oldSide = createTypedElement(repository, op,
				oldCommit, oldObjectId);
		final ITypedElement newSide = createTypedElement(repository, np,
				newCommit, newObjectId);
		CompareUtils.openInCompare(page,
				new GitCompareFileRevisionEditorInput(newSide, oldSide, null));
	}

	private static ITypedElement createTypedElement(Repository repository,
			String path, final RevCommit commit, final ObjectId objectId) {
		if (null != commit) {
			return CompareUtils.getFileRevisionTypedElement(path, commit,
					repository, objectId);
		} else {
			return new GitCompareFileRevisionEditorInput.EmptyTypedElement(""); //$NON-NLS-1$
		}
	}

}
