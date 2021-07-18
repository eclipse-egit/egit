/*******************************************************************************
 * Copyright (C) 2010, 2021 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffContainer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

/**
 * A Git-specific {@link CompareEditorInput} for comparing the workspace against
 * a commit, or a commit against a commit, performing a two-way diff.
 */
public class GitCompareEditorInput extends AbstractGitCompareEditorInput {

	private final String leftVersion;

	private final String rightVersion;

	/**
	 * Creates a new {@link GitCompareEditorInput} with the comparison filtered
	 * to the given paths.
	 *
	 * @param leftVersion
	 *            git object name (ref name, commit id) to show on the left side
	 * @param rightVersion
	 *            git object name (ref name, commit id) to compare against;
	 *            shown on the right side
	 * @param repository
	 *            repository where resources are coming from
	 * @param paths
	 *            as selected by the user
	 */
	public GitCompareEditorInput(String leftVersion, String rightVersion,
			Repository repository, IPath... paths) {
		super(repository, paths);
		this.leftVersion = leftVersion;
		this.rightVersion = rightVersion;
	}

	@Override
	protected DiffContainer buildInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		Repository repo = getRepository();
		try (RevWalk rw = new RevWalk(repo)) {
			RevCommit leftCommit = rw.parseCommit(repo.resolve(leftVersion));
			RevCommit rightCommit = rw.parseCommit(repo.resolve(rightVersion));

			CompareConfiguration config = getCompareConfiguration();
			// Labels based on the FileRevisionTypedElements
			config.setDefaultLabelProvider(new GitCompareLabelProvider());
			// Fallback labels
			config.setLeftLabel(leftVersion);
			config.setRightLabel(rightVersion);

			setTitle(MessageFormat.format(
					UIText.GitCompareEditorInput_EditorTitle,
					RepositoryUtil.INSTANCE.getRepositoryName(repo),
					CompareUtils.truncatedRevision(leftVersion),
					CompareUtils.truncatedRevision(rightVersion)));

			return buildDiffContainer(leftCommit, rightCommit, monitor);
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
	}

	private DiffContainer buildDiffContainer(RevCommit leftCommit,
			RevCommit rightCommit, IProgressMonitor monitor)
			throws IOException, InterruptedException {
		DiffContainer result = new DiffNode(Differencer.CONFLICTING);

		Repository repo = getRepository();
		try (TreeWalk tw = new TreeWalk(repo)) {

			int leftIndex = tw.addTree(new CanonicalTreeParser(null,
					repo.newObjectReader(), leftCommit.getTree()));
			int rightIndex = tw.addTree(new CanonicalTreeParser(null,
					repo.newObjectReader(), rightCommit.getTree()));

			// filter by selected resources
			Collection<String> filterPaths = getFilterPaths();
			if (!filterPaths.isEmpty()) {
				if (filterPaths.size() > 1) {
					tw.setFilter(
							PathFilterGroup.createFromStrings(filterPaths));
				} else {
					String path = filterPaths.iterator().next();
					if (!path.isEmpty()) {
						tw.setFilter(PathFilterGroup.createFromStrings(path));
					}
				}
			}

			tw.setRecursive(true);

			CheckoutData data = new CheckoutData();
			while (tw.next()) {
				if (monitor.isCanceled()) {
					throw new InterruptedException();
				}
				AbstractTreeIterator leftIter = tw.getTree(leftIndex,
						AbstractTreeIterator.class);
				AbstractTreeIterator rightIter = tw.getTree(rightIndex,
						AbstractTreeIterator.class);
				data.clear();

				String gitPath = tw.getPathString();

				Supplier<ITypedElement> leftItem = () -> {
					data.fill(repo, tw, gitPath);
					GitFileRevision revision = GitFileRevision.inCommit(repo,
							leftCommit, gitPath,
							leftIter.getEntryObjectId(), data.getMetadata());
					return new FileRevisionTypedElement(revision,
							data.getEncoding());
				};
				Supplier<ITypedElement> rightItem = () -> {
					data.fill(repo, tw, gitPath);
					GitFileRevision revision = GitFileRevision.inCommit(repo,
							rightCommit, gitPath,
							rightIter.getEntryObjectId(), data.getMetadata());
					return new FileRevisionTypedElement(revision,
							data.getEncoding());
				};

				MergeDiffNode node = twoWayDiff(leftIter, rightIter,
						leftItem, rightItem);
				if (node != null) {
					getFileParent(result, gitPath).add(node);
				}
			}
			return result;
		}
	}

	private MergeDiffNode twoWayDiff(AbstractTreeIterator leftIter,
			AbstractTreeIterator rightIter,
			Supplier<ITypedElement> leftItem,
			Supplier<ITypedElement> rightItem) {
		int kind;
		ITypedElement left = null;
		ITypedElement right = null;
		if (leftIter == null) {
			if (rightIter == null) {
				return null;
			}
			kind = Differencer.LEFT + Differencer.DELETION;
			right = rightItem.get();
		} else if (rightIter == null) {
			kind = Differencer.LEFT + Differencer.ADDITION;
			left = leftItem.get();
		} else if (leftIter.getEntryObjectId()
				.equals(rightIter.getEntryObjectId())) {
			return null;
		} else {
			kind = Differencer.CHANGE;
			left = leftItem.get();
			right = rightItem.get();
		}
		return new MergeDiffNode(kind, null, left, right);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = result * 31 + Objects.hash(leftVersion, rightVersion);
		Repository repo = getRepository();
		if (repo != null) {
			result = result * 31 + repo.getDirectory().hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		GitCompareEditorInput other = (GitCompareEditorInput) obj;
		if (!Objects.equals(rightVersion, other.rightVersion)
				|| !Objects.equals(leftVersion, other.leftVersion)) {
			return false;
		}
		Repository repo = getRepository();
		File myDir = repo == null ? null : repo.getDirectory();
		repo = other.getRepository();
		File otherDir = repo == null ? null : repo.getDirectory();
		return Objects.equals(myDir, otherDir);
	}

	private static class CheckoutData {

		private boolean filled;

		private String encoding;

		private CheckoutMetadata metadata;

		void fill(Repository repository, TreeWalk walk, String gitPath) {
			if (!filled) {
				filled = true;
				encoding = CompareCoreUtils.getResourceEncoding(repository,
						gitPath);
				try {
					metadata = new CheckoutMetadata(
							walk.getEolStreamType(
									TreeWalk.OperationType.CHECKOUT_OP),
							walk.getFilterCommand(
									Constants.ATTR_FILTER_TYPE_SMUDGE));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

		String getEncoding() {
			return encoding;
		}

		CheckoutMetadata getMetadata() {
			return metadata;
		}

		void clear() {
			filled = false;
			encoding = null;
			metadata = null;
		}
	}
}
