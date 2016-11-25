/*******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc. and others.
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
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter.DiffStyleRange.Type;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
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
			 * Header (after HEADLINE)
			 */
			HEADER,

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

		@Override
		public boolean similarTo(StyleRange style) {
			return super.similarTo(style) && style instanceof DiffStyleRange
					&& diffType == ((DiffStyleRange) style).diffType;
		}
	}

	/**
	 * Range giving access to the {@link FileDiff} and its {@link Repository}
	 * that generated the content.
	 */
	public static class FileDiffRange {
		private final int startOffset;

		private final int endOffset;

		private final FileDiff diff;

		private final Repository repository;

		/**
		 * Creates a new {@link FileDiffRange}
		 *
		 * @param repository
		 *            the {@link FileDiff} belongs to
		 * @param fileDiff
		 *            the range belongs to
		 * @param start
		 *            of the range
		 * @param end
		 *            of the range
		 */
		public FileDiffRange(Repository repository, FileDiff fileDiff,
				int start, int end) {
			this.startOffset = start;
			this.endOffset = end;
			this.diff = fileDiff;
			this.repository = repository;
		}

		/**
		 * Retrieves the start offset.
		 *
		 * @return the offset
		 */
		public int getStartOffset() {
			return startOffset;
		}

		/**
		 * Retrieves the end offset.
		 *
		 * @return the offset
		 */
		public int getEndOffset() {
			return endOffset;
		}

		/**
		 * Retrieves the {@link FileDiff}.
		 *
		 * @return the {@link FileDiff}
		 */
		public FileDiff getDiff() {
			return diff;
		}

		/**
		 * Retrieves the {@link Repository}.
		 *
		 * @return the {@link Repository}
		 */
		public Repository getRepository() {
			return repository;
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

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (charset == null)
				lineBuffer.append(new String(b, off, len, "UTF-8")); //$NON-NLS-1$
			else
				lineBuffer.append(new String(b, off, len, charset));
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
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

	private List<DiffStyleRange> ranges = new ArrayList<>();

	private List<FileDiffRange> fileRanges = new ArrayList<>();

	private final int maxLines;

	private int linesWritten;

	/**
	 * @param document
	 * @param offset
	 */
	public DiffStyleRangeFormatter(IDocument document, int offset) {
		this(document, offset, -1);
	}

	/**
	 * @param document
	 */
	public DiffStyleRangeFormatter(IDocument document) {
		this(document, document.getLength(), -1);
	}

	/**
	 * @param document
	 * @param offset
	 * @param maxLines
	 */
	public DiffStyleRangeFormatter(IDocument document, int offset,
			int maxLines) {
		super(new DocumentOutputStream(document, offset));
		this.stream = (DocumentOutputStream) getOutputStream();
		this.maxLines = maxLines;
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
		int start = stream.offset;
		diff.outputDiff(null, repository, this, true);
		flush();
		fileRanges
				.add(new FileDiffRange(repository, diff, start, stream.offset));
		return this;
	}

	/**
	 * Get diff style ranges, sorted by offset
	 *
	 * @return non-null but possibly empty array
	 */
	public DiffStyleRange[] getRanges() {
		return this.ranges.toArray(new DiffStyleRange[this.ranges.size()]);
	}

	/**
	 * Gets the file diff ranges, sorted by offset.
	 *
	 * @return the ranges
	 */
	public FileDiffRange[] getFileRanges() {
		return this.fileRanges
				.toArray(new FileDiffRange[this.fileRanges.size()]);
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
	@Override
	protected void writeHunkHeader(int aStartLine, int aEndLine,
			int bStartLine, int bEndLine) throws IOException {
		int start = stream.offset;
		if (!ranges.isEmpty()) {
			DiffStyleRange last = ranges.get(ranges.size() - 1);
			int lastEnd = last.start + last.length;
			if (last.diffType == Type.HEADLINE && lastEnd < start) {
				addRange(Type.HEADER, lastEnd, start);
			}
		}
		super.writeHunkHeader(aStartLine, aEndLine, bStartLine, bEndLine);
		stream.flushLine();
		addRange(Type.HUNK, start, stream.offset);
	}

	/**
	 * @see org.eclipse.jgit.diff.DiffFormatter#writeLine(char,
	 *      org.eclipse.jgit.diff.RawText, int)
	 */
	@Override
	protected void writeLine(char prefix, RawText text, int cur)
			throws IOException {
		if (maxLines > 0 && linesWritten > maxLines) {
			if (linesWritten == maxLines + 1) {
				int start = stream.offset;
				stream.flushLine();
				stream.write(
						NLS.bind(UIText.DiffStyleRangeFormatter_diffTruncated,
								Integer.valueOf(maxLines)));
				stream.write("\n"); //$NON-NLS-1$
				addRange(Type.HEADLINE, start, stream.offset);
				linesWritten++;
			}
			return;
		}
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
		linesWritten++;
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
