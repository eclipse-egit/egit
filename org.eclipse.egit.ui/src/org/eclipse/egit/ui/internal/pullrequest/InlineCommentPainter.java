/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.pullrequest;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

/**
 * Paints inline pull request comment bubbles directly on a StyledText widget
 * using {@link StyledText#setLineVerticalIndent(int, int)} to reserve space
 * and a {@link PaintListener} to draw the comment bubbles.
 *
 * <p>
 * This approach bypasses Eclipse's annotation framework and directly
 * manipulates the StyledText widget, making it compatible with TextMergeViewer
 * which manages its own document lifecycle.
 * </p>
 */
public class InlineCommentPainter implements PaintListener {

	private static final int PADDING_X = 8;

	private static final int PADDING_Y = 4;

	private static final int ARC = 8;

	private static final int MAX_TEXT_LENGTH = 120;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm"); //$NON-NLS-1$

	private static final RGB COMMENT_BG_RGB = new RGB(219, 234, 254);

	private static final RGB RESOLVED_BG_RGB = new RGB(220, 237, 222);

	private static final RGB AUTHOR_RGB = new RGB(30, 64, 120);

	private static final RGB TEXT_RGB = new RGB(50, 50, 50);

	private static final RGB TIMESTAMP_RGB = new RGB(120, 120, 120);

	private static final RGB BORDER_RGB = new RGB(180, 200, 230);

	private final StyledText styledText;

	private final IDocument document;

	/**
	 * Map from 1-based line numbers to comments. Multiple comments per line are
	 * not yet supported - only the first comment per line is displayed.
	 */
	private final Map<Integer, PullRequestComment> commentsMap = new HashMap<>();

	private Color bgColor;

	private Color resolvedBgColor;

	private Color authorColor;

	private Color textColor;

	private Color timestampColor;

	private Color borderColor;

	/**
	 * Creates a new inline comment painter.
	 *
	 * @param styledText
	 *            the StyledText widget to paint on
	 * @param document
	 *            the document (for line offset calculations)
	 * @param comments
	 *            the list of comments to display
	 */
	public InlineCommentPainter(StyledText styledText, IDocument document,
			List<PullRequestComment> comments) {
		this.styledText = styledText;
		this.document = document;

		// Build comments map
		for (PullRequestComment comment : comments) {
			if (comment.isInlineComment() && comment.getLine() != null) {
				Integer lineNumber = comment.getLine();
				// For now, only keep the first comment per line
				// TODO: support multiple comments per line
				if (!commentsMap.containsKey(lineNumber)) {
					commentsMap.put(lineNumber, comment);
				}
			}
		}
	}

	/**
	 * Installs this painter on the StyledText widget. This sets vertical
	 * indents for commented lines and adds the paint listener.
	 */
	public void install() {
		if (styledText == null || styledText.isDisposed()) {
			return;
		}

		ensureColors();

		// Set vertical indent for each commented line
		for (Map.Entry<Integer, PullRequestComment> entry : commentsMap
				.entrySet()) {
			int oneBasedLine = entry.getKey().intValue();
			PullRequestComment comment = entry.getValue();

			try {
				// Convert 1-based line number to 0-based document line index
				int zeroBasedLine = oneBasedLine - 1;

				if (zeroBasedLine >= 0
						&& zeroBasedLine < document.getNumberOfLines()) {
					// Calculate height needed for this comment bubble
					int height = calculateCommentHeight(comment);

					// Set vertical indent on the StyledText widget
					// This creates blank space ABOVE the line
					styledText.setLineVerticalIndent(zeroBasedLine, height);
				}
			} catch (Exception e) {
				org.eclipse.egit.ui.Activator.logError(
						"Failed to set vertical indent for comment at line " //$NON-NLS-1$
								+ oneBasedLine,
						e);
			}
		}

		// Add paint listener to draw comment bubbles
		styledText.addPaintListener(this);

		// Force initial redraw
		styledText.redraw();
		
		// Also try forcing a full redraw after a short delay
		styledText.getDisplay().asyncExec(() -> {
			if (!styledText.isDisposed()) {
				styledText.redraw();
			}
		});
	}

