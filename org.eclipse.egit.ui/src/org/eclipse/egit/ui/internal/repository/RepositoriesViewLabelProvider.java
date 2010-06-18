/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Label Provider for the Git Repositories View
 */
public class RepositoriesViewLabelProvider extends LabelProvider {

	/**
	 * A map of regular images to their decorated counterpart.
	 */
	private Map<Image, Image> decoratedImages = new HashMap<Image, Image>();

	@Override
	public Image getImage(Object element) {
		return decorateImage(
				((RepositoryTreeNode) element).getType().getIcon(), element);
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof RepositoryTreeNode))
			return null;

		RepositoryTreeNode node = (RepositoryTreeNode) element;
		switch (node.getType()) {
		case REPO:
			File directory = ((Repository) node.getObject()).getDirectory();
			StringBuilder sb = new StringBuilder();
			sb.append(directory.getParentFile().getName());
			sb.append(" - "); //$NON-NLS-1$
			sb.append(directory.getAbsolutePath());
			return sb.toString();
		case FILE:
			// fall through
		case FOLDER:
			return ((File) node.getObject()).getName();
		case BRANCHES:
			return UIText.RepositoriesView_Branches_Nodetext;
		case LOCALBRANCHES:
			return UIText.RepositoriesViewLabelProvider_LocalBranchesNodetext;
		case REMOTEBRANCHES:
			return UIText.RepositoriesViewLabelProvider_RemoteBrancheNodetext;
		case TAGS:
			return UIText.RepositoriesViewLabelProvider_TagsNodeText;
		case SYMBOLICREFS:
			return UIText.RepositoriesViewLabelProvider_SymbolicRefNodeText;
		case REMOTES:
			return UIText.RepositoriesView_RemotesNodeText;
		case REMOTE:
			// fall through
		case ERROR:
			return (String) node.getObject();
		case REF:
			// fall through
		case TAG:
			// fall through
		case SYMBOLICREF:
			Ref ref = (Ref) node.getObject();
			// shorten the name
			String refName = node.getRepository().shortenRefName(ref.getName());
			if (ref.isSymbolic()) {
				refName = refName + " - " //$NON-NLS-1$
						+ ref.getLeaf().getName();
			}
			return refName;
		case WORKINGDIR:
			if (node.getRepository().getConfig().getBoolean(
					"core", "bare", false)) //$NON-NLS-1$ //$NON-NLS-2$
				return UIText.RepositoriesView_WorkingDir_treenode
						+ " - " //$NON-NLS-1$
						+ UIText.RepositoriesViewLabelProvider_BareRepositoryMessage;
			else
				return UIText.RepositoriesView_WorkingDir_treenode + " - " //$NON-NLS-1$
						+ node.getRepository().getWorkDir().getAbsolutePath();
		case PUSH:
			// fall through
		case FETCH:
			return (String) node.getObject();

		}

		return null;
	}

	@Override
	public void dispose() {
		// dispose of our decorated images
		for (Image image : decoratedImages.values()) {
			image.dispose();
		}
		decoratedImages.clear();
		super.dispose();
	}

	private Image decorateImage(final Image image, Object element) {

		RepositoryTreeNode node = (RepositoryTreeNode) element;
		switch (node.getType()) {

		case TAG:
			// fall through
		case REF:
			// if the branch or tag is checked out,
			// we want to decorate the corresponding
			// node with a little check indicator
			String refName = ((Ref) node.getObject()).getName();

			String branchName;
			String compareString;

			try {
				branchName = node.getRepository().getFullBranch();
				if (branchName == null)
					return image;
				if (refName.startsWith(Constants.R_HEADS)) {
					// local branch: HEAD would be on the branch
					compareString = refName;
				} else if (refName.startsWith(Constants.R_TAGS)) {
					// tag: HEAD would be on the commit id to which the tag is
					// pointing
					compareString = node.getRepository().mapTag(refName)
							.getObjId().getName();
				} else if (refName.startsWith(Constants.R_REMOTES)) {
					// remote branch: HEAD would be on the commit id to which
					// the branch is pointing
					compareString = node.getRepository().mapCommit(refName)
							.getCommitId().getName();
				} else {
					// some other symbolic reference
					return image;
				}
			} catch (IOException e1) {
				return image;
			}

			if (compareString.equals(branchName)) {
				return getDecoratedImage(image);
			}

			return image;

		default:
			return image;
		}
	}

	private Image getDecoratedImage(final Image image) {
		// check if we have a decorated image yet or not
		Image decoratedImage = decoratedImages.get(image);
		if (decoratedImage == null) {
			// create one
			CompositeImageDescriptor cd = new CompositeImageDescriptor() {

				@Override
				protected Point getSize() {
					Rectangle bounds = image.getBounds();
					return new Point(bounds.width, bounds.height);
				}

				@Override
				protected void drawCompositeImage(int width, int height) {
					drawImage(image.getImageData(), 0, 0);
					drawImage(UIIcons.OVR_CHECKEDOUT.getImageData(), 0, 0);

				}
			};
			decoratedImage = cd.createImage();
			// store it
			decoratedImages.put(image, decoratedImage);
		}
		return decoratedImage;
	}

}
