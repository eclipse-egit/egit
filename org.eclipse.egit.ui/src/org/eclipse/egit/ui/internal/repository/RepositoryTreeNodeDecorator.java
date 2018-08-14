/*******************************************************************************
 * Copyright (c) 2018 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.GitDecorator;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.command.ToggleBranchCommitCommand;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Lightweight decorator for {@link RepositoryTreeNode}s. Note that this
 * decorator does <em>not</em> listen on "references changed" events to fire
 * {@link org.eclipse.jface.viewers.LabelProviderChangedEvent
 * LabelProviderChangedEvent}s -- the RepositoriesView does so and refreshes
 * itself completely.
 */
public class RepositoryTreeNodeDecorator extends GitDecorator
		implements IStateListener {

	private final State verboseBranchModeState;

	private boolean verboseBranchMode = false;

	/**
	 * Constructs a repositories view label provider
	 */
	public RepositoryTreeNodeDecorator() {
		ICommandService srv = CommonUtils.getService(PlatformUI.getWorkbench(), ICommandService.class);
		verboseBranchModeState = srv.getCommand(ToggleBranchCommitCommand.ID)
				.getState(ToggleBranchCommitCommand.TOGGLE_STATE);
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
			try {
				decorateText(node, repository, decoration);
			} catch (IOException e) {
				Activator.logError(MessageFormat.format(
						UIText.GitLabelProvider_UnableToRetrieveLabel,
						element.toString()), e);
			}
		}
	}

	private void decorateText(RepositoryTreeNode<?> node,
			@NonNull Repository repository, IDecoration decoration)
			throws IOException {
		boolean decorated = false;
		switch (node.getType()) {
		case REPO:
			decorated = decorateRepository(node, repository, decoration);
			break;
		case ADDITIONALREF:
			decorated = decorateAdditionalRef((AdditionalRefNode) node,
					decoration);
			break;
		case REF:
			decorated = decorateRef((RefNode) node, decoration);
			break;
		case TAG:
			decorated = decorateTag((TagNode) node, decoration);
			break;
		case STASHED_COMMIT:
			decorated = decorateStash((StashedCommitNode) node, decoration);
			break;
		case SUBMODULES:
			decorated = decorateSubmodules(repository, decoration);
			break;
		default:
			return;
		}
		if (!decorated) {
			// Ensure the caching of last labels in
			// RepositoryTreeNodeLabelProvider works
			decoration.addSuffix(" "); //$NON-NLS-1$
		}
	}

	private boolean decorateAdditionalRef(AdditionalRefNode node,
			IDecoration decoration) {
		Ref ref = node.getObject();
		StringBuilder suffix = new StringBuilder();
		if (ref.isSymbolic()) {
			suffix.append(" [").append(ref.getLeaf().getName()).append(']'); //$NON-NLS-1$
		}
		ObjectId refId = ref.getObjectId();
		suffix.append(' ');
		RevCommit commit = getLatestCommit(node);
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
		return true;
	}

	private boolean decorateRef(RefNode node, IDecoration decoration) {
		if (verboseBranchMode) {
			RevCommit latest = getLatestCommit(node);
			if (latest != null) {
				decoration.addSuffix(" " + abbreviate(latest) + ' ' //$NON-NLS-1$
						+ latest.getShortMessage());
				return true;
			}
		}
		return false;
	}

	private boolean decorateRepository(RepositoryTreeNode<?> node,
			@NonNull Repository repository, IDecoration decoration)
			throws IOException {
		boolean isSubModule = node.getParent() != null && node.getParent()
				.getType() == RepositoryTreeNodeType.SUBMODULES;
		if (RepositoryUtil.hasChanges(repository)) {
			decoration.addPrefix("> "); //$NON-NLS-1$
		}
		StringBuilder suffix = new StringBuilder();
		if (isSubModule) {
			Ref head = repository.exactRef(Constants.HEAD);
			if (head == null) {
				return false;
			}
			suffix.append(" ["); //$NON-NLS-1$
			if (head.isSymbolic()) {
				suffix.append(
						Repository.shortenRefName(head.getLeaf().getName()));
			} else if (head.getObjectId() != null) {
				suffix.append(abbreviate(head.getObjectId()));
			}
			suffix.append(']');
			if (verboseBranchMode && head.getObjectId() != null) {
				try (RevWalk walk = new RevWalk(repository)) {
					RevCommit commit = walk.parseCommit(head.getObjectId());
					suffix.append(' ').append(commit.getShortMessage());
				} catch (IOException ignored) {
					// Ignored
				}
			}
		} else {
			// Not a submodule
			String branch = Activator.getDefault().getRepositoryUtil()
					.getShortBranch(repository);
			if (branch == null) {
				return false;
			}
			suffix.append(" ["); //$NON-NLS-1$
			suffix.append(branch);

			BranchTrackingStatus trackingStatus = BranchTrackingStatus
					.of(repository, branch);
			if (trackingStatus != null && (trackingStatus.getAheadCount() != 0
					|| trackingStatus.getBehindCount() != 0)) {
				String formattedTrackingStatus = GitLabels
						.formatBranchTrackingStatus(trackingStatus);
				suffix.append(' ').append(formattedTrackingStatus);
			}

			RepositoryState repositoryState = repository.getRepositoryState();
			if (repositoryState != RepositoryState.SAFE) {
				suffix.append(" - ") //$NON-NLS-1$
						.append(repositoryState.getDescription());
			}
			suffix.append(']');
		}
		decoration.addSuffix(suffix.toString());
		return true;
	}

	private boolean decorateStash(StashedCommitNode node,
			IDecoration decoration) {
		RevCommit commit = node.getObject();
		decoration.addSuffix(
				" [" + abbreviate(commit) + "] " + commit.getShortMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		return true;
	}

	private boolean decorateSubmodules(@NonNull Repository repository,
			IDecoration decoration) throws IOException {
		if (haveSubmoduleChanges(repository)) {
			decoration.addPrefix("> "); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	private boolean decorateTag(TagNode node, IDecoration decoration) {
		if (verboseBranchMode && node.getCommitId() != null
				&& node.getCommitId().length() > 0) {
			decoration.addSuffix(" " + node.getCommitId().substring(0, 7) + ' ' //$NON-NLS-1$
					+ node.getCommitShortMessage());
			return true;
		}
		return false;
	}

	private RevCommit getLatestCommit(RepositoryTreeNode node) {
		Ref ref = (Ref) node.getObject();
		ObjectId id;
		if (ref.isSymbolic()) {
			id = ref.getLeaf().getObjectId();
		} else {
			id = ref.getObjectId();
		}
		if (id == null) {
			return null;
		}
		try (RevWalk walk = new RevWalk(node.getRepository())) {
			walk.setRetainBody(true);
			return walk.parseCommit(id);
		} catch (IOException ignored) {
			return null;
		}
	}

	private String abbreviate(final ObjectId id) {
		if (id != null) {
			return id.abbreviate(7).name();
		} else {
			return ObjectId.zeroId().abbreviate(7).name();
		}
	}

	private boolean haveSubmoduleChanges(@NonNull Repository repository)
			throws IOException {
		boolean hasChanges = false;
		try (SubmoduleWalk walk = SubmoduleWalk.forIndex(repository)) {
			while (!hasChanges && walk.next()) {
				Repository submodule = walk.getRepository();
				if (submodule != null) {
					Repository cached = org.eclipse.egit.core.Activator
							.getDefault().getRepositoryCache().lookupRepository(
									submodule.getDirectory().getAbsoluteFile());
					hasChanges = cached != null
							&& RepositoryUtil.hasChanges(cached);
					submodule.close();
				}
			}
		}
		return hasChanges;
	}

	@Override
	protected String getName() {
		return UIText.RepositoryTreeNodeDecorator_name;
	}
}
