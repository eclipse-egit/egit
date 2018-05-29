/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.sharing;

import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
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

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		Repository repo = (Repository) element;
		String repoName = util.getRepositoryName(repo);
		return NLS.bind("{0} - {1}", repoName, repo.getDirectory().getPath()); //$NON-NLS-1$
	}
}
