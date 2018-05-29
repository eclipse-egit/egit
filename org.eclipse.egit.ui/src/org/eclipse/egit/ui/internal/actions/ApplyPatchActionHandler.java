/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Taken from the corresponding Team UI action
 */
public class ApplyPatchActionHandler extends RepositoryActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IResource[] resources = getSelectedResources(event);
		IResource resource = null;
		if (resources.length > 0) {
			resource = resources[0];
		}
		boolean isPatch = false;
		if (resource instanceof IFile) {
			try {
				isPatch = ApplyPatchOperation.isPatch((IFile) resource);
			} catch (CoreException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		}

		final ApplyPatchOperation op;
		if (isPatch) {
			op = new ApplyPatchOperation(HandlerUtil.getActivePart(event),
					(IFile) resource, null, new CompareConfiguration());
		} else {
			op = new ApplyPatchOperation(HandlerUtil.getActivePart(event),
					resource);
		}
		BusyIndicator.showWhile(Display.getDefault(), op);
		return null;
	}
}
