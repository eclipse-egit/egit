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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

/**
 * Custom {@link TextMergeViewer} subclass that supports displaying inline pull
 * request comments as visual bubbles above commented lines.
 *
 * <p>
 * This viewer uses {@link InlineCommentPainter} to draw comment bubbles
 * directly on the {@link StyledText} widget via
 * {@link StyledText#setLineVerticalIndent(int, int)} and a paint listener. This
 * approach bypasses Eclipse's annotation framework, making it compatible with
 * {@link TextMergeViewer}'s document lifecycle management.
 * </p>
 */
public class InlineCommentTextMergeViewer extends TextMergeViewer {

	private SourceViewer leftSourceViewer;

	private SourceViewer rightSourceViewer;

	private InlineCommentPainter leftPainter;

	private InlineCommentPainter rightPainter;

	/**
	 * Counter for configureTextViewer calls. The call order is always ancestor
	 * (0), left (1), right (2) — even in 2-way mode where the ancestor pane is
	 * created but hidden.
	 */
	private int configureCount;

	/**
	 * Pending comments to be applied once documents are available
	 */
	private List<PullRequestComment> pendingComments;

	/**
	 * Constructor
	 *
	 * @param parent
	 *            the parent composite
	 * @param configuration
	 *            the compare configuration
	 */
	public InlineCommentTextMergeViewer(Composite parent,
			CompareConfiguration configuration) {
		super(parent, configuration);
	}

	@Override
	protected void configureTextViewer(
			org.eclipse.jface.text.TextViewer textViewer) {
		super.configureTextViewer(textViewer);

		// Track left/right SourceViewer references.
		// configureTextViewer is called in order: ancestor (0), left (1),
		// right (2) — even in 2-way mode (ancestor is created but hidden).
		if (textViewer instanceof SourceViewer) {
			SourceViewer sv = (SourceViewer) textViewer;
			int index = configureCount++;
			if (index == 1) {
				leftSourceViewer = sv;
			} else if (index == 2) {
				rightSourceViewer = sv;
			}
		}
	}

	@Override
	protected void updateContent(Object ancestor, Object left, Object right) {
		super.updateContent(ancestor, left, right);

		// After content is updated, documents should be available
		// Try to apply pending comments if any
		if (pendingComments != null) {
			applyComments(pendingComments);
			pendingComments = null;
		}
	}

	/**
	 * Sets the pull request comments to display as inline annotations.
	 *
	 * <p>
	 * Only inline comments (those with a non-null {@code line} and
	 * {@code path}) are rendered. Comments are placed on the left side
	 * ({@code fileType == "FROM"}) or right side ({@code fileType == "TO"}).
	 * </p>
	 *
	 * @param comments
	 *            the list of comments for the current file
	 */
	public void setComments(List<PullRequestComment> comments) {
		// If documents are not yet available, store comments as pending
		boolean documentsReady = (leftSourceViewer != null
				&& leftSourceViewer.getDocument() != null)
				|| (rightSourceViewer != null
						&& rightSourceViewer.getDocument() != null);

		if (!documentsReady) {
			pendingComments = comments;
			return;
		}

		applyComments(comments);
	}

	/**
	 * Actually applies comments to the viewers. This is called either
	 * immediately from setComments() if documents are ready, or deferred until
	 * updateContent() is called.
	 *
	 * @param comments
	 *            the list of comments to apply
	 */
	private void applyComments(List<PullRequestComment> comments) {
		// Clear previous painters
		clearPainters();

		if (comments == null || comments.isEmpty()) {
			return;
		}

		// Separate comments by side (LEFT = "FROM", RIGHT = "TO")
		List<PullRequestComment> leftComments = new ArrayList<>();
		List<PullRequestComment> rightComments = new ArrayList<>();

		for (PullRequestComment comment : comments) {
			if (!comment.isInlineComment()) {
				continue;
			}
			if (comment.getLine() == null || comment.getLine().intValue() < 1) {
				continue;
			}

			String fileType = comment.getFileType();
			boolean isLeft = "FROM".equals(fileType); //$NON-NLS-1$

			if (isLeft) {
				leftComments.add(comment);
			} else {
				rightComments.add(comment);
			}
		}

		// Install painters for left and right sides
		if (leftSourceViewer != null && !leftComments.isEmpty()) {
			IDocument doc = leftSourceViewer.getDocument();
			StyledText styledText = leftSourceViewer.getTextWidget();
			if (doc != null && styledText != null
					&& !styledText.isDisposed()) {
				leftPainter = new InlineCommentPainter(styledText, doc,
						leftComments);
				leftPainter.install();
			}
		}

		if (rightSourceViewer != null && !rightComments.isEmpty()) {
			IDocument doc = rightSourceViewer.getDocument();
			StyledText styledText = rightSourceViewer.getTextWidget();
			if (doc != null && styledText != null
					&& !styledText.isDisposed()) {
				rightPainter = new InlineCommentPainter(styledText, doc,
						rightComments);
				rightPainter.install();
			}
		}
	}

	/**
	 * Removes all inline comment painters from both panes.
	 */
	private void clearPainters() {
		if (leftPainter != null) {
			leftPainter.uninstall();
			leftPainter = null;
		}
		if (rightPainter != null) {
			rightPainter.uninstall();
			rightPainter = null;
		}
	}

	@Override
	protected void handleDispose(org.eclipse.swt.events.DisposeEvent event) {
		clearPainters();
		leftSourceViewer = null;
		rightSourceViewer = null;
		super.handleDispose(event);
	}
}
