/*******************************************************************************
 * Copyright (c) 2016, Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
