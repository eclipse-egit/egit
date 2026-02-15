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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.bitbucket.BitbucketClient;
import org.eclipse.egit.core.internal.bitbucket.PullRequest;
import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;

/**
 * Compare editor input for comparing files from a Bitbucket pull request
 */
public class BitbucketCompareEditorInput extends CompareEditorInput {

	private final BitbucketClient client;

	private final PullRequest pullRequest;

	private final PullRequestChangedFile changedFile;

	private Viewer contentViewer;

	private List<PullRequestComment> comments = new ArrayList<>();

	/**
	 * Creates a new BitbucketCompareEditorInput
	 *
	 * @param client
	 *            the Bitbucket client
	 * @param pullRequest
	 *            the pull request
	 * @param changedFile
	 *            the changed file to compare
	 */
	public BitbucketCompareEditorInput(BitbucketClient client,
			PullRequest pullRequest, PullRequestChangedFile changedFile) {
		super(new CompareConfiguration());
		this.client = client;
		this.pullRequest = pullRequest;
		this.changedFile = changedFile;

		// Configure the compare editor
		CompareConfiguration config = getCompareConfiguration();
		config.setLeftEditable(false);
		config.setRightEditable(false);

		// Set labels for left and right sides
		String fromBranch = pullRequest.getFromRef().getDisplayId();
		String toBranch = pullRequest.getToRef().getDisplayId();

		switch (changedFile.getChangeType()) {
		case ADDED:
			config.setLeftLabel("(non-existent)"); //$NON-NLS-1$
			config.setRightLabel(fromBranch + " (new file)"); //$NON-NLS-1$
			break;
		case DELETED:
			config.setLeftLabel(toBranch + " (deleted)"); //$NON-NLS-1$
			config.setRightLabel("(non-existent)"); //$NON-NLS-1$
			break;
		case RENAMED:
			config.setLeftLabel(toBranch + " (old: " + changedFile.getOldPath() //$NON-NLS-1$
					+ ")"); //$NON-NLS-1$
			config.setRightLabel(fromBranch + " (new: " + changedFile.getPath() //$NON-NLS-1$
					+ ")"); //$NON-NLS-1$
			break;
		case MODIFIED:
		default:
			config.setLeftLabel(toBranch);
			config.setRightLabel(fromBranch);
			break;
		}

		setTitle(MessageFormat.format("Compare {0}", changedFile.getName())); //$NON-NLS-1$
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		return createCompareInput(client, pullRequest, changedFile, monitor);
	}

	@Override
	public Viewer findContentViewer(Viewer oldViewer, ICompareInput input,
			Composite parent) {
		// Check if inline comments should be displayed
		boolean useInlineComments = Activator.getDefault()
				.getPreferenceStore()
				.getBoolean(UIPreferences.PULLREQUEST_SHOW_INLINE_COMMENTS);

		if (useInlineComments && comments != null && !comments.isEmpty()) {
			// Check if we can reuse the existing viewer
			if (oldViewer instanceof InlineCommentTextMergeViewer) {
				InlineCommentTextMergeViewer inlineViewer = (InlineCommentTextMergeViewer) oldViewer;
				inlineViewer.setComments(comments);
				contentViewer = inlineViewer;
				return contentViewer;
			}

			// Need to create a new InlineCommentTextMergeViewer
			// Only do this if oldViewer is null or incompatible
			if (oldViewer == null) {
				InlineCommentTextMergeViewer inlineViewer = new InlineCommentTextMergeViewer(
						parent, getCompareConfiguration());
				inlineViewer.setComments(comments);
				contentViewer = inlineViewer;
				return contentViewer;
			}
			// If oldViewer exists but is not our type, fall through to default
			// to avoid handler conflicts
		}

		// Use default viewer
		contentViewer = super.findContentViewer(oldViewer, input, parent);
		return contentViewer;
	}

	/**
	 * Set the comments to display as inline annotations in the compare editor
	 *
	 * @param comments
	 *            the list of comments for this file
	 */
	public void setComments(List<PullRequestComment> comments) {
		if (comments != null) {
			this.comments = new ArrayList<>(comments);
		} else {
			this.comments.clear();
		}
	}

