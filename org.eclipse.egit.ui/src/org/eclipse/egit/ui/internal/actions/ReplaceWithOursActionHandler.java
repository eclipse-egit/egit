/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.egit.core.op.DiscardChangesOperation;

/**
 * Action handler to replace selected conflicting files with 'our' version.
 */
public class ReplaceWithOursActionHandler extends ReplaceConflictActionHandler {

	/**
	 * Creates a new instance.
	 */
	public ReplaceWithOursActionHandler() {
		super(DiscardChangesOperation.Stage.OURS);
	}
}
