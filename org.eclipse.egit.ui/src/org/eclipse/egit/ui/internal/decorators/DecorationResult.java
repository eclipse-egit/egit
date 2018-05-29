/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * Copyright (C) 2009, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * IDecoration which stores the applied decoration elements and provides methods
 * for getting them.
 */
public class DecorationResult implements IDecoration {

	private List<String> prefixes = new ArrayList<>();

	private List<String> suffixes = new ArrayList<>();

	private ImageDescriptor overlay = null;

	private Color backgroundColor = null;

	private Font font = null;

	private Color foregroundColor = null;

	/**
	 * Adds an icon overlay to the decoration
	 * <p>
	 * Copies the behavior of <code>DecorationBuilder</code> of only allowing
	 * the overlay to be set once.
	 */
	@Override
	public void addOverlay(ImageDescriptor overlayImage) {
		if (overlay == null)
			overlay = overlayImage;
	}

	@Override
	public void addOverlay(ImageDescriptor overlayImage, int quadrant) {
		addOverlay(overlayImage);
	}

	@Override
	public void addPrefix(String prefix) {
		prefixes.add(prefix);
	}

	@Override
	public void addSuffix(String suffix) {
		suffixes.add(suffix);
	}

	@Override
	public IDecorationContext getDecorationContext() {
		return new DecorationContext();
	}

	@Override
	public void setBackgroundColor(Color color) {
		backgroundColor = color;
	}

	@Override
	public void setForegroundColor(Color color) {
		foregroundColor = color;
	}

	@Override
	public void setFont(Font font) {
		this.font = font;
	}

	/**
	 * @return the overlay image which was set
	 */
	public ImageDescriptor getOverlay() {
		return overlay;
	}

	/**
	 * @return background color
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * @return foreground color
	 */
	public Color getForegroundColor() {
		return foregroundColor;
	}

	/**
	 * @return font
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * @return text decoration prefix
	 */
	public String getPrefix() {
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> iter = prefixes.iterator(); iter.hasNext();) {
			sb.append(iter.next());
		}
		return sb.toString();
	}

	/**
	 * @return text decoration suffix
	 */
	public String getSuffix() {
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> iter = suffixes.iterator(); iter.hasNext();) {
			sb.append(iter.next());
		}
		return sb.toString();
	}

}
