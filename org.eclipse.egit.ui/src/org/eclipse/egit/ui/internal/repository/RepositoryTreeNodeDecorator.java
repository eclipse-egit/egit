/*******************************************************************************
 * Copyright (c) 2018, 2019 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *    Simon Muschel <smuschel@gmx.de> - Bug 422365
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.UnitOfWork;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.ToggleCommand;
import org.eclipse.egit.ui.internal.decorators.DecoratorRepositoryStateCache;
import org.eclipse.egit.ui.internal.decorators.GitDecorator;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroupNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.resources.IResourceState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;

/**
 * Lightweight decorator for {@link RepositoryTreeNode}s. Note that this
 * decorator does <em>not</em> listen on "references changed" events to fire
 * {@link org.eclipse.jface.viewers.LabelProviderChangedEvent
 * LabelProviderChangedEvent}s -- the RepositoriesView does so and refreshes
 * itself completely.
 */
public class RepositoryTreeNodeDecorator extends GitDecorator
		implements IStateListener {

	private static final String HAS_CHANGES_PREFIX = "> "; //$NON-NLS-1$

	private static final String OPEN_BRACKET = " ["; //$NON-NLS-1$

	private static final String OPEN_PARENTHESIS = " ("; //$NON-NLS-1$

	private static final String MULTIPLE_REPOSITORIES = "*"; //$NON-NLS-1$

	private final State verboseBranchModeState;

	private boolean verboseBranchMode = false;

	private final RefCache.Cache refCache = RefCache.get();

	/**
	 * Constructs a repositories view label provider
	 */
	public RepositoryTreeNodeDecorator() {
		ICommandService srv = PlatformUI.getWorkbench()
				.getService(ICommandService.class);
		verboseBranchModeState = srv
				.getCommand(ToggleCommand.COMMIT_MESSAGE_DECORATION_ID)
				.getState(RegistryToggleState.STATE_ID);
		verboseBranchModeState.addListener(this);
		try {
			this.verboseBranchMode = ((Boolean) verboseBranchModeState
					.getValue()).booleanValue();
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}

	}

	@Override
	public void dispose() {
		verboseBranchModeState.removeListener(this);
		refCache.dispose();
		super.dispose();
	}

	@Override
	public void handleStateChange(State state, Object oldValue) {
		try {
			boolean newValue = ((Boolean) state.getValue())
					.booleanValue();
			if (newValue != verboseBranchMode) {
				verboseBranchMode = newValue;
				postLabelEvent();
			}
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		RepositoryTreeNode<?> node = (RepositoryTreeNode) element;
		Repository repository = node.getRepository();
		if (repository != null) {
			UnitOfWork.execute(repository,
					() -> decorateNode(node, repository, decoration));
		} else if (node.getType() == RepositoryTreeNodeType.REPOGROUP) {
			decorateRepositoryGroup(node, decoration);
		}
	}

	private void decorateNode(RepositoryTreeNode<?> node,
			@NonNull Repository repository, IDecoration decoration) {
		try {
			decorateText(node, repository, decoration);
			decorateIcon(node, repository, decoration);
		} catch (IOException e) {
			Activator.logError(MessageFormat.format(
					UIText.GitLabelProvider_UnableToRetrieveLabel,
					node.toString()), e);
		}
	}

	private void decorateIcon(RepositoryTreeNode<?> node,
			@NonNull Repository repository, IDecoration decoration)
			throws IOException {
		switch (node.getType()) {
		case TAG: {
			String branchName = DecoratorRepositoryStateCache.INSTANCE
					.getFullBranchName(repository);
			if (branchName == null) {
				return;
			}
			// HEAD would be on the commit id to which the tag is pointing
			if (branchName.equals(((TagNode) node).getCommitId())) {
				decoration.addOverlay(UIIcons.OVR_CHECKEDOUT,
						IDecoration.TOP_LEFT);
			}
			break;
		}
		case ADDITIONALREF: {
			Ref ref = refCache.findAdditional(repository,
					((Ref) node.getObject()).getName());
			if (ref != null) {
				decorateRefIcon(repository, ref, decoration);
			}
			break;
		}
		case REF: {
			Ref ref = refCache.exact(repository,
					((Ref) node.getObject()).getName());
			if (ref != null) {
				decorateRefIcon(repository, ref, decoration);
			}
			break;
		}
		case WORKINGDIR:
		case FOLDER:
		case FILE:
			decorateConflict(node.getPath().toFile(), decoration);
			break;
		default:
			break;
		}
	}

	private void decorateRefIcon(@NonNull Repository repository, Ref ref,
			IDecoration decoration) {
		String branchName = DecoratorRepositoryStateCache.INSTANCE
				.getFullBranchName(repository);
		if (branchName == null) {
			return;
		}
		String refName = ref.getName();
		Ref leaf = ref.getLeaf();

		String compareString = null;
		if (refName.startsWith(Constants.R_HEADS)) {
			// local branch: HEAD would be on the branch
			compareString = refName;
		} else if (refName.startsWith(Constants.R_REMOTES)) {
			// remote branch: branch name is object id in detached HEAD
			// state
			ObjectId objectId = leaf.getObjectId();
			if (objectId != null) {
				String leafName = objectId.getName();
				if (leafName.equals(branchName)) {
					decoration.addOverlay(UIIcons.OVR_CHECKEDOUT,
							IDecoration.TOP_LEFT);
					return;
				}
			}
		} else if (refName.equals(Constants.HEAD)) {
			decoration.addOverlay(UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
			return;
		} else {
			String leafname = leaf.getName();
			if (leafname.startsWith(Constants.R_REFS)
					&& leafname.equals(branchName)) {
				decoration.addOverlay(UIIcons.OVR_CHECKEDOUT,
						IDecoration.TOP_LEFT);
				return;
			}
			ObjectId objectId = leaf.getObjectId();
			if (objectId != null
					&& objectId.equals(DecoratorRepositoryStateCache.INSTANCE
							.getHead(repository))) {
				decoration.addOverlay(UIIcons.OVR_CHECKEDOUT,
						IDecoration.TOP_LEFT);
			}
			// some other symbolic reference
			return;
		}

		if (branchName.equals(compareString)) {
			decoration.addOverlay(UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
		}
	}

	private void decorateConflict(File file, IDecoration decoration) {
		IResourceState state = ResourceStateFactory.getInstance().get(file);
		if (state.hasConflicts()) {
			decoration.addOverlay(UIIcons.OVR_CONFLICT);
		}
	}

	private void decorateText(RepositoryTreeNode<?> node,
			@NonNull Repository repository, IDecoration decoration)
			throws IOException {
		switch (node.getType()) {
		case REPO:
			decorateRepository(node, repository, decoration);
			break;
		case ADDITIONALREF:
			decorateAdditionalRef((AdditionalRefNode) node,
					decoration);
			break;
		case REF:
			decorateRef((RefNode) node, decoration);
			break;
		case TAG:
			decorateTag((TagNode) node, decoration);
			break;
		case STASHED_COMMIT:
			decorateStash((StashedCommitNode) node, decoration);
			break;
		case SUBMODULES:
			decorateSubmodules(repository, decoration);
			break;
		default:
			return;
		}
	}

	private void decorateAdditionalRef(AdditionalRefNode node,
			IDecoration decoration) throws IOException {
		String name = node.getObject().getName();
		Ref ref = refCache.findAdditional(node.getRepository(), name);
		if (ref == null) {
			return;
		}
		StringBuilder suffix = new StringBuilder();
		if (ref.isSymbolic()) {
			suffix.append(OPEN_BRACKET).append(ref.getLeaf().getName())
					.append(']');
		}
		ObjectId refId = ref.getObjectId();
		suffix.append(' ');
		RevCommit commit = getLatestCommit(node.getRepository(), ref);
		if (commit != null) {
			suffix.append(abbreviate(commit)).append(' ')
					.append(commit.getShortMessage());
		} else if (!ref.isSymbolic() || refId != null) {
			suffix.append(abbreviate(refId));
		} else {
			suffix.append(
					UIText.RepositoriesViewLabelProvider_UnbornBranchText);
		}
		decoration.addSuffix(suffix.toString());
	}

	private void decorateRef(RefNode node, IDecoration decoration)
			throws IOException {
		if (verboseBranchMode) {
			Ref ref = refCache.exact(node.getRepository(),
					node.getObject().getName());
			if (ref == null) {
				return;
			}
			RevCommit latest = getLatestCommit(node.getRepository(), ref);
			if (latest != null) {
				decoration.addSuffix(" " + abbreviate(latest) + ' ' //$NON-NLS-1$
						+ latest.getShortMessage());
			}
		}
	}

	private void decorateRepository(RepositoryTreeNode<?> node,
			@NonNull Repository repository, IDecoration decoration) {
		boolean isSubModule = node.getParent() != null && node.getParent()
				.getType() == RepositoryTreeNodeType.SUBMODULES;
		if (RepositoryUtil.hasChanges(repository)) {
			decoration.addPrefix(HAS_CHANGES_PREFIX);
		}
		StringBuilder suffix = new StringBuilder();
		if (isSubModule) {
			Ref head = DecoratorRepositoryStateCache.INSTANCE
					.getHeadRef(repository);
			if (head == null) {
				return;
			}
			suffix.append(OPEN_BRACKET);
			if (head.isSymbolic()) {
				suffix.append(
						Repository.shortenRefName(head.getLeaf().getName()));
			} else if (head.getObjectId() != null) {
				suffix.append(abbreviate(head.getObjectId()));
			}
			suffix.append(']');
			if (verboseBranchMode && head.getObjectId() != null) {
				RevCommit commit = DecoratorRepositoryStateCache.INSTANCE
						.getHeadCommit(repository);
				if (commit != null) {
					suffix.append(' ').append(commit.getShortMessage());
				}
			}
		} else {
			// Not a submodule
			String branch = DecoratorRepositoryStateCache.INSTANCE
					.getCurrentBranchLabel(repository);
			if (branch == null) {
				return;
			}
			suffix.append(OPEN_BRACKET);
			suffix.append(branch);

			String trackingStatus = DecoratorRepositoryStateCache.INSTANCE
					.getBranchStatus(repository);
			if (trackingStatus != null) {
				suffix.append(' ').append(trackingStatus);
			}
			RepositoryState repositoryState = DecoratorRepositoryStateCache.INSTANCE
					.getRepositoryState(repository);
			if (repositoryState != RepositoryState.SAFE) {
				suffix.append(" - ") //$NON-NLS-1$
						.append(repositoryState.getDescription());
			}
			suffix.append(']');
		}
		decoration.addSuffix(suffix.toString());
	}

	private void decorateRepositoryGroup(RepositoryTreeNode<?> node,
			IDecoration decoration) {
		RepositoryCache cache = org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache();
		RepositoryGroup group = ((RepositoryGroupNode) node).getObject();
		boolean markGroupDirty = false;
		int numberOfBranches = 0;
		String singleBranch = null;
		String singleRepoName = null;
		int numberOfDirectories = group.getRepositoryDirectories().size();
		for (File repoDir : group.getRepositoryDirectories()) {
			Repository repo = cache.getRepository(repoDir);
			if (repo != null) {
				if (numberOfDirectories == 1) {
					singleRepoName = DecoratorRepositoryStateCache.INSTANCE
							.getRepositoryNameAndState(repo);
				}
				if (!markGroupDirty && RepositoryUtil.hasChanges(repo)) {
					markGroupDirty = true;
				}
				if (numberOfBranches <= 1) {
					String thisBranch = Repository.shortenRefName(
							DecoratorRepositoryStateCache.INSTANCE
									.getFullBranchName(repo));
					if (!thisBranch.equals(singleBranch)) {
						numberOfBranches++;
					}
					if (singleBranch == null) {
						singleBranch = thisBranch;
					}
				}
				if (markGroupDirty && numberOfBranches > 1) {
					break;
				}
			}
		}
		if (markGroupDirty) {
			decoration.addPrefix(HAS_CHANGES_PREFIX);
		}
		if (numberOfBranches == 1) {
			String repoLabel = singleRepoName != null ? singleRepoName
					: MULTIPLE_REPOSITORIES;
			decoration.addSuffix(
					OPEN_BRACKET + repoLabel + ' ' + singleBranch + ']');
		} else if (numberOfDirectories > 1) {
			decoration.addSuffix(OPEN_PARENTHESIS + numberOfDirectories + ')');
		}
	}

	private void decorateStash(StashedCommitNode node,
			IDecoration decoration) {
		RevCommit commit = node.getObject();
		decoration.addSuffix(
				OPEN_BRACKET + abbreviate(commit) + "] " //$NON-NLS-1$
				+ commit.getShortMessage());
	}

	private void decorateSubmodules(@NonNull Repository repository,
			IDecoration decoration) {
		if (haveSubmoduleChanges(repository)) {
			decoration.addPrefix(HAS_CHANGES_PREFIX);
		}
	}

	private void decorateTag(TagNode node, IDecoration decoration) {
		if (verboseBranchMode && node.getCommitId() != null
				&& node.getCommitId().length() > 0) {
			decoration.addSuffix(" " + node.getCommitId().substring(0, 7) + ' ' //$NON-NLS-1$
					+ node.getCommitShortMessage());
		}
	}

	private RevCommit getLatestCommit(Repository repository, Ref ref) {
		ObjectId id;
		if (ref.isSymbolic()) {
			id = ref.getLeaf().getObjectId();
		} else {
			id = ref.getObjectId();
		}
		if (id == null) {
			return null;
		}
		try (RevWalk walk = new RevWalk(repository)) {
			walk.setRetainBody(true);
			return walk.parseCommit(id);
		} catch (IOException ignored) {
			return null;
		}
	}

	private String abbreviate(final ObjectId id) {
		if (id != null) {
			return Utils.getShortObjectId(id);
		} else {
			return Utils.getShortObjectId(ObjectId.zeroId());
		}
	}

	private boolean haveSubmoduleChanges(@NonNull Repository repository) {
		IndexDiffCache cache = org.eclipse.egit.core.Activator.getDefault()
				.getIndexDiffCache();
		if (cache == null) {
			return false;
		}
		IndexDiffCacheEntry entry = cache.getIndexDiffCacheEntry(repository);
		IndexDiffData data = entry != null ? entry.getIndexDiff() : null;
		if (data == null) {
			return false;
		}
		Set<String> modified = data.getModified();
		return data.getSubmodules().stream()
				.anyMatch(modified::contains);
	}

	@Override
	protected String getName() {
		return UIText.RepositoryTreeNodeDecorator_name;
	}
}
