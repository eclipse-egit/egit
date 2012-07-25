/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;

/**
 * Label decorator for warning/error problem markers (used in Staging View and
 * Commit Dialog).
 * <p>
 * Users must make the decoratable element implement {@link IProblemDecoratable}.
 */
public class ProblemLabelDecorator extends BaseLabelProvider implements
		ILightweightLabelDecorator {

	public void decorate(Object element, IDecoration decoration) {
		IProblemDecoratable decoratable = getProblemDecoratable(element);
		if (decoratable != null) {
			int problemSeverity = decoratable.getProblemSeverity();
			if (problemSeverity == IMarker.SEVERITY_ERROR)
				addOverlay(decoration, ISharedImages.IMG_ERROR_OVR);
			else if (problemSeverity == IMarker.SEVERITY_WARNING)
				addOverlay(decoration, ISharedImages.IMG_WARNING_OVR);
		}
	}

	private IProblemDecoratable getProblemDecoratable(Object element) {
		if (element instanceof IProblemDecoratable)
			return (IProblemDecoratable) element;
		else
			return null;
	}

	private void addOverlay(IDecoration decoration, String teamImageId) {
		ImageDescriptor overlay = TeamImages.getImageDescriptor(teamImageId);
		decoration.addOverlay(overlay, IDecoration.BOTTOM_LEFT);
	}
}