	/**
	 * Uninstalls this painter from the StyledText widget. This removes
	 * vertical indents and the paint listener.
	 */
	public void uninstall() {
		if (styledText == null || styledText.isDisposed()) {
			return;
		}

		// Remove paint listener
		styledText.removePaintListener(this);

		// Reset vertical indents
		for (Map.Entry<Integer, PullRequestComment> entry : commentsMap
				.entrySet()) {
			int oneBasedLine = entry.getKey().intValue();
			int zeroBasedLine = oneBasedLine - 1;

			if (zeroBasedLine >= 0
					&& zeroBasedLine < styledText.getLineCount()) {
				styledText.setLineVerticalIndent(zeroBasedLine, 0);
			}
		}

		// Dispose colors
		disposeColors();

		// Force redraw
		styledText.redraw();
	}

	@Override
	public void paintControl(PaintEvent e) {
		if (styledText == null || styledText.isDisposed()) {
			return;
		}

		GC gc = e.gc;
		int clientWidth = styledText.getClientArea().width;
		int clientHeight = styledText.getClientArea().height;

		// Paint comments for each commented line in the visible range
		for (Map.Entry<Integer, PullRequestComment> entry : commentsMap
				.entrySet()) {
			int oneBasedLine = entry.getKey().intValue();
			PullRequestComment comment = entry.getValue();
			int zeroBasedLine = oneBasedLine - 1;

			if (zeroBasedLine < 0
					|| zeroBasedLine >= styledText.getLineCount()) {
				continue;
			}

			// Get the pixel position of this line (relative to viewport top)
			int linePixel = styledText.getLinePixel(zeroBasedLine);

			// Get the vertical indent for this line
			int verticalIndent = styledText
					.getLineVerticalIndent(zeroBasedLine);

			if (verticalIndent <= 0) {
				continue; // No space reserved, skip
			}

			// The vertical indent area is ABOVE the line
			// linePixel is relative to viewport, so compare against 0 and clientHeight
			int bubbleTop = linePixel - verticalIndent;
			int bubbleBottom = linePixel;

			// Check if this bubble is in the visible range (viewport-relative coords)
			if (bubbleBottom < 0 || bubbleTop > clientHeight) {
				continue; // Not visible
			}

			// Draw the comment bubble in the vertical indent space
			drawCommentBubble(gc, comment, PADDING_X, bubbleTop, clientWidth,
					verticalIndent);
		}
	}

