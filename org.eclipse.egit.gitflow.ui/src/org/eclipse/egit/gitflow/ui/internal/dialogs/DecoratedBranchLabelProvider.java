/*******************************************************************************
 * Copyright (C) 2016, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.dialogs;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;

class DecoratedBranchLabelProvider extends ColumnLabelProvider {
	private ResourceManager resourceManager = Activator.getDefault().getResourceManager();

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
			return decorateImage((Ref) element);
		}
		return super.getImage(element);
	}

	private Image decorateImage(Ref node) {
		String refName = node.getName();

		String branchName;
		String compareString;

		try {
			branchName = repository.getFullBranch();
			compareString = refName;
		} catch (IOException e) {
			return UIIcons.getImage(resourceManager, UIIcons.BRANCH);
		}

		if (compareString.equals(branchName)) {
			return UIIcons.getImage(resourceManager, UIIcons.CHECKED_OUT_BRANCH);
		} else {
			return UIIcons.getImage(resourceManager, UIIcons.BRANCH);
		}
	}
}
