/*******************************************************************************
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HyperlinkTokenScannerTest {

	@Mock
	private ISourceViewer viewer;

	@Mock
	private SourceViewerConfiguration configuration;

	@Mock
	private IPreferenceStore preferenceStore;

	private IHyperlinkDetector[] detectors = new IHyperlinkDetector[] {
			new URLHyperlinkDetector() };

	private IHyperlinkDetector[] detectorsWithFailure = new IHyperlinkDetector[] {
			new URLHyperlinkDetector(), new CrashingHyperlinkDetector() };

	@Test
	public void tokenizeEmpty() {
		String testString = "";
		String expected = "";
		assertTokens(testString, 0, testString.length(), expected);
	}

	@Test
	public void tokenizeNoHyperlinks() {
		String testString = "hello world";
		String expected = "DDDDDDDDDDD";
		assertTokens(testString, 0, testString.length(), expected);
	}

	@Test
	public void tokenizeLeadingHyperlink() {
		String testString = "http://foo bar";
		String expected = "HHHHHHHHHHDDDD";
		assertTokens(testString, 0, testString.length(), expected);
	}

	@Test
	public void tokenizeEmbeddedHyperlink() {
		String testString = "Link: http://foo bar";
		String expected = "DDDDDDHHHHHHHHHHDDDD";
		assertTokens(testString, 0, testString.length(), expected);
	}

	@Test
	public void tokenizeMultipleHyperlinksSimple() {
		String testString = "Link: http://foo http://www.example.com bar";
		String expected = "DDDDDDHHHHHHHHHHDHHHHHHHHHHHHHHHHHHHHHHDDDD";
		assertTokens(testString, 0, testString.length(), expected);
	}

	@Test
	public void tokenizeMultipleHyperlinksMultiline() {
		String testString = "Link: http://foo\n\n* http://foo\n* ftp://somewhere\n\nTwo links above.";
		String expected = "DDDDDDHHHHHHHHHHDDDDHHHHHHHHHHDDDHHHHHHHHHHHHHHHDDDDDDDDDDDDDDDDDD";
		assertTokens(testString, 0, testString.length(), expected);
	}

	@Test
	public void tokenizeHyperlinksOutsideRegion() {
		String testString = "Link: http://foo\n\n* http://foo\n* ftp://somewhere\n\nTwo links above.";
		String expected = "DDDDD                                                             ";
		assertTokens(testString, 0, 5, expected);
		expected = "                                                  DDDDDDDDDDDD    ";
		assertTokens(testString, 50, 12, expected);
	}

	@Test
	public void tokenizeHyperlinksCutByRegion() {
		String testString = "Link: http://foo\n\n* http://foo\n* ftp://somewhere\n\nTwo links above.";
		String expected = "DDDDDDHHHHHHHH                                                    ";
		assertTokens(testString, 0, 14, expected);
		expected = "               HDDDDHHHHHHHHH                                     ";
		assertTokens(testString, 15, 14, expected);
	}

	@Test
	public void tokenizeWithFailingDetector() {
		String testString = "Link: http://foo bar";
		String expected = "DDDDDDHHHHHHHHHHDDDD";
		assertTokens(testString, 0, testString.length(), detectorsWithFailure,
				expected);
		// With only a failing detector
		expected = "DDDDDDDDDDDDDDDDDDDD";
		assertTokens(testString, 0, testString.length(),
				new IHyperlinkDetector[] { new CrashingHyperlinkDetector() },
				expected);
	}

	@Test
	public void tokenizeWithoutDetectors() {
		String testString = "Link: http://foo bar";
		String expected = "DDDDDDDDDDDDDDDDDDDD";
		assertTokens(testString, 0, testString.length(),
				new IHyperlinkDetector[] {}, expected);
	}

	private void assertTokens(String text, int offset, int length,
			String expected) {
		assertTokens(text, offset, length, detectors, expected);
	}

	@SuppressWarnings("boxing")
	private void assertTokens(String text, int offset, int length,
			IHyperlinkDetector[] hyperlinkDetectors, String expected) {
		assertEquals("Test definition problem: 'expected' length mismatch",
				text.length(), expected.length());
		IDocument testDocument = new Document(text);
		when(viewer.getDocument()).thenReturn(testDocument);
		when(configuration.getHyperlinkDetectors(viewer))
				.thenReturn(hyperlinkDetectors);
		when(preferenceStore
				.getBoolean(AbstractTextEditor.PREFERENCE_HYPERLINKS_ENABLED))
						.thenReturn(true);
		when(preferenceStore.getBoolean(
				"org.eclipse.ui.internal.editors.text.URLHyperlinkDetector"))
						.thenReturn(hyperlinkDetectors.length == 0
								|| (hyperlinkDetectors[0] instanceof CrashingHyperlinkDetector));
		HyperlinkTokenScanner scanner = new HyperlinkTokenScanner(configuration,
				viewer, preferenceStore, null);
		scanner.setRangeAndColor(testDocument, offset, length, null);
		IToken token = null;
		char[] found = new char[text.length()];
		Arrays.fill(found, ' ');
		while (!(token = scanner.nextToken()).isEOF()) {
			int tokenOffset = scanner.getTokenOffset();
			int tokenLength = scanner.getTokenLength();
			char ch = 'x';
			Object data = token.getData();
			if (data == null) {
				ch = 'D';
			} else if (data instanceof TextAttribute) {
				int style = ((TextAttribute) data).getStyle();
				if ((style & TextAttribute.UNDERLINE) != 0) {
					ch = 'H';
				}
			}
			Arrays.fill(found, tokenOffset, tokenOffset + tokenLength, ch);
		}
		assertEquals("Unexpected tokens", expected, new String(found));
	}

	private static class CrashingHyperlinkDetector
			extends AbstractHyperlinkDetector {

		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			throw new IllegalStateException(
					"CrashingHyperlinkDetector fails on purpose");
		}

	}
}
