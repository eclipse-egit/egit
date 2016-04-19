package org.eclipse.egit.gitflow.ui.internal.dialogs;

import static org.eclipse.egit.ui.internal.UIIcons.OVR_CHECKEDOUT;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

class DecoratedBranchLabelProvider extends ColumnLabelProvider {
	private Map<Image, Image> image2decoratedImage = new HashMap<Image, Image>();

	private ResourceManager resourceManager = new LocalResourceManager(
			JFaceResources.getResources());

	private Repository repository;

	private String prefix;

	public DecoratedBranchLabelProvider(Repository repository, String prefix) {
		this.repository = repository;
		this.prefix = prefix;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Ref) {
			String name = ((Ref) element).getName();
			return name.substring(prefix.length());
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Ref) {
			Image icon = RepositoryTreeNodeType.REF.getIcon();
			return decorateImage(icon, (Ref) element);
		}
		return super.getImage(element);
	}

	private Image decorateImage(final Image image, Ref node) {
		String refName = node.getName();

		String branchName;
		String compareString;

		try {
			branchName = repository.getFullBranch();
			compareString = refName;
		} catch (IOException e) {
			return image;
		}

		if (compareString.equals(branchName)) {
			return getDecoratedImage(image);
		}

		return image;

	}

	private Image getDecoratedImage(final Image image) {
		if (image2decoratedImage.containsKey(image)) {
			return image2decoratedImage.get(image);
		}

		CompositeImageDescriptor cd = new CompositeImageDescriptor() {

			@Override
			protected Point getSize() {
				Rectangle bounds = image.getBounds();
				return new Point(bounds.width, bounds.height);
			}

			@Override
			protected void drawCompositeImage(int width, int height) {
				drawImage(image.getImageData(), 0, 0);
				drawImage(OVR_CHECKEDOUT.getImageData(), 0, 0);

			}
		};
		Image decoratedImage = cd.createImage();
		image2decoratedImage.put(image, decoratedImage);
		return decoratedImage;
	}

	@Override
	public void dispose() {
		// dispose of our decorated images
		for (Image image : image2decoratedImage.values()) {
			image.dispose();
		}
		resourceManager.dispose();
		image2decoratedImage.clear();
		super.dispose();
	}
}