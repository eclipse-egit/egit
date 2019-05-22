/*******************************************************************************
 * Copyright (C) 2010, 2015 Benjamin Muskalla <bmuskalla@eclipsesource.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Benjamin Muskalla (EclipseSource) - initial implementation
 *    Tomasz Zarna (IBM) - show whitespace action, bug 371353
 *    Wayne Beaton (Eclipse Foundation) - Bug 433721
 *    Thomas Wolf (Paranor) - Hyperlink syntax coloring; bug 471355
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.ActionUtils.UpdateableAction;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubMenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.MarginPainter;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.util.IntList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BidiSegmentEvent;
import org.eclipse.swt.custom.BidiSegmentListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Text field with support for spellchecking.
 */
public class SpellcheckableMessageArea extends Composite {

	static final int MAX_LINE_WIDTH = 72;

	private static final Pattern TRAILING_WHITE_SPACE_ON_LINES = Pattern
			.compile("\\h+$", Pattern.MULTILINE); //$NON-NLS-1$

	private static final Pattern TRAILING_NEWLINES = Pattern.compile("\\v+$"); //$NON-NLS-1$

	private final HyperlinkSourceViewer sourceViewer;

	private TextSourceViewerConfiguration configuration;

	private BidiSegmentListener hardWrapSegmentListener;

	// XXX: workaround for https://bugs.eclipse.org/400727
	private int brokenBidiPlatformTextWidth;

	private IAction contentAssistAction;

	/**
	 * @param parent
	 * @param initialText
	 */
	public SpellcheckableMessageArea(Composite parent, String initialText) {
		this(parent, initialText, SWT.BORDER);
	}

	/**
	 * @param parent
	 * @param initialText
	 * @param styles
	 */
	public SpellcheckableMessageArea(Composite parent, String initialText,
			int styles) {
		this(parent, initialText, false, styles);
	}

