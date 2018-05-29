/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Benjamin Muskalla (Tasktop Technologies Inc.) - support for model scoping
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jgit.lib.Constants;

/**
 * Replace with HEAD revision action handler
 */
public class ReplaceWithHeadActionHandler extends DiscardChangesActionHandler {

	@Override
	protected String gatherRevision(ExecutionEvent event) {
		return Constants.HEAD;
	}

}
