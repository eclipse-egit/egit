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

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
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
	LOCALBRANCHES(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
			/** */
	BRANCHHIERARCHY(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	REMOTEBRANCHES(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	TAGS(UIIcons.TAGS.createImage()), //
	/** */
	SYMBOLICREFS(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	SYMBOLICREF(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FILE)), // TODO icon
	/** */
	TAG(UIIcons.TAG.createImage()), //
	/**	 */
	FILE(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FILE)), //
	/**	 */
	FOLDER(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/**	 */
	REMOTES(UIIcons.REMOTE_REPOSITORY.createImage()), //
	/**	 */
	REMOTE(UIIcons.REMOTE_SPEC.createImage()), //
	/**	 */
	FETCH(UIIcons.FETCH.createImage()), //
	/**	 */
	PUSH(UIIcons.PUSH.createImage()), //
	/**	 */
	WORKINGDIR(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER)), //
	/** */
	ERROR(PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_ELCL_STOP)) // TODO icon?

	;

	private final Image myImage;

	private RepositoryTreeNodeType(String iconName) {

		if (iconName != null) {
			myImage = Activator.getDefault().getImageRegistry().get(
					iconName);
		} else {
			myImage = null;
		}

	}

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
