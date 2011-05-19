/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for a {@link ComboViewer} to display Repositories
 * <p>
 * Renders "<Repository name> - <Repository directory>" similar to the
 * {@link RepositoriesView}
 */
public class RepoComboLabelProvider extends BaseLabelProvider implements
		ILabelProvider {
	private RepositoryUtil util = Activator.getDefault().getRepositoryUtil();

	public Image getImage(Object element) {
		return null;
	}

	public String getText(Object element) {
		Repository repo = (Repository) element;
		String repoName = util.getRepositoryName(repo);
		return repoName + " - " + repo.getDirectory().getPath(); //$NON-NLS-1$
	}
}
