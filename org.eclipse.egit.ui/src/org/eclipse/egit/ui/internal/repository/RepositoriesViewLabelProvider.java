/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - added styled label support
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Label Provider for the Git Repositories View
 */
public class RepositoriesViewLabelProvider extends LabelProvider implements IStyledLabelProvider {

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

		return getSimpleText(node);
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
					ObjectId id = node.getRepository().resolve(refName);
					if (id == null)
						return image;
					RevWalk rw = new RevWalk(node.getRepository());
					RevTag tag = rw.parseTag(id);
					compareString = tag.getObject().name();

				} else if (refName.startsWith(Constants.R_REMOTES)) {
					// remote branch: HEAD would be on the commit id to which
					// the branch is pointing
					ObjectId id = node.getRepository().resolve(refName);
					if (id == null)
						return image;
					RevWalk rw = new RevWalk(node.getRepository());
					RevCommit commit = rw.parseCommit(id);
					compareString = commit.getId().name();
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

	public StyledString getStyledText(Object element) {
		if (!(element instanceof RepositoryTreeNode))
			return null;

		RepositoryTreeNode node = (RepositoryTreeNode) element;

		String label = getSimpleText(node);
		if (label == null)
			return new StyledString(element.toString());

		StyledString text = new StyledString(label);

		try {
			switch (node.getType()) {
			case REPO:
				Repository repository = (Repository) node.getObject();
				File directory = repository.getDirectory();
				StyledString string = new StyledString(directory.getParentFile().getName());
				string.append(" - " + directory.getAbsolutePath(), StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				string.append(" [" + repository.getBranch() + "]", StyledString.DECORATIONS_STYLER);  //$NON-NLS-1$//$NON-NLS-2$
				return string;
			case SYMBOLICREF:
				Ref ref = (Ref) node.getObject();
				// shorten the name
				StyledString refName = new StyledString(node.getRepository().shortenRefName(ref.getName()));
				if (ref.isSymbolic()) {
					refName.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
					refName.append(ref.getLeaf().getName(), StyledString.QUALIFIER_STYLER);
				}
				return refName;
			case WORKINGDIR:
				StyledString dirString = new StyledString(
						UIText.RepositoriesView_WorkingDir_treenode);
				dirString.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				if (node.getRepository().isBare()) {
					dirString
							.append(
									UIText.RepositoriesViewLabelProvider_BareRepositoryMessage,
									StyledString.QUALIFIER_STYLER);
				} else {
					dirString.append(node.getRepository().getWorkTree()
							.getAbsolutePath(), StyledString.QUALIFIER_STYLER);
				}
				return dirString;
			case PUSH:
				// fall through
			case FETCH:
				// fall through
			case FILE:
				// fall through
			case FOLDER:
				// fall through
			case BRANCHES:
				// fall through
			case LOCALBRANCHES:
				// fall through
			case REMOTETRACKINGBRANCHES:
				// fall through
			case BRANCHHIERARCHY:
				// fall through
			case TAGS:
				// fall through;
			case SYMBOLICREFS:
				// fall through
			case REMOTES:
				// fall through
			case REMOTE:
				// fall through
			case ERROR:
				// fall through
			case REF:
				// fall through
			case TAG:
				// fall through

			}
		} catch (IOException e) {
			Activator.logError(e.getMessage(), e);
		}

		return text;

	}

	private String getSimpleText(RepositoryTreeNode node) {
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
		case REMOTETRACKINGBRANCHES:
			return UIText.RepositoriesViewLabelProvider_RemoteTrackingBranchesNodetext;
		case BRANCHHIERARCHY:
			IPath fullPath = (IPath) node.getObject();
			return fullPath.lastSegment();
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
			if (node.getParent().getType() == RepositoryTreeNodeType.BRANCHHIERARCHY) {
				int index = refName.lastIndexOf('/');
				refName = refName.substring(index + 1);
			}
			return refName;
		case WORKINGDIR:
			if (node.getRepository().isBare())
				return UIText.RepositoriesView_WorkingDir_treenode
						+ " - " //$NON-NLS-1$
						+ UIText.RepositoriesViewLabelProvider_BareRepositoryMessage;
			else
				return UIText.RepositoriesView_WorkingDir_treenode + " - " //$NON-NLS-1$
						+ node.getRepository().getWorkTree().getAbsolutePath();
		case PUSH:
			// fall through
		case FETCH:
			return (String) node.getObject();

		}
		return null;
	}

}
