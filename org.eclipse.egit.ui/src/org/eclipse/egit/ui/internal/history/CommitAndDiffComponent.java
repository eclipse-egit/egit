/*******************************************************************************
 * Copyright (C) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.dialogs.HyperlinkSourceViewer;
import org.eclipse.egit.ui.internal.dialogs.HyperlinkTokenScanner;
import org.eclipse.egit.ui.internal.dialogs.ShowWhitespaceAction;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * A scrollable component that provides combined scrolling over two vertically
 * arranged {@link StyledText}s, a {@link CommitMessageViewer} and a
 * {@link DiffViewer}.
 */
public class CommitAndDiffComponent {

	private ScrolledComposite commentAndDiffScrolledComposite;

	private Composite commentAndDiffComposite;

	/** Viewer displaying the currently selected commit. */
	private CommitMessageViewer commentViewer;

	/** Viewer displaying a unified diff of selected files from the commit. */
	private DiffViewer diffViewer;

	/** Remember caret positions; x = line, y = global offset. */
	private Point commentCaret = new Point(0, 0);

	private Point diffCaret = new Point(0, 0);

	/** True during {@link #resizeCommentAndDiffScrolledComposite()}. */
	private volatile boolean resizing;

	/**
	 * Creates a new {@link CommitAndDiffComponent}.
	 *
	 * @param parent
	 *            widget to use as parent
	 * @param site
	 *            of the workbench part
	 */
	public CommitAndDiffComponent(Composite parent, IWorkbenchPartSite site) {
		commentAndDiffScrolledComposite = new ScrolledComposite(parent,
				SWT.H_SCROLL | SWT.V_SCROLL);
		commentAndDiffScrolledComposite.setExpandHorizontal(true);
		commentAndDiffScrolledComposite.setExpandVertical(true);

		commentAndDiffComposite = new Composite(commentAndDiffScrolledComposite,
				SWT.NONE);
		commentAndDiffScrolledComposite.setContent(commentAndDiffComposite);
		commentAndDiffComposite
				.setLayout(GridLayoutFactory.fillDefaults().create());

		commentViewer = new CommitMessageViewer(commentAndDiffComposite, site);
		commentViewer.getControl().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());

		commentViewer.addTextInputListener(new ITextInputListener() {

			@Override
			public void inputDocumentChanged(IDocument oldInput,
					IDocument newInput) {
				commentCaret = new Point(0, 0);
			}

			@Override
			public void inputDocumentAboutToBeChanged(IDocument oldInput,
					IDocument newInput) {
				// Nothing
			}
		});
		commentViewer.addTextListener(
				event -> resizeCommentAndDiffScrolledComposite());

		StyledText commentWidget = commentViewer.getTextWidget();
		commentWidget.addVerifyKeyListener(event -> {
			// Get the current caret position *before* it moves (if it does)
			int offset = commentWidget.getCaretOffset();
			commentCaret = new Point(commentWidget.getLineAtOffset(offset),
					offset);
		});
		commentWidget.addCaretListener(event -> {
			Point location = commentWidget
					.getLocationAtOffset(event.caretOffset);
			scrollCommentAndDiff(location,
					commentWidget.getLineHeight(event.caretOffset));
		});

		commentAndDiffComposite
				.setBackground(commentViewer.getControl().getBackground());

		HyperlinkSourceViewer.Configuration configuration = new HyperlinkSourceViewer.Configuration(
				EditorsUI.getPreferenceStore()) {

			@Override
			public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
				return SWT.NONE;
			}

			@Override
			protected IHyperlinkDetector[] internalGetHyperlinkDetectors(
					ISourceViewer sourceViewer) {
				IHyperlinkDetector[] registered = super.internalGetHyperlinkDetectors(
						sourceViewer);
				// Always add our special detector for commit hyperlinks; we
				// want those to show always.
				if (registered == null) {
					return new IHyperlinkDetector[] {
							new CommitMessageViewer.KnownHyperlinksDetector() };
				} else {
					IHyperlinkDetector[] result = new IHyperlinkDetector[registered.length
							+ 1];
					System.arraycopy(registered, 0, result, 0,
							registered.length);
					result[registered.length] = new CommitMessageViewer.KnownHyperlinksDetector();
					return result;
				}
			}