	/**
	 * Creates a compare input (DiffNode) for the given changed file
	 *
	 * @param client
	 *            the Bitbucket client
	 * @param pullRequest
	 *            the pull request
	 * @param changedFile
	 *            the changed file to compare
	 * @param monitor
	 *            progress monitor
	 * @return the DiffNode for comparison
	 * @throws InvocationTargetException
	 *             if an error occurs
	 * @throws InterruptedException
	 *             if the operation is interrupted
	 */
	public static Object createCompareInput(BitbucketClient client,
			PullRequest pullRequest, PullRequestChangedFile changedFile,
			IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		monitor.beginTask("Comparing files...", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

		try {
			String projectKey = pullRequest.getToRef().getRepository()
					.getProject().getKey();
			String repoSlug = pullRequest.getToRef().getRepository().getSlug();

			// Get commit IDs - use displayId (branch name) as commit reference
			String fromCommitId = pullRequest.getFromRef().getDisplayId();
			String toCommitId = pullRequest.getToRef().getDisplayId();

			Object left = null;
			Object right = null;

			switch (changedFile.getChangeType()) {
			case ADDED:
				// File doesn't exist in target branch (left side)
				left = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						changedFile.getName());
				// File exists in source branch (right side)
				right = new BitbucketFileTypedElement(client, projectKey,
						repoSlug, fromCommitId, changedFile.getPath());
				break;

			case DELETED:
				// File exists in target branch (left side)
				left = new BitbucketFileTypedElement(client, projectKey,
						repoSlug, toCommitId, changedFile.getPath());
				// File doesn't exist in source branch (right side)
				right = new GitCompareFileRevisionEditorInput.EmptyTypedElement(
						changedFile.getName());
				break;

			case RENAMED:
				// Old file in target branch (left side)
				left = new BitbucketFileTypedElement(client, projectKey,
						repoSlug, toCommitId, changedFile.getOldPath());
				// New file in source branch (right side)
				right = new BitbucketFileTypedElement(client, projectKey,
						repoSlug, fromCommitId, changedFile.getPath());
				break;

			case MODIFIED:
			default:
				// File in target branch (left side)
				left = new BitbucketFileTypedElement(client, projectKey,
						repoSlug, toCommitId, changedFile.getPath());
				// File in source branch (right side)
				right = new BitbucketFileTypedElement(client, projectKey,
						repoSlug, fromCommitId, changedFile.getPath());
				break;
			}

			return new DiffNode((ITypedElement) left, (ITypedElement) right);

		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates a configured CompareConfiguration for the given changed file
	 *
	 * @param pullRequest
	 *            the pull request
	 * @param changedFile
	 *            the changed file
	 * @return configured CompareConfiguration
	 */
	public static CompareConfiguration createCompareConfiguration(
			PullRequest pullRequest, PullRequestChangedFile changedFile) {
		CompareConfiguration config = new CompareConfiguration();
		config.setLeftEditable(false);
		config.setRightEditable(false);

		// Set labels for left and right sides
		String fromBranch = pullRequest.getFromRef().getDisplayId();
		String toBranch = pullRequest.getToRef().getDisplayId();

		switch (changedFile.getChangeType()) {
		case ADDED:
			config.setLeftLabel("(non-existent)"); //$NON-NLS-1$
			config.setRightLabel(fromBranch + " (new file)"); //$NON-NLS-1$
			break;
		case DELETED:
			config.setLeftLabel(toBranch + " (deleted)"); //$NON-NLS-1$
			config.setRightLabel("(non-existent)"); //$NON-NLS-1$
			break;
		case RENAMED:
			config.setLeftLabel(toBranch + " (old: " + changedFile.getOldPath() //$NON-NLS-1$
					+ ")"); //$NON-NLS-1$
			config.setRightLabel(fromBranch + " (new: " + changedFile.getPath() //$NON-NLS-1$
					+ ")"); //$NON-NLS-1$
			break;
		case MODIFIED:
		default:
			config.setLeftLabel(toBranch);
			config.setRightLabel(fromBranch);
			break;
		}

		return config;
	}

	/**
	 * Get the changed file being compared
	 *
	 * @return the changed file
	 */
	public PullRequestChangedFile getChangedFile() {
		return changedFile;
	}

	/**
	 * Get the control for the compare viewer. This allows external code to
	 * traverse the widget tree to find specific controls like StyledText.
	 *
	 * @return the viewer control, or null if not available
	 */
	public org.eclipse.swt.widgets.Control getViewerControl() {
		if (contentViewer != null && contentViewer.getControl() != null) {
			return contentViewer.getControl();
		}
		return null;
	}
}
