/*******************************************************************************
 * Copyright (C) 2011, Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.patch.PatchOperationUI;

/**
 * The "Create Patch" action.
 */
public class CreatePatchActionHandler extends RepositoryActionHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		PatchOperationUI.createPatch(getPart(event), getRepository()).start();
		return null;
	}

	@Override
	public boolean isEnabled() {
		return getRepository() != null;
	}
}
