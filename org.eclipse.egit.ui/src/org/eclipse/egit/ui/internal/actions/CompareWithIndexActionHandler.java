/*******************************************************************************
 * Copyright (C) 2009, Yann Simon <yann.simon.fr@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.GitCompareFileRevisionEditorInput;
import org.eclipse.egit.ui.internal.dialogs.CompareTreeView;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The "compare with index" action. This action opens a diff editor comparing
 * the file as found in the working directory and the version found in the index
 * of the repository.
 */
public class CompareWithIndexActionHandler extends RepositoryActionHandler {
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// assert all resources map to the same repository
		if (getRepository(true, event) == null)
			return null;
		final IPath[] locations = getSelectedLocations(event);

		if (locations.length == 1 && locations[0].toFile().isFile()) {
			final IPath baseLocation = locations[0];
			final ITypedElement base = CompareUtils
					.getFileTypedElement(baseLocation);
			final ITypedElement next;
			try {
				next = CompareUtils.getIndexTypedElement(baseLocation);
			} catch (IOException e) {
				Activator.handleError(
						UIText.CompareWithIndexAction_errorOnAddToIndex, e,
						true);
				return null;
			}

			final GitCompareFileRevisionEditorInput in = new GitCompareFileRevisionEditorInput(
					base, next, null);
			IWorkbenchPage workBenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
			CompareUtils.openInCompare(workBenchPage, in);
		} else {
			CompareTreeView view;
			try {
				view = (CompareTreeView) PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage().showView(
								CompareTreeView.ID);
				view.setInput(getSelectedResources(event), CompareTreeView.INDEX_VERSION);
			} catch (PartInitException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
		}
		return null;
	}


	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
