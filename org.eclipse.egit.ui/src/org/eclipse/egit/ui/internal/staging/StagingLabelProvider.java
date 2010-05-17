/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

class StagingLabelProvider extends LabelProvider implements
		IStyledLabelProvider {

	private Image repositoryImage;

	private Image stagedImage;

	private Image modifiedImage;

	private Image untrackedImage;

	private Image removedImage;

	StagingLabelProvider() {
		repositoryImage = UIIcons.REPOSITORY.createImage();
		stagedImage = createdDecoratedImage(UIIcons.OVR_STAGED);
		modifiedImage = createdDecoratedImage(UIIcons.OVR_MODIFIED);
		untrackedImage = createdDecoratedImage(UIIcons.OVR_UNTRACKED);
		removedImage = createdDecoratedImage(UIIcons.OVR_STAGED_REMOVE);
	}

	private Image createdDecoratedImage(final ImageDescriptor decoration) {
		final Image fileImage = PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJ_FILE);
		CompositeImageDescriptor cd = new CompositeImageDescriptor() {

			@Override
			protected Point getSize() {
				Rectangle bounds = fileImage.getBounds();
				return new Point(bounds.width, bounds.height);
			}

			@Override
			protected void drawCompositeImage(int width, int height) {
				drawImage(fileImage.getImageData(), 0, 0);
				drawImage(decoration.getImageData(), 8, 8);
			}
		};
		return cd.createImage();
	}

	@Override
	public void dispose() {
		repositoryImage.dispose();
		stagedImage.dispose();
		modifiedImage.dispose();
		untrackedImage.dispose();
		removedImage.dispose();
		super.dispose();
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Repository) {
			return ((Repository) element).getDirectory().getParentFile()
					.getName();
		} else if (element instanceof StatusNode) {
			return ((StatusNode) element).getLabel();
		} else if (element instanceof ResourceNode) {
			return element.toString();
		} else if (element instanceof IFile) {
			return ((IFile) element).getName();
		}
		return super.getText(element);
	}

	public StyledString getStyledText(Object element) {
		return new StyledString(getText(element));
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IFile) {
			if (((IFile) element).exists()) {
				return PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_OBJ_FILE);
			}
			return removedImage;
		} else if (element instanceof ResourceNode) {
			IResource resource = ((ResourceNode) element).getResource();
			if (resource.getType() == IResource.PROJECT) {
				return PlatformUI.getWorkbench().getSharedImages()
						.getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
			}
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if (element instanceof Repository) {
			return repositoryImage;
		} else if (element instanceof StagedNode) {
			return stagedImage;
		} else if (element instanceof ModifiedNode) {
			return modifiedImage;
		} else if (element instanceof UntrackedNode) {
			return untrackedImage;
		}
		return null;
	}
}
