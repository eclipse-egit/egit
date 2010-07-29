/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.UIText;

/**
 * An action to add files to a Git index.
 *
 * @see AddToIndexOperation
 */
public class AddToIndexAction extends AbstractResourceOperationAction {
	private AddToIndexOperation operation = null;

	protected IEGitOperation createOperation(final List<IResource> sel) {
		if (sel.isEmpty()) {
			return null;
		} else {
			operation = new AddToIndexOperation(sel);
			return operation;
		}
	}

	@Override
	protected String getJobName() {
		return UIText.AddToIndexAction_addingFiles;
	}


}
