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

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.internal.DecorationOverlayDescriptor;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.decorators.DecoratorRepositoryStateCache;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * A {@link WorkbenchAdapter} for {@link RepositoryTreeNode}s, providing images
 * and labels. The adapter does <em>not</em> deliver children. That
 * functionality is left to the content provider since it may depend on global
 * state.
 */
public class RepositoryTreeNodeWorkbenchAdapter extends WorkbenchAdapter {

	/**
	 * The singleton instance of the {@link RepositoryTreeNodeWorkbenchAdapter}.
	 */
	public static final RepositoryTreeNodeWorkbenchAdapter INSTANCE = new RepositoryTreeNodeWorkbenchAdapter();

	private RepositoryTreeNodeWorkbenchAdapter() {
		// Prevent creations outside of this class
	}

	@Override
	public Object getParent(Object object) {
		return ((RepositoryTreeNode) object).getParent();
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object == null) {
			return null;
		}
		RepositoryTreeNode<?> node = (RepositoryTreeNode) object;
		ImageDescriptor base = getBaseImageDescriptor(node);
		if (base == null) {
			return null;
		}
		// We have to decorate here: if we let an asynchronous lightweight
		// decorator do it, image decorations may flicker in the
		// repositories view and elsewhere where we'd refresh viewers.
		return decorateImageDescriptor(base, node);
	}

	private ImageDescriptor getBaseImageDescriptor(
			@NonNull RepositoryTreeNode<?> node) {
		switch (node.getType()) {
		case FILE: {
			Object item = node.getObject();
			if (item instanceof File) {
				return PlatformUI.getWorkbench().getEditorRegistry()
						.getImageDescriptor(((File) item).getName());
			}
			break;
		}
		case REPO: {
			Object item = node.getObject();
			if (item instanceof Repository && ResourcePropertyTester
					.hasGerritConfiguration((Repository) item)) {
				return UIIcons.REPOSITORY_GERRIT;
			}
			break;
		}
		case TAG:
			if (((TagNode) node).isAnnotated()) {
				return UIIcons.TAG_ANNOTATED;
			}
			break;
		default:
			break;
		}
		return node.getType().getIcon();
	}

	private ImageDescriptor decorateImageDescriptor(
			@NonNull ImageDescriptor base,
			@NonNull RepositoryTreeNode<?> node) {
		switch (node.getType()) {
		case TAG:
		case ADDITIONALREF:
		case REF:
			// if the branch or tag is checked out,
			// we want to decorate the corresponding
			// node with a little check indicator
			String refName = ((Ref) node.getObject()).getName();
			Ref leaf = ((Ref) node.getObject()).getLeaf();

			String compareString = null;
			Repository repository = node.getRepository();
			String branchName = DecoratorRepositoryStateCache.INSTANCE
					.getFullBranchName(repository);
			if (branchName == null) {
				return base;
			}
			if (refName.startsWith(Constants.R_HEADS)) {
				// local branch: HEAD would be on the branch
				compareString = refName;
			} else if (refName.startsWith(Constants.R_TAGS)) {
				// tag: HEAD would be on the commit id to which the tag is
				// pointing
				TagNode tagNode = (TagNode) node;
				compareString = tagNode.getCommitId();
			} else if (refName.startsWith(Constants.R_REMOTES)) {
				// remote branch: branch name is object id in detached HEAD
				// state
				ObjectId objectId = leaf.getObjectId();
				if (objectId != null) {
					String leafName = objectId.getName();
					if (leafName.equals(branchName)) {
						return new DecorationOverlayDescriptor(base,
								UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
					}
				}
			} else if (refName.equals(Constants.HEAD)) {
				return new DecorationOverlayDescriptor(base,
						UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
			} else {
				String leafname = leaf.getName();
				if (leafname.startsWith(Constants.R_REFS)
						&& leafname.equals(branchName)) {
					return new DecorationOverlayDescriptor(base,
							UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
				}
				ObjectId objectId = leaf.getObjectId();
				if (objectId != null && objectId
						.equals(DecoratorRepositoryStateCache.INSTANCE
								.getHead(repository))) {
					return new DecorationOverlayDescriptor(base,
							UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
				}
				// some other symbolic reference
				return base;
			}

			if (compareString != null && compareString.equals(branchName)) {
				return new DecorationOverlayDescriptor(base,
						UIIcons.OVR_CHECKEDOUT, IDecoration.TOP_LEFT);
			}

			break;
		default:
			break;
		}
		return base;
	}

	@Override
	public String getLabel(Object object) {
		RepositoryTreeNode<?> node = (RepositoryTreeNode) object;
		switch (node.getType()) {
		case REPO:
			Repository repository = (Repository) node.getObject();
			return GitLabels.getPlainShortLabel(repository);
		case FILE:
		case FOLDER:
			return ((File) node.getObject()).getName();
		case BRANCHES:
			return UIText.RepositoriesView_Branches_Nodetext;
		case LOCAL:
			return UIText.RepositoriesViewLabelProvider_LocalNodetext;
		case REMOTETRACKING:
			return UIText.RepositoriesViewLabelProvider_RemoteTrackingNodetext;
		case BRANCHHIERARCHY:
			IPath fullPath = (IPath) node.getObject();
			return fullPath.lastSegment();
		case TAGS:
			return UIText.RepositoriesViewLabelProvider_TagsNodeText;
		case ADDITIONALREFS:
			return UIText.RepositoriesViewLabelProvider_SymbolicRefNodeText;
		case REMOTES:
			return UIText.RepositoriesView_RemotesNodeText;
		case SUBMODULES:
			return UIText.RepositoriesViewLabelProvider_SubmodulesNodeText;
		case STASH:
			return UIText.RepositoriesViewLabelProvider_StashNodeText;
		case STASHED_COMMIT:
			return MessageFormat.format("{0}@'{'{1}'}'", //$NON-NLS-1$
					Constants.STASH,
					Integer.valueOf(((StashedCommitNode) node).getIndex()));
		case REF:
		case TAG: {
			Ref ref = (Ref) node.getObject();
			// shorten the name
			String refName = Repository.shortenRefName(ref.getName());
			if (node.getParent()
					.getType() == RepositoryTreeNodeType.BRANCHHIERARCHY) {
				refName = refName.substring(refName.lastIndexOf('/') + 1);
			}
			return refName;
		}
		case ADDITIONALREF: {
			Ref ref = (Ref) node.getObject();
			return Repository.shortenRefName(ref.getName());
		}
		case WORKINGDIR:
			return UIText.RepositoriesView_WorkingDir_treenode;
		default:
			return (String) node.getObject();
		}
	}
}
