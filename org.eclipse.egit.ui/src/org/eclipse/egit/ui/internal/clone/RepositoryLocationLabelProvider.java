/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

class RepositoryLocationLabelProvider extends LabelProvider {

	private Image repoImage = UIIcons.CLONEGIT.createImage();

	private List<Image> images = new ArrayList<>();

	@Override
	public String getText(Object element) {
		if (element instanceof CloneSourceProvider)
			return ((CloneSourceProvider) element).getLabel();
		else if (element instanceof RepositoryServerInfo)
			return ((RepositoryServerInfo) element).getLabel();
		return null;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof CloneSourceProvider) {
			Image image = ((CloneSourceProvider) element).getImage().createImage();
			images.add(image);
			return image;
		}
		else if (element instanceof RepositoryServerInfo)
			return repoImage;
		return null;
	}

	@Override
	public void dispose() {
		repoImage.dispose();
		for (Image image  : images)
			image.dispose();
	}

}
