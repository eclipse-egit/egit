/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter.DiffStyleRange.Type;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
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
			 * Other line
			 */
			OTHER,

		}

		@Override
		public boolean equals(Object object) {
			return super.equals(object);
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
				write(new String(b, off, len));
			else
				write(new String(b, off, len, charset));
		}

		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		public void write(int b) throws IOException {
			write(new byte[] { (byte) b });
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
		this.stream.charset = CompareUtils.getResourceEncoding(repository,
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
	 * @return added range
	 */
	protected DiffStyleRange addRange(Type type) {
		DiffStyleRange range = new DiffStyleRange();
		range.start = stream.offset;
		range.diffType = type;
		ranges.add(range);
		return range;
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#writeHunkHeader(int, int, int,
	 *      int)
	 */
	protected void writeHunkHeader(int aStartLine, int aEndLine,
			int bStartLine, int bEndLine) throws IOException {
		DiffStyleRange range = addRange(Type.HUNK);
		super.writeHunkHeader(aStartLine, aEndLine, bStartLine, bEndLine);
		range.length = stream.offset - range.start;
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#writeLine(char,
	 *      org.eclipse.jgit.diff.RawText, int)
	 */
	protected void writeLine(char prefix, RawText text, int cur)
			throws IOException {
		if (prefix == ' ')
			super.writeLine(prefix, text, cur);
		else {
			DiffStyleRange range = addRange(prefix == '+' ? Type.ADD
					: Type.REMOVE);
			super.writeLine(prefix, text, cur);
			range.length = stream.offset - range.start;
		}
	}
}
