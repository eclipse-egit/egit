/*******************************************************************************
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.AssumeUnchangedOperation;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.UIText;

/**
 * This operation unsets the assume-valid bit in the index for the
 * selected resources.
 *
 * @see AssumeUnchangedOperation
 */
public class NoAssumeUnchangedAction extends AbstractResourceOperationAction {
	protected IEGitOperation createOperation(final List<IResource> sel) {
		return sel.isEmpty() ? null : new AssumeUnchangedOperation(sel, false);
	}

	@Override
	protected String getJobName() {
		return UIText.AssumeUnchanged_noAssumeUnchanged;
	}
}