	/**
	 * Draws a comment bubble at the specified position.
	 *
	 * @param gc
	 *            the graphics context
	 * @param comment
	 *            the comment to draw
	 * @param x
	 *            the left x coordinate
	 * @param y
	 *            the top y coordinate
	 * @param width
	 *            the available width
	 * @param height
	 *            the height of the bubble
	 */
	private void drawCommentBubble(GC gc, PullRequestComment comment, int x,
			int y, int width, int height) {
		boolean isResolved = "RESOLVED".equals(comment.getState()); //$NON-NLS-1$

		// Draw background
		gc.setBackground(isResolved ? resolvedBgColor : bgColor);
		gc.fillRoundRectangle(x, y + 2, width - 2 * PADDING_X, height - 4, ARC,
				ARC);

		// Draw border
		gc.setForeground(borderColor);
		gc.drawRoundRectangle(x, y + 2, width - 2 * PADDING_X, height - 4, ARC,
				ARC);

		FontMetrics fm = gc.getFontMetrics();
		int lineHeight = fm.getHeight() + 2;
		int textX = x + PADDING_X;
		int currentY = y + PADDING_Y + 2;

		// Draw author + timestamp line
		String author = comment.getAuthorDisplayName();
		if (author == null || author.isEmpty()) {
			author = comment.getAuthorName();
		}
		if (author == null) {
			author = "Unknown"; //$NON-NLS-1$
		}
		String timestamp = ""; //$NON-NLS-1$
		if (comment.getCreatedDate() != null) {
			synchronized (DATE_FORMAT) {
				timestamp = DATE_FORMAT.format(comment.getCreatedDate());
			}
		}

		gc.setForeground(authorColor);
		gc.drawString(author, textX, currentY, true);
		int authorWidth = gc.textExtent(author).x;

		gc.setForeground(timestampColor);
		gc.drawString("  " + timestamp, textX + authorWidth, currentY, true); //$NON-NLS-1$

		if (isResolved) {
			String resolvedTag = " [RESOLVED]"; //$NON-NLS-1$
			int tsWidth = gc.textExtent("  " + timestamp).x; //$NON-NLS-1$
			gc.drawString(resolvedTag, textX + authorWidth + tsWidth, currentY,
					true);
		}

		currentY += lineHeight;

		// Draw comment text (truncated if necessary)
		gc.setForeground(textColor);
		String text = comment.getText();
		if (text != null) {
			// Replace newlines with spaces for single-line display
			text = text.replace('\n', ' ').replace('\r', ' ').trim();
			if (text.length() > MAX_TEXT_LENGTH) {
				text = text.substring(0, MAX_TEXT_LENGTH) + "..."; //$NON-NLS-1$
			}
			gc.drawString(text, textX, currentY, true);
		}
		currentY += lineHeight;

		// Draw replies summary
		List<PullRequestComment> replies = comment.getReplies();
		if (replies != null && !replies.isEmpty()) {
			for (PullRequestComment reply : replies) {
				String replyAuthor = reply.getAuthorDisplayName();
				if (replyAuthor == null || replyAuthor.isEmpty()) {
					replyAuthor = reply.getAuthorName();
				}
				if (replyAuthor == null) {
					replyAuthor = "Unknown"; //$NON-NLS-1$
				}
				String replyText = reply.getText();
				if (replyText != null) {
					replyText = replyText.replace('\n', ' ').replace('\r', ' ')
							.trim();
					if (replyText.length() > MAX_TEXT_LENGTH - 20) {
						replyText = replyText.substring(0,
								MAX_TEXT_LENGTH - 20) + "..."; //$NON-NLS-1$
					}
				} else {
					replyText = ""; //$NON-NLS-1$
				}

				gc.setForeground(authorColor);
				String replyPrefix = "\u21B3 " + replyAuthor + ": "; //$NON-NLS-1$ //$NON-NLS-2$
				gc.drawString(replyPrefix, textX + PADDING_X, currentY, true);
				int prefixWidth = gc.textExtent(replyPrefix).x;

				gc.setForeground(textColor);
				gc.drawString(replyText, textX + PADDING_X + prefixWidth,
						currentY, true);
				currentY += lineHeight;
			}
		}
	}

	/**
	 * Calculates the height needed for a comment bubble.
	 *
	 * @param comment
	 *            the comment
	 * @return the height in pixels
	 */
	private int calculateCommentHeight(PullRequestComment comment) {
		if (styledText == null || styledText.isDisposed()) {
			return 0;
		}

		// Calculate number of lines needed
		// 1 line for author + timestamp
		// 1 line for comment text
		// N lines for replies
		int lines = 2;
		List<PullRequestComment> replies = comment.getReplies();
		if (replies != null && !replies.isEmpty()) {
			lines += replies.size();
		}

		// Get line height from font metrics
		GC gc = new GC(styledText);
		try {
			FontMetrics fm = gc.getFontMetrics();
			int lineHeight = fm.getHeight() + 2;
			return lineHeight * lines + 2 * PADDING_Y + 4;
		} finally {
			gc.dispose();
		}
	}

	private void ensureColors() {
		if (styledText == null || styledText.isDisposed()) {
			return;
		}

		if (bgColor == null || bgColor.isDisposed()) {
			bgColor = new Color(styledText.getDisplay(), COMMENT_BG_RGB);
			resolvedBgColor = new Color(styledText.getDisplay(),
					RESOLVED_BG_RGB);
			authorColor = new Color(styledText.getDisplay(), AUTHOR_RGB);
			textColor = new Color(styledText.getDisplay(), TEXT_RGB);
			timestampColor = new Color(styledText.getDisplay(), TIMESTAMP_RGB);
			borderColor = new Color(styledText.getDisplay(), BORDER_RGB);
		}
	}

	private void disposeColors() {
		// On Eclipse 4.x+ / SWT with newer Color API, Colors created via
		// new Color(Device, RGB) are managed and don't strictly need disposal,
		// but we null out references to help GC.
		bgColor = null;
		resolvedBgColor = null;
		authorColor = null;
		textColor = null;
		timestampColor = null;
		borderColor = null;
	}
}
