/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Daniel Megert <daniel_megert@ch.ibm.com> - EGit must not pollute toolbars of perspectives it doesn't own - http://bugs.eclipse.org/356554
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.egit.ui.internal.reflog.ReflogView;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Git Repository Exploring perspective factory
 */
public class GitRepositoriesPerspectiveFactory implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {

		// repositories on the left hand
		layout.addView(RepositoriesView.VIEW_ID, IPageLayout.LEFT, (float) 0.3,
				layout.getEditorArea());

		IFolderLayout bottom = layout.createFolder(
				"bottom", IPageLayout.BOTTOM, (float) 0.5, //$NON-NLS-1$
				layout.getEditorArea());

		// Views under editor area
		bottom.addView(IHistoryView.VIEW_ID);
		bottom.addView(ISynchronizeView.VIEW_ID);
		bottom.addView(StagingView.VIEW_ID);
		bottom.addView(ReflogView.VIEW_ID);
		bottom.addView(IPageLayout.ID_PROP_SHEET);

		// place holder for Package Explorer under repositories
		layout.addPlaceholder("org.eclipse.jdt.ui.PackageExplorer", IPageLayout.BOTTOM, (float) 0.7, RepositoriesView.VIEW_ID); //$NON-NLS-1$

		// Window->Show View
		layout.addShowViewShortcut("org.eclipse.jdt.ui.PackageExplorer"); //$NON-NLS-1$
		layout.addShowViewShortcut(IHistoryView.VIEW_ID);
		layout.addShowViewShortcut(ISynchronizeView.VIEW_ID);
		layout.addShowViewShortcut(StagingView.VIEW_ID);
		layout.addShowViewShortcut(ReflogView.VIEW_ID);
		layout.addShowViewShortcut(RepositoriesView.VIEW_ID);

		// Window->Perspective->Open
		layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$

		// Window->Perspective->Customize
		layout.addActionSet("org.eclipse.egit.ui.navigation"); //$NON-NLS-1$
		layout.addActionSet("org.eclipse.egit.ui.SearchActionSet"); //$NON-NLS-1$

		// File->New
		layout.addNewWizardShortcut(
				"org.eclipse.egit.ui.CreateRepositoryWizard"); //$NON-NLS-1$
	}

}
