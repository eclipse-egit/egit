/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Chris Aniszczyk <caniszczyk@gmail.com> - added styled label support
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IStateListener;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.GitLabels;
import org.eclipse.egit.ui.internal.ResourcePropertyTester;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.AdditionalRefNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.egit.ui.internal.repository.tree.StashedCommitNode;
import org.eclipse.egit.ui.internal.repository.tree.SubmodulesNode;
import org.eclipse.egit.ui.internal.repository.tree.TagNode;
import org.eclipse.egit.ui.internal.repository.tree.command.ToggleBranchCommitCommand;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Label Provider for the Git Repositories View
 */
public class RepositoriesViewLabelProvider extends ColumnLabelProvider
		implements IStateListener, IStyledLabelProvider {

	/**
	 * A map of regular images to their decorated counterpart.
	 */
	private Map<Image, Image> decoratedImages = new HashMap<>();

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private Image annotatedTagImage = resourceManager
			.createImage(UIIcons.TAG_ANNOTATED);

	private Image gerritRepoImage = resourceManager
			.createImage(UIIcons.REPOSITORY_GERRIT);

	private final State verboseBranchModeState;

	private boolean verboseBranchMode = false;

	/**
	 * Constructs a repositories view label provider
	 */
	public RepositoriesViewLabelProvider() {
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
	public Image getImage(Object element) {
		RepositoryTreeNode node = (RepositoryTreeNode) element;
		RepositoryTreeNodeType type = node.getType();
		if (type == RepositoryTreeNodeType.TAG) {
			TagNode tagNode = (TagNode) node;
			if (tagNode.isAnnotated())
				return decorateImage(annotatedTagImage, element);
		} else if (type == RepositoryTreeNodeType.FILE) {
			Object object = node.getObject();
			if (object instanceof File) {
				ImageDescriptor descriptor = PlatformUI.getWorkbench()
						.getEditorRegistry()
						.getImageDescriptor(((File) object).getName());
				return decorateImage((Image) resourceManager.get(descriptor),
						element);
			}
		} else if (type == RepositoryTreeNodeType.REPO) {
			Object object = node.getObject();
			if (object instanceof Repository) {
				Repository r = (Repository) object;
				if (ResourcePropertyTester.hasGerritConfiguration(r))
					return gerritRepoImage;
			}
		}
		return decorateImage(node.getType().getIcon(), element);
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
		verboseBranchModeState.removeListener(this);
		// dispose of our decorated images
		for (Image image : decoratedImages.values()) {
			image.dispose();
		}
		resourceManager.dispose();
		decoratedImages.clear();
		super.dispose();
	}

	private Image decorateImage(final Image image, Object element) {

		RepositoryTreeNode node = (RepositoryTreeNode) element;
		switch (node.getType()) {

		case TAG:
			// fall through
		case ADDITIONALREF:
			// fall through
		case REF:
			// if the branch or tag is checked out,
			// we want to decorate the corresponding
			// node with a little check indicator
			String refName = ((Ref) node.getObject()).getName();
			Ref leaf = ((Ref) node.getObject()).getLeaf();

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
					TagNode tagNode = (TagNode) node;
					compareString = tagNode.getCommitId();
				} else if (refName.startsWith(Constants.R_REMOTES)) {
					// remote branch: HEAD would be on the commit id to which
					// the branch is pointing
					ObjectId id = node.getRepository().resolve(refName);
					if (id == null)
						return image;
					try (RevWalk rw = new RevWalk(node.getRepository())) {
						RevCommit commit = rw.parseCommit(id);
						compareString = commit.getId().name();
					}
				} else if (refName.equals(Constants.HEAD)) {
					return getDecoratedImage(image);
				} else {
					String leafname = leaf.getName();
					if (leafname.startsWith(Constants.R_REFS)
							&& leafname.equals(node.getRepository()
									.getFullBranch())) {
						return getDecoratedImage(image);
					}
					ObjectId objectId = leaf.getObjectId();
					if (objectId != null && objectId.equals(
							node.getRepository().resolve(Constants.HEAD))) {
						return getDecoratedImage(image);
					}
					// some other symbolic reference
					return image;
				}
			} catch (IOException e1) {
				return image;
			}

			if (compareString != null && compareString.equals(branchName)) {
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

	private RevCommit getLatestCommit(RepositoryTreeNode node) {
		Ref ref = (Ref) node.getObject();
		ObjectId id;
		if (ref.isSymbolic())
			id = ref.getLeaf().getObjectId();
		else
			id = ref.getObjectId();
		if (id == null)
			return null;
		try (RevWalk walk = new RevWalk(node.getRepository())) {
			walk.setRetainBody(true);
			return walk.parseCommit(id);
		} catch (IOException ignored) {
			return null;
		}
	}

	private String abbreviate(final ObjectId id) {
		if (id != null)
			return id.abbreviate(7).name();
		else
			return ObjectId.zeroId().abbreviate(7).name();
	}

	/**
	 * Get styled text for submodule repository node
	 *
	 * @param node
	 * @return styled string
	 */
	protected StyledString getStyledTextForSubmodule(RepositoryTreeNode node) {
		Repository repository = (Repository) node.getObject();
		if (repository == null) {
			return new StyledString();
		}
		StyledString string = GitLabels.getChangedPrefix(repository);

		String path = Repository.stripWorkDir(node.getParent().getRepository()
				.getWorkTree(), repository.getWorkTree());
		string.append(path);

		Ref head;
		try {
			head = repository.exactRef(Constants.HEAD);
		} catch (IOException e) {
			return string;
		}
		if (head != null) {
			string.append(' ');
			string.append('[', StyledString.DECORATIONS_STYLER);
			if (head.isSymbolic())
				string.append(
						Repository.shortenRefName(head.getLeaf().getName()),
						StyledString.DECORATIONS_STYLER);
			else if (head.getObjectId() != null)
				string.append(abbreviate(head.getObjectId()),
						StyledString.DECORATIONS_STYLER);
			string.append(']', StyledString.DECORATIONS_STYLER);
			if (verboseBranchMode && head.getObjectId() != null) {
				RevCommit commit;
				try (RevWalk walk = new RevWalk(repository)) {
					commit = walk.parseCommit(head.getObjectId());
					string.append(' ');
					string.append(commit.getShortMessage(),
							StyledString.QUALIFIER_STYLER);
				} catch (IOException ignored) {
					// Ignored
				}
			}
		}
		return string;
	}

	/**
	 * Get styled text for commit node
	 *
	 * @param node
	 * @return styled string
	 */
	protected StyledString getStyledTextForCommit(StashedCommitNode node) {
		StyledString string = new StyledString();
		RevCommit commit = node.getObject();
		string.append(MessageFormat.format("{0}@'{'{1}'}'", //$NON-NLS-1$
				Constants.STASH, Integer.valueOf(node.getIndex())));
		string.append(' ');
		string.append('[', StyledString.DECORATIONS_STYLER);
		string.append(abbreviate(commit), StyledString.DECORATIONS_STYLER);
		string.append(']', StyledString.DECORATIONS_STYLER);
		string.append(' ');
		string.append(commit.getShortMessage(), StyledString.QUALIFIER_STYLER);
		return string;
	}

	/**
	 * Gets the {@link StyledString} for a {@link SubmodulesNode}.
	 *
	 * @param node
	 *            to get the text for
	 * @return the {@link StyledString}
	 */
	protected StyledString getStyledTextForSubmodules(SubmodulesNode node) {
		String label = getSimpleText(node);
		if (label == null) {
			return new StyledString();
		}
		StyledString styled = new StyledString(label);
		Repository repository = node.getRepository();
		if (repository != null) {
			boolean hasChanges = false;
			try (SubmoduleWalk walk = SubmoduleWalk.forIndex(repository)) {
				while (!hasChanges && walk.next()) {
					Repository submodule = walk.getRepository();
					if (submodule != null) {
						Repository cached = org.eclipse.egit.core.Activator
								.getDefault().getRepositoryCache()
								.lookupRepository(submodule.getDirectory()
										.getAbsoluteFile());
						hasChanges = cached != null
								&& RepositoryUtil.hasChanges(cached);
						submodule.close();
					}
				}
			} catch (IOException e) {
				hasChanges = false;
			}
			if (hasChanges) {
				StyledString prefixed = new StyledString();
				prefixed.append('>', StyledString.DECORATIONS_STYLER);
				prefixed.append(' ').append(styled);
				return prefixed;
			}
		}
		return styled;
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (!(element instanceof RepositoryTreeNode))
			return null;

		RepositoryTreeNode node = (RepositoryTreeNode) element;

		switch (node.getType()) {
		case REPO:
			if (node.getParent() != null
					&& node.getParent().getType() == RepositoryTreeNodeType.SUBMODULES)
				return getStyledTextForSubmodule(node);
			return GitLabels.getStyledLabelExtendedSafe(node.getObject());
		case ADDITIONALREF:
			Ref ref = (Ref) node.getObject();
			// shorten the name
			StyledString refName = new StyledString(
					Repository.shortenRefName(ref.getName()));

			ObjectId refId;
			if (ref.isSymbolic()) {
				refName.append(' ');
				refName.append('[', StyledString.DECORATIONS_STYLER);
				refName.append(ref.getLeaf().getName(),
						StyledString.DECORATIONS_STYLER);
				refName.append(']', StyledString.DECORATIONS_STYLER);
				refId = ref.getLeaf().getObjectId();
			} else
				refId = ref.getObjectId();

			refName.append(' ');
			RevCommit commit = getLatestCommit(node);
			if (commit != null)
				refName.append(abbreviate(commit),
						StyledString.QUALIFIER_STYLER)
						.append(' ')
						.append(commit.getShortMessage(),
								StyledString.QUALIFIER_STYLER);
			else
				refName.append(abbreviate(refId),
						StyledString.QUALIFIER_STYLER);
			return refName;
		case WORKINGDIR:
			StyledString dirString = new StyledString(
					UIText.RepositoriesView_WorkingDir_treenode);
			dirString.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			dirString.append(node.getRepository().getWorkTree()
					.getAbsolutePath(), StyledString.QUALIFIER_STYLER);
			return dirString;

		case REF:
			StyledString styled = null;
			String nodeText = getSimpleText(node);
			if (nodeText != null) {
				styled = new StyledString(nodeText);
				if (verboseBranchMode) {
					RevCommit latest = getLatestCommit(node);
					if (latest != null)
						styled.append(' ')
								.append(abbreviate(latest),
										StyledString.QUALIFIER_STYLER)
								.append(' ')
								.append(latest.getShortMessage(),
										StyledString.QUALIFIER_STYLER);
				}
			}
			return styled;
		case TAG:
			return getStyledTextForTag((TagNode) node);
		case STASHED_COMMIT:
			return getStyledTextForCommit((StashedCommitNode) node);
		case SUBMODULES:
			return getStyledTextForSubmodules((SubmodulesNode) node);
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
		case LOCAL:
			// fall through
		case REMOTETRACKING:
			// fall through
		case BRANCHHIERARCHY:
			// fall through
		case TAGS:
			// fall through;
		case ADDITIONALREFS:
			// fall through
		case REMOTES:
			// fall through
		case REMOTE:
			// fall through
		case STASH:
			// fall through
		case ERROR: {
			String label = getSimpleText(node);
			if (label != null)
				return new StyledString(label);
		}

		}

		return null;

	}

	private StyledString getStyledTextForTag(TagNode node) {
		String tagText = getSimpleText(node);
		if (tagText != null) {
			StyledString styled = new StyledString(tagText);
			if (verboseBranchMode) {
				if (node.getCommitId() != null
						&& node.getCommitId().length() > 0)
					styled.append(' ')
							.append(node.getCommitId().substring(0, 7),
									StyledString.QUALIFIER_STYLER)
							.append(' ')
							.append(node.getCommitShortMessage(),
									StyledString.QUALIFIER_STYLER);
			}
			return styled;
		} else {
			return null;
		}
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof AdditionalRefNode) {
			AdditionalRefNode additionalRefNode = (AdditionalRefNode) element;
			Ref ref = additionalRefNode.getObject();
			return GitLabels.getRefDescription(ref);
		}
		return null;
	}

	private String getSimpleText(RepositoryTreeNode node) {
		switch (node.getType()) {
		case REPO:
			Repository repository = (Repository) node.getObject();
			return GitLabels.getPlainShortLabel(repository);
		case FILE:
			// fall through
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
			return MessageFormat.format(
					"{0}@'{'{1}'}'", //$NON-NLS-1$
					Constants.STASH,
					Integer.valueOf(((StashedCommitNode) node).getIndex()));
		case REF:
			// fall through
		case TAG: {
			Ref ref = (Ref) node.getObject();
			// shorten the name
			String refName = Repository.shortenRefName(ref.getName());
			if (node.getParent().getType() == RepositoryTreeNodeType.BRANCHHIERARCHY) {
				int index = refName.lastIndexOf('/');
				refName = refName.substring(index + 1);
			}
			return refName;
		}
		case ADDITIONALREF: {
			Ref ref = (Ref) node.getObject();
			// shorten the name
			String refName = Repository.shortenRefName(ref.getName());
			if (ref.isSymbolic()) {
				refName = refName
						+ " - " //$NON-NLS-1$
						+ ref.getLeaf().getName()
						+ " - " + ObjectId.toString(ref.getLeaf().getObjectId()); //$NON-NLS-1$
			} else {
				refName = refName + " - " //$NON-NLS-1$
						+ ObjectId.toString(ref.getObjectId());
			}
			return refName;
		}
		case WORKINGDIR:
			return UIText.RepositoriesView_WorkingDir_treenode + " - " //$NON-NLS-1$
					+ node.getRepository().getWorkTree().getAbsolutePath();
		case REMOTE:
			// fall through
		case PUSH:
			// fall through
		case FETCH:
			// fall through
		case ERROR:
			return (String) node.getObject();

		}
		return null;
	}

	/**
	 * @see org.eclipse.core.commands.IStateListener#handleStateChange(org.eclipse.core.commands.State,
	 *      java.lang.Object)
	 */
	@Override
	public void handleStateChange(State state, Object oldValue) {
		try {
			this.verboseBranchMode = ((Boolean) state.getValue())
					.booleanValue();
		} catch (Exception e) {
			Activator.logError(e.getMessage(), e);
		}
	}

}
