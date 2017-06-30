/*******************************************************************************
 * Copyright (C) 2010, Robin Stocker
 * Copyright (C) 2011, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, IBM Corporation (Markus Keller <markus_keller@ch.ibm.com>)
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import static org.junit.Assert.assertEquals;

import org.eclipse.egit.core.internal.Utils;
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
	public void dontWrapWordLongerThanOneLineAndKeepSpaceAtFront() {
		String input = " 1234567890123456789012345678901234567890123456789012345678901234567890123456789012";
		assertWrappedEquals(input, input);
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

	@Test
	public void lineAfterWrappedWordShouldNotBeJoined() {
		String input = "000000001 000000002 000000003 000000004 000000005 000000006 000000007 000000008\n000000009";
		String expected = "000000001 000000002 000000003 000000004 000000005 000000006 000000007\n000000008\n000000009";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void lineAfterWrappedWordShouldNotBeJoined2() {
		String input = "000000001 000000002 000000003 000000004 000000005 000000006 000000007 000000008\n"
				+ "000000009 000000010 000000011 000000012 000000013 000000014 000000015 000000016";
		String expected = "000000001 000000002 000000003 000000004 000000005 000000006 000000007\n"
				+ "000000008\n"
				+ "000000009 000000010 000000011 000000012 000000013 000000014 000000015\n"
				+ "000000016";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void lineAfterWrappedLineShouldBeJoinedAndFollowingLineWrappedCorrectly() {
		String input = "000000001 000000002 000000003 000000004 000000005 000000006 000000007 "
				+ "000000008 000000009 000000010 000000011 000000012 000000013 000000014\n"
				+ "000000015 000000016 000000017 000000018 000000019 000000020 000000021";
		String expected = "000000001 000000002 000000003 000000004 000000005 000000006 000000007\n"
				+ "000000008 000000009 000000010 000000011 000000012 000000013 000000014\n"
				+ "000000015 000000016 000000017 000000018 000000019 000000020 000000021";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void lineAfterWrappedWordShouldNotBeJoinedIfItsEmpty() {
		String input = "000000001 000000002 000000003 000000004 000000005 000000006 000000007 000000008\n\nNew paragraph";
		String expected = "000000001 000000002 000000003 000000004 000000005 000000006 000000007\n000000008\n\nNew paragraph";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void lineAfterWrappedWordShouldNotBeJoinedIfItStartsWithASymbol() {
		String input = "* 000000001 000000002 000000003 000000004 000000005 000000006 000000007 000000008\n* Bullet 2";
		String expected = "* 000000001 000000002 000000003 000000004 000000005 000000006 000000007\n000000008\n* Bullet 2";
		assertWrappedEquals(expected, input);
	}

	@Test
	public void lineAfterWrappedWordShouldNotBeJoined3() {
		String input = "* 000000001 000000002 000000003 000000004 000000005 000000006 000000007 000000008\n(paren)";
		String expected = "* 000000001 000000002 000000003 000000004 000000005 000000006 000000007\n000000008\n(paren)";
		assertWrappedEquals(expected, input);
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
		String wrapped = SpellcheckableMessageArea.hardWrap(Utils.normalizeLineEndings(text));
		return wrapped.replaceAll("\n", lineDelimiter);
	}

	@Test
	public void dontWrapShortMessage() {
		String input = "short";
		assertEquals(input, SpellcheckableMessageArea.wrapCommitMessage(input));
	}

	@Test
	public void dontWrapLongCommitMessageFooter() {
		String input = "short\n\nfoo\n\n"
				+ "Change-Id: I0000000000000000000000000000000000000000\n"
				+ "Signed-off-by: Some-Arguablylong Name <jsomearguablylong.name@somecompany.com>";
		assertEquals(input, SpellcheckableMessageArea.wrapCommitMessage(input));
	}

	@Test
	public void wrapOnlyCommitMessageBody() {
		String input = "short\n\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789\n\n"
				+ "Change-Id: I0000000000000000000000000000000000000000\n"
				+ "Signed-off-by: Some-Arguablylong Name <somearguablylong.name@somecompany.com>";
		String expected = "short\n\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 123456789\n"
				+ "123456789\n\n"
				+ "Change-Id: I0000000000000000000000000000000000000000\n"
				+ "Signed-off-by: Some-Arguablylong Name <somearguablylong.name@somecompany.com>";
		assertEquals(expected,
				SpellcheckableMessageArea.wrapCommitMessage(input));
	}

	@Test
	public void wrapAlternatingBlankNonBlank() {
		String input = " x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x";
		String expected = " x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x\n"
				+ "x x x x x x";
		assertEquals(expected,
				SpellcheckableMessageArea.wrapCommitMessage(input));
	}

	@Test
	public void testWrappingLongWords() {
		String input = "A commit message with long words\n\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789 A_single_rather_long_word_that_exceeds_the_margin\n"
				+ "More text with normal short words on a single line fitting the margin.\n"
				+ "Even more text with normal short words on a single line exceeding the margin.\n\n"
				+ "[1] https://foo.example.org/with/a/very/long/url/that/should/remain/on/this/line/\n"
				+ " * https://foo.example.org/with/a/very/long/url/that/should/also/remain/on/this/line/\n"
				+ " * ** https://foo.example.org/with/a/very/long/url/that/should/also/remain/on/this/line/\n"
				+ "                                                this_long_word_also_should_remain_here, but this should be a new line\n"
				+ "     *                                          but_this_long_word_should_be_wrapped, and this should follow\n\n"
				+ "Change-Id: I0000000000000000000000000000000000000000\n"
				+ "Signed-off-by: Some-Arguablylong Name <somearguablylong.name@somecompany.com>";
		String expected = "A commit message with long words\n\n"
				+ "123456789 123456789 123456789 123456789 123456789 123456789\n"
				+ "A_single_rather_long_word_that_exceeds_the_margin\n"
				+ "More text with normal short words on a single line fitting the margin.\n"
				+ "Even more text with normal short words on a single line exceeding the\n"
				+ "margin.\n\n"
				+ "[1] https://foo.example.org/with/a/very/long/url/that/should/remain/on/this/line/\n"
				+ " * https://foo.example.org/with/a/very/long/url/that/should/also/remain/on/this/line/\n"
				+ " * ** https://foo.example.org/with/a/very/long/url/that/should/also/remain/on/this/line/\n"
				+ "                                                this_long_word_also_should_remain_here,\n"
				+ "but this should be a new line\n"
				+ "     *\n" // Trailing blanks stripped!
				+ "but_this_long_word_should_be_wrapped, and this should follow\n\n"
				+ "Change-Id: I0000000000000000000000000000000000000000\n"
				+ "Signed-off-by: Some-Arguablylong Name <somearguablylong.name@somecompany.com>";
		assertEquals(expected,
				SpellcheckableMessageArea.wrapCommitMessage(input));
	}
}
