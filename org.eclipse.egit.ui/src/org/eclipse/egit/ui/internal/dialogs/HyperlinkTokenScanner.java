/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;

/**
 * A simple {@link ITokenScanner} that recognizes hyperlinks using
 * {@link IHyperlinkDetector}s.
 */
public class HyperlinkTokenScanner implements ITokenScanner {

	private int tokenStart;

	private int lastLineStart;

	private IToken hyperlinkToken;

	private final ISourceViewer viewer;

	private final IHyperlinkDetector[] hyperlinkDetectors;

	/**
	 * Caches all hyperlinks on a line to avoid calling the hyperlink detectors
	 * too often.
	 */
	private final List<IHyperlink> hyperlinksOnLine = new ArrayList<>();

	/** The current offset in the document. */
	protected int currentOffset;

	/** The end of the range to tokenize. */
	protected int endOfRange;

	/** The {@link IDocument} the current scan operates on. */
	protected IDocument document;

	/** The {@link IToken} to use for default content. */
	protected final IToken defaultToken;

	/**
	 * Creates a new instance that uses the given hyperlink detector and viewer.
	 *
	 * @param hyperlinkDetectors
	 *            the {@link IHyperlinkDetector}s to use
	 * @param viewer
	 *            the {@link ISourceViewer} to operate in
	 */
	public HyperlinkTokenScanner(IHyperlinkDetector[] hyperlinkDetectors,
			ISourceViewer viewer) {
		this(hyperlinkDetectors, viewer, null);
	}

	/**
	 * Creates a new instance that uses the given hyperlink detector and viewer.
	 *
	 * @param hyperlinkDetectors
	 *            the {@link IHyperlinkDetector}s to use
	 * @param viewer
	 *            the {@link ISourceViewer} to operate in
	 * @param defaultAttribute
	 *            the {@link TextAttribute} to use for the default token; may be
	 *            {@code null} to use the default style of the viewer
	 */
	public HyperlinkTokenScanner(IHyperlinkDetector[] hyperlinkDetectors,
			ISourceViewer viewer, @Nullable TextAttribute defaultAttribute) {
		IHyperlinkDetector[] allDetectors;
		if (hyperlinkDetectors == null || hyperlinkDetectors.length == 0) {
			allDetectors = new IHyperlinkDetector[0];
		} else {
			allDetectors = new IHyperlinkDetector[hyperlinkDetectors.length
					+ 1];
			System.arraycopy(hyperlinkDetectors, 0, allDetectors, 0,
					hyperlinkDetectors.length);
			// URLHyperlinkDetector can only detect hyperlinks at the start of
			// the range. We need one that can detect all hyperlinks in a given
			// region.
			allDetectors[hyperlinkDetectors.length] = new MultiURLHyperlinkDetector();
		}
		this.hyperlinkDetectors = allDetectors;
		this.viewer = viewer;
		this.defaultToken = new Token(defaultAttribute);
	}

	@Override
	public void setRange(IDocument document, int offset, int length) {
		setRangeAndColor(document, offset, length, JFaceColors
				.getHyperlinkText(viewer.getTextWidget().getDisplay()));
	}

	@Override
	public IToken nextToken() {
		tokenStart = currentOffset;
		if (currentOffset >= endOfRange) {
			hyperlinksOnLine.clear();
			return Token.EOF;
		}
		if (hyperlinkDetectors.length > 0) {
			try {
				IRegion currentLine = document
						.getLineInformationOfOffset(currentOffset);
				if (currentLine.getOffset() != lastLineStart) {
					// Compute all hyperlinks in the line
					hyperlinksOnLine.clear();
					for (IHyperlinkDetector hyperlinkDetector : hyperlinkDetectors) {
						IHyperlink[] newLinks = hyperlinkDetector
								.detectHyperlinks(viewer, currentLine, false);
						if (newLinks != null && newLinks.length > 0) {
							Collections.addAll(hyperlinksOnLine, newLinks);
						}
					}
					// Sort them by offset, and with increasing length
					Collections.sort(hyperlinksOnLine,
							new Comparator<IHyperlink>() {
								@Override
								public int compare(IHyperlink a, IHyperlink b) {
									int diff = a.getHyperlinkRegion()
											.getOffset()
											- b.getHyperlinkRegion()
													.getOffset();
									if (diff != 0) {
										return diff;
									}
									return a.getHyperlinkRegion().getLength()
											- b.getHyperlinkRegion()
													.getLength();
								}
							});
					lastLineStart = currentLine.getOffset();
				}
				if (!hyperlinksOnLine.isEmpty()) {
					// Find first hyperlink for the position. We may have to
					// skip a few in case there are several hyperlinks at the
					// same position and with the same length.
					Iterator<IHyperlink> iterator = hyperlinksOnLine.iterator();
					while (iterator.hasNext()) {
						IHyperlink next = iterator.next();
						IRegion linkRegion = next.getHyperlinkRegion();
						int linkEnd = linkRegion.getOffset()
								+ linkRegion.getLength();
						if (currentOffset >= linkEnd) {
							iterator.remove();
						} else if (linkRegion.getOffset() <= currentOffset) {
							// This is our link
							iterator.remove();
							int end = Math.min(endOfRange, linkEnd);
							if (end > currentOffset) {
								currentOffset = end;
								return hyperlinkToken;
							}
						} else {
							// Next hyperlink is beyond current position
							break;
						}
					}
				}
			} catch (BadLocationException e) {
				// Ignore and keep going
			}
		}
		int actualOffset = currentOffset;
		IToken token = scanToken();
		if (token != null && actualOffset < currentOffset) {
			return token;
		}
		currentOffset = actualOffset + 1;
		return defaultToken;
	}