			@Override
			public String[] getConfiguredContentTypes(
					ISourceViewer sourceViewer) {
				return new String[] { IDocument.DEFAULT_CONTENT_TYPE,
						CommitMessageViewer.HEADER_CONTENT_TYPE,
						CommitMessageViewer.FOOTER_CONTENT_TYPE };
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
				TextAttribute headerDefault = new TextAttribute(
						PlatformUI.getWorkbench().getDisplay()
								.getSystemColor(SWT.COLOR_DARK_GRAY));
				DefaultDamagerRepairer headerDamagerRepairer = new DefaultDamagerRepairer(
						new HyperlinkTokenScanner(this, viewer, headerDefault));
				reconciler.setDamager(headerDamagerRepairer,
						CommitMessageViewer.HEADER_CONTENT_TYPE);
				reconciler.setRepairer(headerDamagerRepairer,
						CommitMessageViewer.HEADER_CONTENT_TYPE);
				DefaultDamagerRepairer footerDamagerRepairer = new DefaultDamagerRepairer(
						new FooterTokenScanner(this, viewer));
				reconciler.setDamager(footerDamagerRepairer,
						CommitMessageViewer.FOOTER_CONTENT_TYPE);
				reconciler.setRepairer(footerDamagerRepairer,
						CommitMessageViewer.FOOTER_CONTENT_TYPE);
				return reconciler;
			}

		};

		commentViewer.configure(configuration);

		diffViewer = new DiffViewer(commentAndDiffComposite, null, SWT.NONE);
		diffViewer.configure(
				new DiffViewer.Configuration(EditorsUI.getPreferenceStore()));
		diffViewer.getControl().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).create());
		diffViewer.addTextInputListener(new ITextInputListener() {

			@Override
			public void inputDocumentChanged(IDocument oldInput,
					IDocument newInput) {
				diffCaret = new Point(0, 0);
			}

			@Override
			public void inputDocumentAboutToBeChanged(IDocument oldInput,
					IDocument newInput) {
				// Nothing
			}
		});
		diffViewer.addTextListener(
				event -> resizeCommentAndDiffScrolledComposite());

		ActionUtils.UpdateableAction selectAll = ActionUtils.createGlobalAction(
				ActionFactory.SELECT_ALL,
				() -> diffViewer.doOperation(ITextOperationTarget.SELECT_ALL),
				() -> diffViewer
						.canDoOperation(ITextOperationTarget.SELECT_ALL));
		ActionUtils.UpdateableAction copy = ActionUtils.createGlobalAction(
				ActionFactory.COPY,
				() -> diffViewer.doOperation(ITextOperationTarget.COPY),
				() -> diffViewer.canDoOperation(ITextOperationTarget.COPY));
		ActionUtils.setGlobalActions(diffViewer.getControl(), copy, selectAll);
		ShowWhitespaceAction showWhitespaceAction = new ShowWhitespaceAction(
				diffViewer);
		diffViewer.addSelectionChangedListener(e -> copy.update());
		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(manager -> {
			if (diffViewer.getDocument().getLength() > 0) {
				manager.add(copy);
				manager.add(selectAll);
				manager.add(new Separator());
				manager.add(showWhitespaceAction);
			}
		});
		StyledText diffWidget = diffViewer.getTextWidget();
		diffWidget.setMenu(contextMenu.createContextMenu(diffWidget));
		diffWidget.addDisposeListener(e -> showWhitespaceAction.dispose());
		diffWidget.addVerifyKeyListener(event -> {
			// Get the current caret position *before* it moves (if it does)
			int offset = diffWidget.getCaretOffset();
			diffCaret = new Point(diffWidget.getLineAtOffset(offset), offset);
		});
		diffWidget.addCaretListener(event -> {
			Point location = diffWidget.getLocationAtOffset(event.caretOffset);
			location.y += diffViewer.getControl().getLocation().y;
			scrollCommentAndDiff(location,
					diffWidget.getLineHeight(event.caretOffset));
		});

		commentAndDiffScrolledComposite
				.addControlListener(new ControlAdapter() {
					@Override
					public void controlResized(ControlEvent e) {
						if (!resizing && commentViewer.getTextWidget()
								.getWordWrap()) {
							resizeCommentAndDiffScrolledComposite();
						}
					}
				});

		// Continuous cursor navigation over the two viewers with the arrow keys
		commentWidget.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (diffWidget.getCharCount() == 0) {
					return;
				}
				if (e.keyCode == SWT.ARROW_DOWN) {
					int lastLine = commentWidget.getLineCount() - 1;
					if (commentCaret.x == lastLine
							&& commentWidget.getLineAtOffset(commentWidget
									.getCaretOffset()) == lastLine) {
						diffWidget.setFocus();
						diffWidget.setCaretOffset(0);
					}
				} else if (e.keyCode == SWT.ARROW_RIGHT) {
					int chars = commentWidget.getCharCount();
					if (commentCaret.y == chars
							&& commentWidget.getCaretOffset() == chars) {
						diffWidget.setFocus();
						diffWidget.setCaretOffset(0);
					}
				}
			}
		});
		diffWidget.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP) {
					if (diffCaret.x == 0 && diffWidget.getLineAtOffset(
							diffWidget.getCaretOffset()) == 0) {
						commentWidget.setFocus();
						commentWidget
								.setCaretOffset(commentWidget.getOffsetAtLine(
										commentWidget.getLineCount() - 1));
					}
				} else if (e.keyCode == SWT.ARROW_LEFT) {
					if (diffCaret.y == 0
							&& diffWidget.getCaretOffset() == 0) {
						commentWidget.setFocus();
						commentWidget
								.setCaretOffset(commentWidget.getCharCount());
					}
				}
			}
		});

	}

	/**
	 * Retrieves the outermost control (a {@link Composite}) wrapping the
	 * viewers.
	 *
	 * @return the outermost control
	 */
	public Control getControl() {
		return commentAndDiffScrolledComposite;
	}

	/**
	 * Retrieves the {@link DiffViewer}.
	 *
	 * @return the {@link DiffViewer}
	 */
	public DiffViewer getDiffViewer() {
		return diffViewer;
	}

	/**
	 * Retrieves the {@link CommitMessageViewer}.
	 *
	 * @return the {@link CommitMessageViewer}
	 */
	public CommitMessageViewer getCommitViewer() {
		return commentViewer;
	}

	/**
	 * Sets word-wrapping on the text viewers.
	 *
	 * @param wrap
	 *            whether to word-wrap
	 */
	public void setWrap(boolean wrap) {
		commentViewer.getTextWidget().setWordWrap(wrap);
		diffViewer.getTextWidget().setWordWrap(wrap);
		resizeCommentAndDiffScrolledComposite();
	}

	private void resizeCommentAndDiffScrolledComposite() {
		resizing = true;
		long start = 0;
		int lines = 0;
		boolean trace = GitTraceLocation.HISTORYVIEW.isActive();
		if (trace) {
			IDocument document = diffViewer.getDocument();
			lines = document != null ? document.getNumberOfLines() : 0;
			System.out.println("Lines: " + lines); //$NON-NLS-1$
			if (lines > 1) {
				new Exception("resizeCommentAndDiffScrolledComposite") //$NON-NLS-1$
						.printStackTrace(System.out);
			}
			start = System.currentTimeMillis();
		}

		Point size = commentAndDiffComposite.computeSize(SWT.DEFAULT,
				SWT.DEFAULT);
		commentAndDiffComposite.layout();
		commentAndDiffScrolledComposite.setMinSize(size);
		resizing = false;

		if (trace) {
			long stop = System.currentTimeMillis();
			long time = stop - start;
			long lps = (lines * 1000) / (time + 1);
			System.out
					.println("Resize + diff: " + time + " ms, line/s: " + lps); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void scrollCommentAndDiff(Point location, int lineHeight) {
		Rectangle size = commentAndDiffScrolledComposite.getBounds();
		ScrollBar bar = commentAndDiffScrolledComposite.getVerticalBar();
		if (bar != null && bar.isVisible()) {
			size.width = Math.max(0, size.width - bar.getSize().x);
		}
		bar = commentAndDiffScrolledComposite.getHorizontalBar();
		if (bar != null && bar.isVisible()) {
			size.height = Math.max(0, size.height - bar.getSize().y);
		}
		Point topLeft = commentAndDiffScrolledComposite.getOrigin();
		size.x = topLeft.x;
		size.y = topLeft.y;
		if (location.y >= size.y) {
			location.y += lineHeight;
		}
		if (!size.contains(location)) {
			int left = size.x;
			int top = size.y;
			// Use the same scrolling as in StyledText: scroll horizontally at
			// least by width / 4. Otherwise horizontal scrolling is slow.
			int minScroll = size.width / 4;
			if (location.x < left) {
				left = Math.max(0,
						left - Math.max(left - location.x, minScroll));
			} else if (location.x > left + size.width) {
				int maxWidth = commentAndDiffComposite.getSize().x;
				int right = Math.max(location.x, left + size.width + minScroll);
				left = Math.min(right, maxWidth) - size.width;
			}
			if (location.y < top) {
				top = location.y;
			} else if (location.y > top + size.height) {
				top = location.y - size.height;
			}
			commentAndDiffScrolledComposite.setOrigin(left, top);
		}
	}

	private static class FooterTokenScanner extends HyperlinkTokenScanner {

		private static final Pattern ITALIC_LINE = Pattern
				.compile("^[A-Z](?:[A-Za-z]+-)+by: "); //$NON-NLS-1$

		private final IToken italicToken;

		public FooterTokenScanner(SourceViewerConfiguration configuration,
				ISourceViewer viewer) {
			super(configuration, viewer);
			Object defaults = defaultToken.getData();
			TextAttribute italic;
			if (defaults instanceof TextAttribute) {
				TextAttribute defaultAttribute = (TextAttribute) defaults;
				int style = defaultAttribute.getStyle() ^ SWT.ITALIC;
				italic = new TextAttribute(defaultAttribute.getForeground(),
						defaultAttribute.getBackground(), style,
						defaultAttribute.getFont());
			} else {
				italic = new TextAttribute(null, null, SWT.ITALIC);
			}
			italicToken = new Token(italic);
		}

		@Override
		protected IToken scanToken() {
			// If we're at a "Signed-off-by" or similar footer line, make it
			// italic.
			try {
				IRegion region = document
						.getLineInformationOfOffset(currentOffset);
				if (currentOffset == region.getOffset()) {
					String line = document.get(currentOffset,
							region.getLength());
					Matcher m = ITALIC_LINE.matcher(line);
					if (m.find()) {
						currentOffset = Math.min(endOfRange,
								currentOffset + region.getLength());
						return italicToken;
					}
				}
			} catch (BadLocationException e) {
				// Ignore and return null below.
			}
			return null;
		}
	}
}
