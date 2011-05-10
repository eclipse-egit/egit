/*******************************************************************************
 * Copyright (C) 2010, Robin Stocker
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea.WrapEdit;
import org.junit.Test;

public class SpellcheckableMessageAreaTest {

	@Test
	public void dontWrapShortText() {
		String input = "short message";
		assertWrappedEquals(input, input);
	}

	@Test
	public void dontWrapAlreadyWrappedText() {
		String input = "This is a test of wrapping\n\nDid it work?\n\nHm?";
		assertWrappedEquals(input, input);
	}

	@Test
	public void dontWrapMaximumLengthText() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789 12";
		assertWrappedEquals(input, input);
	}

	@Test
	public void wrapOverlengthText() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789 12 3";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789 123456789 12\n3";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void wrapOverlengthTextByOne() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789.abc";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789\n123456789.abc";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void wrapOverlengthText2() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789. 1234567890123456";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789.\n1234567890123456";
		assertWrappedEquals(expected, input);
	}

	public void wrapOverlengthTextTwice() {
		String input = "123456789 123456789 123456789 123456789 123456789 123456789 123456789.12 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.12 "
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.12";
		String expected = "123456789 123456789 123456789 123456789 123456789 123456789 123456789.12\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.12\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789.12";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void dontWrapWordLongerThanOneLineAtStart() {
		String input = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012 the previous was longer than a line";
		String expected = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012\nthe previous was longer than a line";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void dontWrapWordLongerThanOneLine() {
		String input = "This has to be on its own line: 1234567890123456789012345678901234567890123456789012345678901234567890123456789012 this not";
		String expected = "This has to be on its own line:\n1234567890123456789012345678901234567890123456789012345678901234567890123456789012\nthis not";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void wrapSecondLongLine() {
		String input = "First line\n123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789.12";
		String expected = "First line\n123456789 123456789 123456789 123456789 123456789 123456789 123456789\n123456789.12";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void keepExistingNewlines() {
		String input = "This\n\nis\nall\nok\n123456789 123456789 123456789 123456789 123456789 123456789 123456789.12";
		assertWrappedEquals(input, input);
	}

	@Test
	public void keepNewlineAtEnd() {
		String input = "Newline\nat\nend\n";
		assertWrappedEquals(input, input);
	}

	@Test
	public void keepWhitespace() {
		String input = "  this   is     deliberate whitespace";
		assertWrappedEquals(input, input);
	}

	@Test
	public void keepTrailingSpace() {
		String input = "space at end ";
		assertWrappedEquals(input, input);
	}

	private static void assertWrappedEquals(String expected, String input) {
		assertWrappedEqualsOnUnix(expected, input);
		assertWrappedEqualsOnWindows(expected, input);
	}

	private static void assertWrappedEqualsOnUnix(String expected, String input) {
		String wrapped = wrap(input, "\n");
		assertEquals(expected, wrapped);
	}

	private static void assertWrappedEqualsOnWindows(String expected,
			String input) {
		String wrapped = wrap(input.replaceAll("\n", "\r\n"), "\r\n");
		assertEquals(expected.replaceAll("\n", "\r\n"), wrapped);
	}

	private static String wrap(String text, String lineDelimiter) {
		StringBuilder sb = new StringBuilder(text);
		List<WrapEdit> wrapEdits = SpellcheckableMessageArea
				.calculateWrapEdits(text,
						SpellcheckableMessageArea.MAX_LINE_WIDTH, lineDelimiter);
		for (WrapEdit wrapEdit : wrapEdits) {
			sb.replace(wrapEdit.getStart(),
					wrapEdit.getStart() + wrapEdit.getLength(), lineDelimiter);
		}
		return sb.toString();
	}
}
