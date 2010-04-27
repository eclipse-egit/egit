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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
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

	// private DefaultInformationControl infoControl;

	/**
	 *
	 * @param viewer
	 */
	RepositoriesViewLabelProvider(final TreeViewer viewer) {

		viewer.setLabelProvider(this);
		// we could implement some hover here to display additional information
		// viewer.getTree().addMouseTrackListener(new MouseTrackAdapter() {
		//
		// @Override
		// public void mouseHover(MouseEvent e) {

		// Point eventPoint = new Point(e.x, e.y);
		//
		// TreeItem item = viewer.getTree().getItem(eventPoint);
		// if (item != null) {
		//
		// RepositoryTreeNode node = (RepositoryTreeNode) item
		// .getData();
		// String text = node.getRepository().getDirectory()
		// .getCanonicalPath();
		//
		// final ViewerCell cell = viewer.getCell(eventPoint);
		//
		// if (infoControl != null && infoControl.isVisible()) {
		// infoControl.setVisible(false);
		// }
		//
		// GC testGc = new GC(cell.getControl());
		// final Point textExtent = testGc.textExtent(text);
		// testGc.dispose();
		//
		// if (infoControl == null || !infoControl.isVisible()) {
		//
		// IInformationPresenter ips = new IInformationPresenter() {
		//
		// public String updatePresentation(
		// Display display, String hoverInfo,
		// TextPresentation presentation,
		// int maxWidth, int maxHeight) {
		// return hoverInfo;
		// }
		//
		// };
		//
		// infoControl = new DefaultInformationControl(Display
		// .getCurrent().getActiveShell().getShell(),
		// ips) {
		//
		// @Override
		// public void setInformation(String content) {
		// super.setInformation(content);
		// super.setSize(textExtent.x, textExtent.y);
		// }
		//
		// };
		// }
		//
		// Point dispPoint = viewer.getControl().toDisplay(
		// eventPoint);
		//
		// infoControl.setLocation(dispPoint);
		//
		// // the default info provider works better with \r ...
		// infoControl.setInformation(text);
		//
		// final MouseMoveListener moveListener = new
		// MouseMoveListener() {
		//
		// public void mouseMove(MouseEvent evt) {
		// infoControl.setVisible(false);
		// cell.getControl().removeMouseMoveListener(this);
		//
		// }
		// };
		//
		// cell.getControl().addMouseMoveListener(moveListener);
		//
		// infoControl.setVisible(true);
		//
		// }
		//
		// }
		//
		// });

	}

	@Override
	public Image getImage(Object element) {
		return decorateImage(
				((RepositoryTreeNode) element).getType().getIcon(), element);
	}

	@Override
	public String getText(Object element) {

		RepositoryTreeNode node = (RepositoryTreeNode) element;
		switch (node.getType()) {
		case REPO:
			File directory = ((Repository) node.getObject()).getDirectory();
			return (directory.getParentFile().getName() + " - " + directory //$NON-NLS-1$
					.getAbsolutePath());
		case FILE:
		case FOLDER: // fall through
			return ((File) node.getObject()).getName();
		case BRANCHES:
			return UIText.RepositoriesView_Branches_Nodetext;
		case REMOTES:
			return UIText.RepositoriesView_RemotesNodeText;
		case REMOTE:
			return (String) node.getObject();
		case PROJECTS:
			return UIText.RepositoriesView_ExistingProjects_Nodetext;
		case REF:
			Ref ref = (Ref) node.getObject();
			// shorten the name
			String refName = node.getRepository().shortenRefName(ref.getName());
			if (ref.isSymbolic()) {
				refName = refName
						+ " - " //$NON-NLS-1$
						+ node.getRepository().shortenRefName(
								ref.getLeaf().getName());
			}
			return refName;
		case PROJ:

			File file = (File) node.getObject();
			return file.getName();

		case WORKINGDIR:

			return UIText.RepositoriesView_WorkingDir_treenode
					+ " - " //$NON-NLS-1$
					+ node.getRepository().getWorkDir().getAbsolutePath();

		case PUSH: // fall through
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

		case REF:
			Ref ref = (Ref) node.getObject();
			// shorten the name
			String refName = node.getRepository().shortenRefName(ref.getName());
			try {
				String branch = node.getBranch();
				if (refName.equals(branch)) {
					return getDecoratedImage(image);
				}
			} catch (IOException e1) {
				// simply ignore here
			}
			return image;

		case PROJ:

			File file = (File) node.getObject();

			for (IProject proj : ResourcesPlugin.getWorkspace().getRoot()
					.getProjects()) {
				if (proj.getLocation().equals(new Path(file.getAbsolutePath()))) {
					return getDecoratedImage(image);
				}
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
					drawImage(UIIcons.OVR_CHECKEDOUT.getImageData(), 0,
							0);

				}
			};
			decoratedImage = cd.createImage();
			// store it
			decoratedImages.put(image, decoratedImage);
		}
		return decoratedImage;
	}

}
