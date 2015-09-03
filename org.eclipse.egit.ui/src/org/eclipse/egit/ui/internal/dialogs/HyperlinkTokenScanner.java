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

	/** The {@link IToken} returned for all non-hyperlink content. */
	protected static final IToken DEFAULT = new Token(null);

	private int endOfRange;

	private int currentOffset;

	private int tokenStart;

	private IToken hyperlinkToken;

	private final ISourceViewer viewer;

	private final IHyperlinkDetector[] hyperlinkDetectors;

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
		this.hyperlinkDetectors = hyperlinkDetectors;
		this.viewer = viewer;
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
		currentOffset++;
		return DEFAULT;
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
	 * should be scanned, plus defining the foreground color to use for hyperlink
	 * syntax coloring.
	 *
	 * @param document
	 *            the document to scan
	 * @param offset
	 *            the offset of the document range to scan
	 * @param length
	 *            the length of the document range to scan
	 * @param color
	 *            the foreground color to use for hyperlinks; may be
	 *            {@code null} in which case the SWT default color for the
	 *            {@link org.eclipse.swt.SWT#UNDERLINE_LINK SWT.UNDERLINE_LINK}
	 *            style is applied
	 */
	protected void setRangeAndColor(@NonNull IDocument document, int offset,
			int length, @Nullable Color color) {
		Assert.isTrue(document == viewer.getDocument());
		this.endOfRange = offset + length;
		this.currentOffset = offset;
		this.tokenStart = -1;
		hyperlinkToken = new Token(
				new HyperlinkDamagerRepairer.HyperlinkTextAttribute(color));
	}
}