	@Override
	public int getTokenOffset() {
		return tokenStart;
	}

	@Override
	public int getTokenLength() {
		return currentOffset - tokenStart;
	}

	/**
	 * Configures the scanner by providing access to the document range that
	 * should be scanned, plus defining the foreground color to use for
	 * hyperlink syntax coloring.
	 *
	 * @param document
	 *            the document to scan
	 * @param offset
	 *            the offset of the document range to scan
	 * @param length
	 *            the length of the document range to scan
	 * @param color
	 *            the foreground color to use for hyperlinks; may be
	 *            {@code null} in which case the default color is applied
	 */
	protected void setRangeAndColor(@NonNull IDocument document, int offset,
			int length, @Nullable Color color) {
		Assert.isTrue(document == viewer.getDocument());
		this.document = document;
		this.lastLineStart = -1;
		this.endOfRange = offset + length;
		this.currentOffset = offset;
		this.tokenStart = -1;
		this.hyperlinkToken = new Token(
				new TextAttribute(color, null, TextAttribute.UNDERLINE));
	}

	/**
	 * Invoked if there is no hyperlink at the current position; may check for
	 * additional tokens. If a token is found, must advance currentOffset and
	 * return the token.
	 *
	 * @return the {@link IToken}, or {@code null} if none.
	 */
	protected IToken scanToken() {
		return null;
	}

	/**
	 * A {@link URLHyperlinkDetector} that returns all hyperlinks in a region.
	 * <p>
	 * This internal class assumes that the region is either empty or else spans
	 * a whole line.
	 * </p>
	 */
	private static class MultiURLHyperlinkDetector
			extends URLHyperlinkDetector {

		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			if (region.getLength() == 0) {
				return super.detectHyperlinks(textViewer, region,
						canShowMultipleHyperlinks);
			}
			// URLHyperlinkDetector only finds hyperlinks at the region start.
			// We know here that the given region spans a whole line since we're
			// only called from HyperlinkTokenScanner.nextToken().
			try {
				String line = textViewer.getDocument().get(region.getOffset(),
						region.getLength());
				int currentOffset = region.getOffset();
				int lineStart = currentOffset;
				int regionEnd = currentOffset + region.getLength();
				List<IHyperlink> allLinks = new ArrayList<>();
				while (currentOffset < regionEnd) {
					IHyperlink[] newLinks = super.detectHyperlinks(
							textViewer, new Region(currentOffset, 0),
							canShowMultipleHyperlinks);
					currentOffset++;
					if (newLinks != null && newLinks.length > 0) {
						Collections.addAll(allLinks, newLinks);
						for (IHyperlink link : newLinks) {
							int end = link.getHyperlinkRegion().getOffset()
									+ link.getHyperlinkRegion().getLength();
							if (end > currentOffset) {
								currentOffset = end;
							}
						}
					}
					// Advance to the next "://" combination.
					int nextCandidatePos = lineStart
							+ line.indexOf("://", currentOffset - lineStart); //$NON-NLS-1$
					if (nextCandidatePos > currentOffset) {
						currentOffset = nextCandidatePos;
					} else if (nextCandidatePos < currentOffset) {
						// No more links.
						break;
					}
				}
				return allLinks.toArray(new IHyperlink[allLinks.size()]);
			} catch (BadLocationException e) {
				return null;
			}
		}

	}
}