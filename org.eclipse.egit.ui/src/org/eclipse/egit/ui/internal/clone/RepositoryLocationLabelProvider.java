/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import org.eclipse.egit.ui.internal.clone.GitCloneSourceProviderExtension.CloneSourceProvider;
import org.eclipse.egit.ui.internal.provisional.wizards.RepositoryServerInfo;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

class RepositoryLocationLabelProvider extends LabelProvider {
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
		return super.getImage(element);
	}
}
