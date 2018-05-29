/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.Arrays;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * A <code>DecorationOverlayIcon</code> is an image descriptor that can be used
 * to overlay decoration images on to the 4 corner quadrants of a base image
 * descriptor. The four quadrants are {@link IDecoration#TOP_LEFT},
 * {@link IDecoration#TOP_RIGHT}, {@link IDecoration#BOTTOM_LEFT} and
 * {@link IDecoration#BOTTOM_RIGHT}. Additionally, the overlay can be used to
 * provide an underlay corresponding to {@link IDecoration#UNDERLAY}.
 *
 * This class is modeled after {@link DecorationOverlayIcon} but supports using
 * a base image descriptor instead of a base image.
 */
public class DecorationOverlayDescriptor extends CompositeImageDescriptor {

	// the base image
	private ImageDescriptor base;

	// the overlay images
	private ImageDescriptor[] overlays;

	// the size
	private Point size;

	/**
	 * Create the decoration overlay for the base image descriptor using the
	 * array of provided overlays. The indices of the array correspond to the
	 * values of the 5 overlay constants defined on {@link IDecoration} (
	 * {@link IDecoration#TOP_LEFT}, {@link IDecoration#TOP_RIGHT},
	 * {@link IDecoration#BOTTOM_LEFT}, {@link IDecoration#BOTTOM_RIGHT} and
	 * {@link IDecoration#UNDERLAY}).
	 *
	 * @param baseImage
	 *            the base image
	 * @param overlaysArray
	 *            the overlay images
	 * @param sizeValue
	 *            the size of the resulting image
	 */
	public DecorationOverlayDescriptor(ImageDescriptor baseImage,
			ImageDescriptor[] overlaysArray, Point sizeValue) {
		this.base = baseImage;
		this.overlays = overlaysArray;
		this.size = sizeValue;
	}

	/**
	 * Create the decoration overlay for the base image descriptor using the
	 * array of provided overlays. The indices of the array correspond to the
	 * values of the 5 overlay constants defined on {@link IDecoration} (
	 * {@link IDecoration#TOP_LEFT}, {@link IDecoration#TOP_RIGHT},
	 * {@link IDecoration#BOTTOM_LEFT}, {@link IDecoration#BOTTOM_RIGHT} and
	 * {@link IDecoration#UNDERLAY}).
	 *
	 * @param baseImage
	 *            the base image
	 * @param overlaysArray
	 *            the overlay images
	 */
	public DecorationOverlayDescriptor(ImageDescriptor baseImage,
			ImageDescriptor[] overlaysArray) {
		this(baseImage, overlaysArray, UIUtils.getSize(baseImage));
	}

	/**
	 * Create a decoration overlay icon that will place the given overlay icon
	 * in the given quadrant of the base image descriptor.
	 *
	 * @param baseImage
	 *            the base image
	 * @param overlayImage
	 *            the overlay image
	 * @param quadrant
	 *            the quadrant (one of {@link IDecoration} (
	 *            {@link IDecoration#TOP_LEFT}, {@link IDecoration#TOP_RIGHT},
	 *            {@link IDecoration#BOTTOM_LEFT},
	 *            {@link IDecoration#BOTTOM_RIGHT} or
	 *            {@link IDecoration#UNDERLAY})
	 */
	public DecorationOverlayDescriptor(ImageDescriptor baseImage,
			ImageDescriptor overlayImage, int quadrant) {
		this(baseImage, createArrayFrom(overlayImage, quadrant));
	}

	/**
	 * Convert the given image and quadrant into the proper input array.
	 *
	 * @param overlayImage
	 *            the overlay image
	 * @param quadrant
	 *            the quadrant
	 * @return an array with the given image in the proper quadrant
	 */
	private static ImageDescriptor[] createArrayFrom(
			ImageDescriptor overlayImage, int quadrant) {
		ImageDescriptor[] descs = new ImageDescriptor[] { null, null, null,
				null, null };
		descs[quadrant] = overlayImage;
		return descs;
	}

	/**
	 * Draw the overlays for the receiver.
	 *
	 * @param overlaysArray
	 */
	private void drawOverlays(ImageDescriptor[] overlaysArray) {
		for (int i = 0; i < overlays.length; i++) {
			ImageDescriptor overlay = overlaysArray[i];
			if (overlay == null)
				continue;
			ImageData overlayData = overlay.getImageData();
			// Use the missing descriptor if it is not there.
			if (overlayData == null)
				overlayData = ImageDescriptor.getMissingImageDescriptor()
						.getImageData();

			switch (i) {
			case IDecoration.TOP_LEFT:
				drawImage(overlayData, 0, 0);
				break;
			case IDecoration.TOP_RIGHT:
				drawImage(overlayData, size.x - overlayData.width, 0);
				break;
			case IDecoration.BOTTOM_LEFT:
				drawImage(overlayData, 0, size.y - overlayData.height);
				break;
			case IDecoration.BOTTOM_RIGHT:
				drawImage(overlayData, size.x - overlayData.width, size.y
						- overlayData.height);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DecorationOverlayDescriptor))
			return false;
		DecorationOverlayDescriptor other = (DecorationOverlayDescriptor) o;
		return base.equals(other.base)
				&& Arrays.equals(overlays, other.overlays);
	}

	@Override
	public int hashCode() {
		int code = base.hashCode();
		for (int i = 0; i < overlays.length; i++)
			if (overlays[i] != null)
				code ^= overlays[i].hashCode();
		return code;
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		if (overlays.length > IDecoration.UNDERLAY) {
			ImageDescriptor underlay = overlays[IDecoration.UNDERLAY];
			if (underlay != null)
				drawImage(underlay.getImageData(), 0, 0);
		}

		if (overlays.length > IDecoration.REPLACE
				&& overlays[IDecoration.REPLACE] != null)
			drawImage(overlays[IDecoration.REPLACE].getImageData(), 0, 0);
		else
			drawImage(getBaseImageData(), 0, 0);

		drawOverlays(overlays);
	}

	private ImageData getBaseImageData() {
		final ImageData data = base.getImageData();
		return data != null ? data : DEFAULT_IMAGE_DATA;
	}

	@Override
	protected Point getSize() {
		return size;
	}

	@Override
	protected int getTransparentPixel() {
		return getBaseImageData().transparentPixel;
	}
}
