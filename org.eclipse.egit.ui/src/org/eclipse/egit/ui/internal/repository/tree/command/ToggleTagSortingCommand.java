/*******************************************************************************
 * Copyright (c) 2020, Alexander Nittka <alex@nittka.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.CommonUtils;

/**
 * Toggles the "Sort tags ascending" preference.
 */
public class ToggleTagSortingCommand extends AbstractToggleCommand {

	/**
	 * The toggle tag sorting command id
	 */
	public static final String ID = CommonUtils.TOGGLE_TAG_SORTING_COMMAND_ID;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object result = super.execute(event);
		CommonUtils.unsetTagSortingOrder();
		return result;
	}

}
