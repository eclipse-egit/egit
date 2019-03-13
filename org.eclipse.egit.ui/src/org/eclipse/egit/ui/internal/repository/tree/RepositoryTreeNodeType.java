/*******************************************************************************
 * Copyright (c) 2010, 2019 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Thomas Wolf <thomas.wolf@paranor.ch> - change from Image to ImageDescriptor
 *    Alexander Nittka <alex@nittka.de> - Bug 545123
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public enum RepositoryTreeNodeType {
	/**	 */
	REPO(UIIcons.REPOSITORY), //
	/**	 */
	REPOGROUP(PlatformUI.getWorkbench().getSharedImages()
			.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER)), //
	/**	 */
	BRANCHES(UIIcons.BRANCHES), //
	/** */
	REF(UIIcons.BRANCH), //
	/** */
	LOCAL(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	BRANCHHIERARCHY(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	REMOTETRACKING(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	TAGS(UIIcons.TAGS), //
	/** */
	ADDITIONALREFS(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	ADDITIONALREF(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FILE)), // TODO icon
	/** */
	TAG(UIIcons.TAG), //
	/**	 */
	FOLDER(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/**	 */
	FILE(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FILE)), //
	/**	 */
	REMOTES(UIIcons.REMOTE_REPOSITORY), //
	/**	 */
	REMOTE(UIIcons.REMOTE_SPEC), //
	/**	 */
	FETCH(UIIcons.FETCH), //
	/**	 */
	PUSH(UIIcons.PUSH), //
	/** */
	SUBMODULES(UIIcons.SUBMODULES),
	/** */
	STASH(UIIcons.STASH),
	/** */
	STASHED_COMMIT(UIIcons.CHANGESET),
	/**	 */
	WORKINGDIR(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	ERROR(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_ELCL_STOP)); // TODO icon?

	private final ImageDescriptor myImage;

	private RepositoryTreeNodeType(ImageDescriptor icon) {
		myImage = icon;

	}

	/**
	 * @return the {@link ImageDescriptor} for the icon for this type
	 */
	public ImageDescriptor getIcon() {
		return myImage;
	}
}
