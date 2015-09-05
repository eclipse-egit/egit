/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
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

	private IToken hyperlinkToken;

	private final ISourceViewer viewer;

	private final IHyperlinkDetector[] hyperlinkDetectors;

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
		this.hyperlinkDetectors = hyperlinkDetectors;
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
		this.tokenStart = currentOffset;
		if (currentOffset >= endOfRange) {
			return Token.EOF;
		}
		for (IHyperlinkDetector hyperlinkDetector : hyperlinkDetectors) {
			IHyperlink[] newLinks = hyperlinkDetector.detectHyperlinks(viewer,
					new Region(currentOffset, 0), false);
			if (newLinks != null && newLinks.length > 0) {
				IRegion region = newLinks[0].getHyperlinkRegion();
				int end = Math.min(endOfRange,
						region.getOffset() + region.getLength());
				if (end > tokenStart) {
					currentOffset = end;
					return hyperlinkToken;
				}
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
}