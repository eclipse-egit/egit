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
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public enum RepositoryTreeNodeType {
	/**	 */
	REPO(UIIcons.REPOSITORY.createImage()), //
	/**	 */
	BRANCHES(UIIcons.BRANCHES.createImage()), //
	/** */
	REF(UIIcons.BRANCH.createImage()), //
	/** */
	LOCAL(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	BRANCHHIERARCHY(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	REMOTETRACKING(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	TAGS(UIIcons.TAGS.createImage()), //
	/** */
	ADDITIONALREFS(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	ADDITIONALREF(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FILE)), // TODO icon
	/** */
	TAG(UIIcons.TAG.createImage()), //
	/**	 */
	FOLDER(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/**	 */
	FILE(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FILE)), //
	/**	 */
	REMOTES(UIIcons.REMOTE_REPOSITORY.createImage()), //
	/**	 */
	REMOTE(UIIcons.REMOTE_SPEC.createImage()), //
	/**	 */
	FETCH(UIIcons.FETCH.createImage()), //
	/**	 */
	PUSH(UIIcons.PUSH.createImage()), //
	/** */
	SUBMODULES(UIIcons.SUBMODULES.createImage()),
	/** */
	STASH(UIIcons.STASH.createImage()),
	/** */
	STASHED_COMMIT(UIIcons.CHANGESET.createImage()),
	/**	 */
	WORKINGDIR(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	ERROR(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_ELCL_STOP)); // TODO icon?

	private final Image myImage;

	private RepositoryTreeNodeType(Image icon) {
		myImage = icon;

	}

	/**
	 * @return the icon for this type
	 */
	public Image getIcon() {
		return myImage;
	}
}
