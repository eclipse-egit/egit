/*******************************************************************************
 * Copyright (c) 2018, 2019 Thomas Wolf <thomas.wolf@paranor.ch>
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
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.groups.RepositoryGroup;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.lib.Constants;
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

	@Override
	public String getLabel(Object object) {
		RepositoryTreeNode<?> node = (RepositoryTreeNode) object;
		switch (node.getType()) {
		case REPO:
			Repository repository = (Repository) node.getObject();
			return GitLabels.getPlainShortLabel(repository);
		case REPOGROUP:
			return ((RepositoryGroup) node.getObject()).getName();
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
