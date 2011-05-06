/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.op.DiscardChangesOperation;
import org.eclipse.jgit.lib.Constants;

/**
 * Replace with HEAD revision action handler
 */
public class ReplaceWithHeadActionHandler extends DiscardChangesActionHandler {

	@Override
	protected DiscardChangesOperation createOperation(ExecutionEvent event)
			throws ExecutionException {
		return new DiscardChangesOperation(getSelectedResources(event),
				Constants.HEAD);
	}

}
