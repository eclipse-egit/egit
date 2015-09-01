/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * A {@link DefaultDamagerRepairer} that can handle
 * {@link HyperlinkTextAttribute}s.
 *
 * @see HyperlinkTokenScanner
 */
public class HyperlinkDamagerRepairer extends DefaultDamagerRepairer {

	/**
	 * A {@link TextAttribute} that the {@link HyperlinkDamagerRepairer} uses to
	 * set the {@link SWT#UNDERLINE_LINK} style property on a {@link StyleRange}
	 * of a text.
	 */
	protected static class HyperlinkTextAttribute extends TextAttribute {

		// Add SWT.UNDERLINE_LINK to avoid it compares equal to other
		// (non-hyperlink) TextAttributes.

		/**
		 * Creates a text attribute for the given foreground color, no
		 * background color and with the SWT normal style and SWT hyperlink
		 * underlining.
		 *
		 * @param foreground
		 *            the foreground color, <code>null</code> if none
		 */
		public HyperlinkTextAttribute(Color foreground) {
			this(foreground, null, SWT.UNDERLINE_LINK);
		}

		/**
		 * Creates a text attribute with the given colors and style, plus SWT
		 * hyperlink underlining.
		 *
		 * @param foreground
		 *            the foreground color, <code>null</code> if none
		 * @param background
		 *            the background color, <code>null</code> if none
		 * @param style
		 *            the style
		 */
		public HyperlinkTextAttribute(Color foreground, Color background,
				int style) {
			this(foreground, background, style, null);
		}

		/**
		 * Creates a text attribute with the given colors, style, and font, plus
		 * SWT hyperlink underlining.
		 *
		 * @param foreground
		 *            the foreground color, <code>null</code> if none
		 * @param background
		 *            the background color, <code>null</code> if none
		 * @param style
		 *            the style
		 * @param font
		 *            the font, <code>null</code> if none
		 */
		public HyperlinkTextAttribute(Color foreground, Color background,
				int style, Font font) {
			super(foreground, background, style | SWT.UNDERLINE_LINK, font);
		}
	}

	/**
	 * Creates a new instance that will use the given scanner to tokenize.
	 *
	 * @param scanner
	 *            the {@link ITokenScanner} to use for tokenizing and
	 *            determining the text attributes
	 */
	public HyperlinkDamagerRepairer(ITokenScanner scanner) {
		super(scanner);
	}

	@Override
	protected void addRange(TextPresentation presentation, int offset,
			int length, TextAttribute attr) {
		if (attr != null) {
			int style = attr.getStyle();
			int fontStyle = style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL);
			StyleRange styleRange = new StyleRange(offset, length,
					attr.getForeground(), attr.getBackground(), fontStyle);
			styleRange.strikeout = (style
					& TextAttribute.STRIKETHROUGH) != 0;
			if (attr instanceof HyperlinkTextAttribute) {
				styleRange.underline = true;
				styleRange.underlineStyle = SWT.UNDERLINE_LINK;
			} else {
				styleRange.underline = (style
						& TextAttribute.UNDERLINE) != 0;
			}
			styleRange.font = attr.getFont();
			presentation.addStyleRange(styleRange);
		}
	}
}