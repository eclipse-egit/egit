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
package org.eclipse.egit.ui;

import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.egit.ui.internal.staging.StagingView;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 *
 */
public class GitRepositoriesPerspectiveFactory implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {

		// repositories on the left hand
		layout.addView(RepositoriesView.VIEW_ID, IPageLayout.LEFT, (float) 0.5, layout.getEditorArea());

		// Properties under editor area
		layout.addView(IPageLayout.ID_PROP_SHEET, IPageLayout.BOTTOM, (float) 0.5, layout
				.getEditorArea());

		// place holder for Package Explorer under repositories
		layout.addPlaceholder("org.eclipse.jdt.ui.PackageExplorer", IPageLayout.BOTTOM, (float) 0.7, RepositoriesView.VIEW_ID); //$NON-NLS-1$

		// shortcut to Package Explorer
		layout.addShowViewShortcut("org.eclipse.jdt.ui.PackageExplorer"); //$NON-NLS-1$
		// shortcut to History view
		layout.addShowViewShortcut(IHistoryView.VIEW_ID);
		// shortcut to Synchronize view
		layout.addShowViewShortcut(ISynchronizeView.VIEW_ID);
		// shortcut to Staging view
		layout.addShowViewShortcut(StagingView.VIEW_ID);

	}

}
