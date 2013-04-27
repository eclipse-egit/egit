/*******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Tobias Pfeifer (SAP AG) - customizable font and color for the first header line - https://bugs.eclipse.org/397723
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter.DiffStyleRange.Type;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

/**
 * Diff style range formatter class that builds up a list of
 * {@link DiffStyleRange} instances as each {@link FileDiff} is being written to
 * an {@link IDocument}.
 */
public class DiffStyleRangeFormatter extends DiffFormatter {

	/**
	 * Diff style range
	 */
	public static class DiffStyleRange extends StyleRange {

		/**
		 * Diff type
		 */
		public enum Type {

			/**
			 * Added line
			 */
			ADD,

			/**
			 * Removed line
			 */
			REMOVE,

			/**
			 * Hunk line
			 */
			HUNK,

			/**
			 * Headline
			 */
			HEADLINE,

			/**
			 * Other line
			 */
			OTHER,

		}

		@Override
		public boolean equals(Object object) {
			return super.equals(object);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		/**
		 * Diff type
		 */
		public Type diffType = Type.OTHER;

		/**
		 * Line background
		 */
		public Color lineBackground = null;

		public boolean similarTo(StyleRange style) {
			return super.similarTo(style) && style instanceof DiffStyleRange
					&& diffType == ((DiffStyleRange) style).diffType;
		}
	}

	private static class DocumentOutputStream extends OutputStream {

		private String charset;

		private IDocument document;

		private int offset;

		private StringBuilder lineBuffer = new StringBuilder();

		public DocumentOutputStream(IDocument document, int offset) {
			this.document = document;
			this.offset = offset;
		}

		private void write(String content) throws IOException {
			try {
				this.document.replace(this.offset, 0, content);
				this.offset += content.length();
			} catch (BadLocationException e) {
				throw new IOException(e.getMessage());
			}
		}

		public void write(byte[] b, int off, int len) throws IOException {
			if (charset == null)
				lineBuffer.append(new String(b, off, len));
			else
				lineBuffer.append(new String(b, off, len, charset));
		}

		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		public void write(int b) throws IOException {
			write(new byte[] { (byte) b });
		}

		@Override
		public void flush() throws IOException {
			flushLine();
		}

		protected void flushLine() throws IOException {
			if (lineBuffer.length() > 0) {
				write(lineBuffer.toString());
				lineBuffer.setLength(0);
			}
		}
	}

	private DocumentOutputStream stream;

	private List<DiffStyleRange> ranges = new ArrayList<DiffStyleRange>();

	/**
	 * @param document
	 * @param offset
	 */
	public DiffStyleRangeFormatter(IDocument document, int offset) {
		super(new DocumentOutputStream(document, offset));
		this.stream = (DocumentOutputStream) getOutputStream();
	}

	/**
	 * @param document
	 */
	public DiffStyleRangeFormatter(IDocument document) {
		this(document, document.getLength());
	}

	/**
	 * Write diff
	 *
	 * @param repository
	 * @param diff
	 * @return this formatter
	 * @throws IOException
	 */
	public DiffStyleRangeFormatter write(Repository repository, FileDiff diff)
			throws IOException {
		this.stream.charset = CompareCoreUtils.getResourceEncoding(repository,
				diff.getPath());
		diff.outputDiff(null, repository, this, true);
		flush();
		return this;
	}

	/**
	 * Get diff style ranges
	 *
	 * @return non-null but possibly empty array
	 */
	public DiffStyleRange[] getRanges() {
		return this.ranges.toArray(new DiffStyleRange[this.ranges.size()]);
	}

	/**
	 * Create and add diff style range
	 *
	 * @param type
	 *            the {@link Type}
	 * @param start
	 *            start offset
	 * @param end
	 *            end offset
	 * @return added range
	 */
	protected DiffStyleRange addRange(Type type, int start, int end) {
		DiffStyleRange range = new DiffStyleRange();
		range.start = start;
		range.diffType = type;
		range.length = end - start;
		ranges.add(range);
		return range;
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#writeHunkHeader(int, int, int,
	 *      int)
	 */
	protected void writeHunkHeader(int aStartLine, int aEndLine,
			int bStartLine, int bEndLine) throws IOException {
		int start = stream.offset;
		super.writeHunkHeader(aStartLine, aEndLine, bStartLine, bEndLine);
		stream.flushLine();
		addRange(Type.HUNK, start, stream.offset);
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#writeLine(char,
	 *      org.eclipse.jgit.diff.RawText, int)
	 */
	protected void writeLine(char prefix, RawText text, int cur)
			throws IOException {
		if (prefix == ' ') {
			super.writeLine(prefix, text, cur);
			stream.flushLine();
		} else {
			Type type = prefix == '+' ? Type.ADD : Type.REMOVE;
			int start = stream.offset;
			super.writeLine(prefix, text, cur);
			stream.flushLine();
			addRange(type, start, stream.offset);
		}
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#formatGitDiffFirstHeaderLine(ByteArrayOutputStream
	 *      o, ChangeType type, String oldPath, String newPath)
	 */
	@Override
	protected void formatGitDiffFirstHeaderLine(ByteArrayOutputStream o,
			final ChangeType type, final String oldPath, final String newPath)
			throws IOException {
		stream.flushLine();
		int offset = stream.offset;
		int start = o.size();
		super.formatGitDiffFirstHeaderLine(o, type, oldPath, newPath);
		int end = o.size();
		addRange(Type.HEADLINE, offset + start, offset + end);
	}

	@Override
	public void format(final EditList edits, final RawText a, final RawText b)
			throws IOException {
		// Flush header before formatting of edits begin
		stream.flushLine();
		super.format(edits, a, b);
	}
}
