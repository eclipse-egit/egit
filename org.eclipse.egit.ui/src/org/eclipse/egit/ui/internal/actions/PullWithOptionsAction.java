/*******************************************************************************
 * Copyright (c) 2016, Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - [485124] initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * An action for asking user to specify a pull operation (via wizard) and run it
 *
 * @see PullWithOptionsActionHandler
 */
public class PullWithOptionsAction extends RepositoryAction {

	/**
	 *
	 */
	public PullWithOptionsAction() {
		super(ActionCommands.PULL_WITH_OPTIONS,
				new PullWithOptionsActionHandler());
	}

}
