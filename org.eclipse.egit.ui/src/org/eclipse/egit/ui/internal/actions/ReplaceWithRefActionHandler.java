/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Benjamin Muskalla (Tasktop Technologies)
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.egit.ui.internal.dialogs.ReplaceTargetSelectionDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;

/**
 * Replace with ref action handler
 */
public class ReplaceWithRefActionHandler extends DiscardChangesActionHandler {

	@Override
	protected DiscardChangesOperation createOperation(ExecutionEvent event)
			throws ExecutionException {
		final IResource[] resources = getSelectedResources(event);
		Shell shell = getShell(event);
		Repository repository = getRepository(true, event);
		final String pathString = resources.length == 1 ? resources[0].getFullPath()
				.toString() : null;
		ReplaceTargetSelectionDialog dlg = new ReplaceTargetSelectionDialog(
				shell, repository, pathString);
		return dlg.open() == Window.OK ? new DiscardChangesOperation(resources,
				dlg.getRefName()) : null;
	}

}