	/**
	 * @param parent
	 * @param initialText
	 * @param readOnly
	 * @param styles
	 */
	public SpellcheckableMessageArea(Composite parent, String initialText,
			boolean readOnly, int styles) {
		super(parent, styles);
		setLayout(new FillLayout());

		AnnotationModel annotationModel = new AnnotationModel();
		sourceViewer = new HyperlinkSourceViewer(this, null,
				SWT.MULTI | SWT.V_SCROLL | SWT.WRAP) {
			@Override
			protected void handleJFacePreferencesChange(
					PropertyChangeEvent event) {
				if (JFaceResources.TEXT_FONT.equals(event.getProperty())) {
					Font themeFont = UIUtils.getFont(
							UIPreferences.THEME_CommitMessageEditorFont);
					Font jFaceFont = JFaceResources.getTextFont();
					if (themeFont.equals(jFaceFont)) {
						setFont(jFaceFont);
					}
				} else {
					super.handleJFacePreferencesChange(event);
				}
			}
		};
		getTextWidget().setAlwaysShowScrollBars(false);
		getTextWidget().setFont(UIUtils
				.getFont(UIPreferences.THEME_CommitMessageEditorFont));
		sourceViewer.setDocument(new Document());
		int endSpacing = 2;
		int textWidth = getCharWidth() * MAX_LINE_WIDTH + endSpacing;
		int textHeight = getLineHeight() * 7;
		Point size = getTextWidget().computeSize(textWidth, textHeight);
		getTextWidget().setSize(size);

		computeBrokenBidiPlatformTextWidth(size.x);

		getTextWidget().setEditable(!readOnly);

		createMarginPainter();

		configureHardWrap();

		final IPropertyChangeListener propertyChangeListener = event -> {
			if (UIPreferences.COMMIT_DIALOG_HARD_WRAP_MESSAGE
					.equals(event.getProperty())) {
				getDisplay().asyncExec(() -> {
					if (!isDisposed()) {
						configureHardWrap();
						if (brokenBidiPlatformTextWidth != -1) {
							layout();
						}
					}
				});
			}
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);
		final IPropertyChangeListener themeListener = event -> {
			String property = event.getProperty();
			if (IThemeManager.CHANGE_CURRENT_THEME.equals(property)
					|| UIPreferences.THEME_CommitMessageEditorFont
							.equals(property)) {
				Font themeFont = UIUtils
						.getFont(UIPreferences.THEME_CommitMessageEditorFont);
				getDisplay().asyncExec(() -> {
					if (!isDisposed()) {
						sourceViewer.setFont(themeFont);
					}
				});
			}
		};
		PlatformUI.getWorkbench().getThemeManager()
				.addPropertyChangeListener(themeListener);

		final SourceViewerDecorationSupport support = configureAnnotationPreferences();

		Document document = new Document(initialText);

		configuration = new HyperlinkSourceViewer.Configuration(
				EditorsUI.getPreferenceStore()) {

			@Override
			public int getHyperlinkStateMask(ISourceViewer targetViewer) {
				if (!targetViewer.isEditable()) {
					return SWT.NONE;
				}
				return super.getHyperlinkStateMask(targetViewer);
			}

			@Override
			protected Map getHyperlinkDetectorTargets(ISourceViewer targetViewer) {
				return getHyperlinkTargets();
			}

			@Override
			public IReconciler getReconciler(ISourceViewer viewer) {
				if (!isEditable(viewer))
					return null;
				return super.getReconciler(sourceViewer);
			}

			@Override
			public IContentAssistant getContentAssistant(ISourceViewer viewer) {
				if (!viewer.isEditable())
					return null;
				IContentAssistant assistant = createContentAssistant(viewer);
				// Add content assist proposal handler if assistant exists
				if (assistant != null)
					contentAssistAction = createContentAssistAction(
							sourceViewer);
				return assistant;
			}

			@Override
			public IPresentationReconciler getPresentationReconciler(
					ISourceViewer viewer) {
				PresentationReconciler reconciler = new PresentationReconciler();
				reconciler.setDocumentPartitioning(
						getConfiguredDocumentPartitioning(viewer));
				DefaultDamagerRepairer hyperlinkDamagerRepairer = new DefaultDamagerRepairer(
						new HyperlinkTokenScanner(this, viewer));
				reconciler.setDamager(hyperlinkDamagerRepairer,
						IDocument.DEFAULT_CONTENT_TYPE);
				reconciler.setRepairer(hyperlinkDamagerRepairer,
						IDocument.DEFAULT_CONTENT_TYPE);
				return reconciler;
			}

		};

		sourceViewer.configure(configuration);
		sourceViewer.setDocument(document, annotationModel);

		configureContextMenu();

		getTextWidget().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent disposeEvent) {
				support.uninstall();
				Activator.getDefault().getPreferenceStore().removePropertyChangeListener(propertyChangeListener);
				PlatformUI.getWorkbench().getThemeManager()
						.removePropertyChangeListener(themeListener);
			}
		});
	}

	private void computeBrokenBidiPlatformTextWidth(int textWidth) {
		class BidiSegmentListenerTester implements BidiSegmentListener {
			boolean called;

			@Override
			public void lineGetSegments(BidiSegmentEvent event) {
				called = true;
			}
		}
		BidiSegmentListenerTester tester = new BidiSegmentListenerTester();
		StyledText textWidget = getTextWidget();
		textWidget.addBidiSegmentListener(tester);
		textWidget.setText(" "); //$NON-NLS-1$
		textWidget.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		textWidget.removeBidiSegmentListener(tester);

		brokenBidiPlatformTextWidth = tester.called ? -1 : textWidth;
	}

	private boolean isEditable(ISourceViewer viewer) {
		return viewer != null && viewer.getTextWidget().getEditable();
	}

	private void configureHardWrap() {
		if (shouldHardWrap()) {
			if (hardWrapSegmentListener == null) {
				final StyledText textWidget = getTextWidget();
				hardWrapSegmentListener = new BidiSegmentListener() {
					@Override
					public void lineGetSegments(BidiSegmentEvent e) {
						if (e.widget == textWidget) {
							int footerOffset = CommonUtils
									.getFooterOffset(textWidget.getText());
							if (footerOffset >= 0
									&& e.lineOffset >= footerOffset) {
								return;
							}
						}
						int[] segments = calculateWrapOffsets(e.lineText, MAX_LINE_WIDTH);
						if (segments != null) {
							char[] segmentsChars = new char[segments.length];
							Arrays.fill(segmentsChars, '\n');
							e.segments = segments;
							e.segmentsChars = segmentsChars;
						}
					}
				};
				textWidget.addBidiSegmentListener(hardWrapSegmentListener);
				textWidget.setText(textWidget.getText()); // XXX: workaround for https://bugs.eclipse.org/384886

				if (brokenBidiPlatformTextWidth != -1) {
					Layout restrictedWidthLayout = new Layout() {
						@Override
						protected Point computeSize(Composite composite,
								int wHint, int hHint, boolean flushCache) {
							Point size = SpellcheckableMessageArea.this
									.getSize();
							Rectangle trim = SpellcheckableMessageArea.this
									.computeTrim(0, 0, 0, 0);
							size.x -= trim.width;
							size.y -= trim.height;
							if (size.x > brokenBidiPlatformTextWidth)
								size.x = brokenBidiPlatformTextWidth;
							return size;
						}

						@Override
						protected void layout(Composite composite,
								boolean flushCache) {
							Point size = computeSize(composite, SWT.DEFAULT,
									SWT.DEFAULT, flushCache);
							textWidget.setBounds(0, 0, size.x, size.y);
						}
					};
					setLayout(restrictedWidthLayout);
				}
			}

		} else if (hardWrapSegmentListener != null) {
			StyledText textWidget = getTextWidget();
			textWidget.removeBidiSegmentListener(hardWrapSegmentListener);
			textWidget.setText(textWidget.getText()); // XXX: workaround for https://bugs.eclipse.org/384886
			hardWrapSegmentListener = null;

			if (brokenBidiPlatformTextWidth != -1)
				setLayout(new FillLayout());
		}
	}

	private void configureContextMenu() {
		final boolean editable = isEditable(sourceViewer);
		IAction quickFixAction = null;
		if (editable) {
			quickFixAction = new QuickfixAction(sourceViewer);
		}

		final ShowWhitespaceAction showWhitespaceAction = new ShowWhitespaceAction(
				sourceViewer, !editable);
		MenuManager contextMenu = new MenuManager();
		UpdateableAction[] standardActions = ActionUtils
				.fillStandardTextActions(sourceViewer, editable, contextMenu);
		contextMenu.add(new Separator());
		contextMenu.add(showWhitespaceAction);
		contextMenu.add(new Separator());

		if (editable) {
			final SubMenuManager quickFixMenu = new SubMenuManager(contextMenu);
			quickFixMenu.setVisible(true);
			quickFixMenu.addMenuListener(new IMenuListener() {
				@Override
				public void menuAboutToShow(IMenuManager manager) {
					quickFixMenu.removeAll();
					addProposals(quickFixMenu);
				}
			});
		}

		final StyledText textWidget = getTextWidget();
		List<IAction> globalActions = new ArrayList<>();
		globalActions.addAll(Arrays.asList(standardActions));
		if (quickFixAction != null) {
			globalActions.add(quickFixAction);
		}
		if (contentAssistAction != null) {
			globalActions.add(contentAssistAction);
		}
		ActionUtils.setGlobalActions(textWidget, globalActions,
				getHandlerService());

		textWidget.setMenu(contextMenu.createContextMenu(textWidget));

		sourceViewer.addSelectionChangedListener(event -> {
			if (standardActions[ITextOperationTarget.CUT] != null) {
				standardActions[ITextOperationTarget.CUT].update();
			}
			standardActions[ITextOperationTarget.COPY].update();
		});

		if (editable) {
			sourceViewer.addTextListener(event -> {
				if (standardActions[ITextOperationTarget.UNDO] != null) {
					standardActions[ITextOperationTarget.UNDO].update();
				}
				if (standardActions[ITextOperationTarget.REDO] != null) {
					standardActions[ITextOperationTarget.REDO].update();
				}
			});
		}

		textWidget.addDisposeListener(e -> showWhitespaceAction.dispose());
	}

	private void addProposals(final SubMenuManager quickFixMenu) {
		IAnnotationModel sourceModel = sourceViewer.getAnnotationModel();
		if (sourceModel == null) {
			return;
		}
		Iterator annotationIterator = sourceModel.getAnnotationIterator();
		while (annotationIterator.hasNext()) {
			Annotation annotation = (Annotation) annotationIterator.next();
			boolean isDeleted = annotation.isMarkedDeleted();
			boolean isIncluded = !isDeleted
					&& includes(sourceModel.getPosition(annotation),
							getTextWidget().getCaretOffset());
			boolean isFixable = isIncluded && sourceViewer
					.getQuickAssistAssistant().canFix(annotation);
			if (isFixable) {
				IQuickAssistProcessor processor = sourceViewer
						.getQuickAssistAssistant().getQuickAssistProcessor();
				IQuickAssistInvocationContext context = sourceViewer
						.getQuickAssistInvocationContext();
				ICompletionProposal[] proposals = processor
						.computeQuickAssistProposals(context);

				for (ICompletionProposal proposal : proposals) {
					quickFixMenu.add(createQuickFixAction(proposal));
				}
			}
		}
	}

	private boolean includes(Position position, int caretOffset) {
		return position != null && (position.includes(caretOffset)
				|| (position.offset + position.length) == caretOffset);
	}

	private IAction createQuickFixAction(final ICompletionProposal proposal) {
		return new Action(proposal.getDisplayString()) {

			@Override
			public void run() {
				proposal.apply(sourceViewer.getDocument());
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				Image image = proposal.getImage();
				if (image != null)
					return ImageDescriptor.createFromImage(image);
				return null;
			}
		};
	}

	/**
	 * Return <code>IHandlerService</code>. The default implementation uses the
	 * workbench window's service locator. Subclasses may override to access the
	 * service by using a local service locator.
	 *
	 * @return <code>IHandlerService</code> using the workbench window's service
	 *         locator. Can be <code>null</code> if the service could not be
	 *         found.
	 */
	protected IHandlerService getHandlerService() {
		return CommonUtils.getService(PlatformUI.getWorkbench(), IHandlerService.class);
	}

	private SourceViewerDecorationSupport configureAnnotationPreferences() {
		ISharedTextColors textColors = EditorsUI.getSharedTextColors();
		IAnnotationAccess annotationAccess = new DefaultMarkerAnnotationAccess();
		final SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(
				sourceViewer, null, annotationAccess, textColors);

		List<AnnotationPreference> annotationPreferences = new MarkerAnnotationPreferences()
				.getAnnotationPreferences();
		annotationPreferences.iterator()
				.forEachRemaining(p -> support.setAnnotationPreference(p));

		support.install(EditorsUI.getPreferenceStore());
		return support;
	}

	/**
	 * Create margin painter and add to source viewer
	 */
	protected void createMarginPainter() {
		MarginPainter marginPainter = new MarginPainter(sourceViewer);
		marginPainter.setMarginRulerColumn(MAX_LINE_WIDTH);
		marginPainter.setMarginRulerColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(
				SWT.COLOR_GRAY));
		sourceViewer.addPainter(marginPainter);
	}

	private int getCharWidth() {
		GC gc = new GC(getTextWidget());
		int charWidth = gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();
		return charWidth;
	}

	private int getLineHeight() {
		return getTextWidget().getLineHeight();
	}

	/**
	 * @return if the commit message should be hard-wrapped (preference)
	 */
	private static boolean shouldHardWrap() {
		return Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.COMMIT_DIALOG_HARD_WRAP_MESSAGE);
	}

	/**
	 * @return widget
	 */
	public StyledText getTextWidget() {
		return sourceViewer.getTextWidget();
	}

	private static class QuickfixAction extends Action {

		private final ITextOperationTarget textOperationTarget;

		public QuickfixAction(ITextOperationTarget target) {
			textOperationTarget = target;
			setActionDefinitionId(
					ITextEditorActionDefinitionIds.QUICK_ASSIST);
		}

		@Override
		public void run() {
			if (textOperationTarget.canDoOperation(ISourceViewer.QUICK_ASSIST)) {
				textOperationTarget.doOperation(ISourceViewer.QUICK_ASSIST);
			}
		}

	}

	private IAction createContentAssistAction(
			final SourceViewer viewer) {
		Action proposalAction = new Action() {

			@Override
			public void run() {
				viewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
			}

		};
		proposalAction
				.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		return proposalAction;
	}

	/**
	 * Returns the commit message, converting platform-specific line endings to
	 * '\n' and hard-wrapping lines if necessary.
	 *
	 * @return commit message, without trailing whitespace on lines and without
	 *         trailing empty lines.
	 */
	public String getCommitMessage() {
		String text = getText();
		text = Utils.normalizeLineEndings(text);
		if (shouldHardWrap()) {
			text = wrapCommitMessage(text);
		}
		text = TRAILING_WHITE_SPACE_ON_LINES.matcher(text).replaceAll(""); //$NON-NLS-1$
		text = TRAILING_NEWLINES.matcher(text).replaceFirst("\n"); //$NON-NLS-1$
		return text;
	}

	/**
	 * Wraps a commit message, leaving the footer as defined by
	 * {@link CommonUtils#getFooterOffset(String)} unwrapped.
	 *
	 * @param text
	 *            of the whole commit message, including footer, using '\n' as
	 *            line delimiter
	 * @return the wrapped text
	 */
	protected static String wrapCommitMessage(String text) {
		// protected in order to be easily testable
		int footerStart = CommonUtils.getFooterOffset(text);
		if (footerStart < 0) {
			return hardWrap(text);
		} else {
			// Do not wrap footer lines.
			String footer = text.substring(footerStart);
			text = hardWrap(text.substring(0, footerStart));
			return text + footer;
		}
	}

	/**
	 * Hard-wraps the given text.
	 *
	 * @param text
	 *            the text to wrap, must use '\n' as line delimiter
	 * @return the wrapped text
	 */
	protected static String hardWrap(String text) {
		// protected for testing
		int[] wrapOffsets = calculateWrapOffsets(text, MAX_LINE_WIDTH);
		if (wrapOffsets != null) {
			StringBuilder builder = new StringBuilder(text.length() + wrapOffsets.length);
			int prev = 0;
			for (int cur : wrapOffsets) {
				builder.append(text.substring(prev, cur));
				for (int j = cur; j > prev && builder.charAt(builder.length() - 1) == ' '; j--)
					builder.deleteCharAt(builder.length() - 1);
				builder.append('\n');
				prev = cur;
			}
			builder.append(text.substring(prev));
			return builder.toString();
		}
		return text;
	}

	/**
	 * Get hyperlink targets
	 *
	 * @return map of targets
	 */
	protected Map<String, IAdaptable> getHyperlinkTargets() {
		return Collections.singletonMap(EditorsUI.DEFAULT_TEXT_EDITOR_ID,
				getDefaultTarget());
	}

	/**
	 * Create content assistant
	 *
	 * @param viewer
	 * @return content assistant
	 */
	protected IContentAssistant createContentAssistant(ISourceViewer viewer) {
		return null;
	}

	/**
	 * Get default target for hyperlink presenter
	 *
	 * @return target
	 */
	protected IAdaptable getDefaultTarget() {
		return null;
	}

	/**
	 * @return text
	 */
	public String getText() {
		return getDocument().get();
	}

	/**
	 * @return document
	 */
	public IDocument getDocument() {
		return sourceViewer.getDocument();
	}

	/**
	 * @param text
	 */
	public void setText(String text) {
		if (text != null) {
			getDocument().set(text);
		}
	}

	/**
	 * Set the same background color to the styledText widget as the Composite
	 */
	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
		StyledText textWidget = getTextWidget();
		textWidget.setBackground(color);
	}

	/**
	 *
	 */
	@Override
	public boolean forceFocus() {
		return getTextWidget().setFocus();
	}

	/**
	 * Calculates wrap offsets for the given line, so that resulting lines are
	 * no longer than <code>maxLineLength</code> if possible.
	 *
	 * @param line
	 *            the line to wrap (can contain '\n', but no other line delimiters)
	 * @param maxLineLength
	 *            the maximum line length
	 * @return an array of offsets where hard-wraps should be inserted, or
	 *         <code>null</code> if the line does not need to be wrapped
	 */
	public static int[] calculateWrapOffsets(final String line, final int maxLineLength) {
		if (line.length() == 0)
			return null;

		IntList wrapOffsets = new IntList();
		int wordStart = 0;
		int lineStart = 0;
		int length = line.length();
		int nofPreviousWordChars = 0;
		int nofCurrentWordChars = 0;
		for (int i = 0; i < length; i++) {
			char ch = line.charAt(i);
			if (ch == ' ') {
				nofPreviousWordChars += nofCurrentWordChars;
				nofCurrentWordChars = 0;
			} else if (ch == '\n') {
				lineStart = i + 1;
				wordStart = i + 1;
				nofPreviousWordChars = 0;
				nofCurrentWordChars = 0;
			} else { // a word character
				if (nofCurrentWordChars == 0) {
					// Break only if we had a certain number of previous
					// non-space characters. If we had less, break only if we
					// had at least one non-space character and the current line
					// offset is at least half the maximum width. This prevents
					// breaking if we have <blanks><very_long_word>, and also
					// for things like "[1] <very_long_word>" or mark-up lists
					// such as " * <very_long_word>".
					if (nofPreviousWordChars > maxLineLength / 10
							|| nofPreviousWordChars > 0
									&& (i - lineStart) > maxLineLength / 2) {
						wordStart = i;
					}
				}
				nofCurrentWordChars++;
				if (i >= lineStart + maxLineLength) {
					if (wordStart != lineStart) { // don't break before a single long word
						wrapOffsets.add(wordStart);
						lineStart = wordStart;
						nofPreviousWordChars = 0;
						nofCurrentWordChars = 0;
					}
				}
			}
		}

		int size = wrapOffsets.size();
		if (size == 0) {
			return null;
		} else {
			int[] result = new int[size];
			for (int i = 0; i < size; i++) {
				result[i] = wrapOffsets.get(i);
			}
			return result;
		}
	}
}
