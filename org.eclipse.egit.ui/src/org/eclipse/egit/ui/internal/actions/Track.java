/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.TrackOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * An action to add resources to the Git repository.
 *
 * @see TrackOperation
 */
public class Track extends RepositoryAction {

	@Override
	public void execute(IAction action) {
		try {
			final TrackOperation op = new TrackOperation(Arrays
					.asList(getSelectedResources()));
			getTargetPart().getSite().getWorkbenchWindow().run(true, false,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor arg0)
								throws InvocationTargetException,
								InterruptedException {
							try {
								op.run(arg0);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
		} catch (InvocationTargetException e) {
			Activator.logError(UIText.Track_error, e);
			MessageDialog.openError(getShell(), UIText.Track_error, UIText.Track_see_log);
		} catch (InterruptedException e) {
			MessageDialog.openError(getShell(), UIText.Track_error, e.getMessage());
		}
	}

	@Override
	public boolean isEnabled() {
		return getSelectedAdaptables(getSelection(), IResource.class).length > 0;
	}
}
