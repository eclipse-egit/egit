/*******************************************************************************
 * Copyright (C) 2011, Abhishek Bhatnagar <abhatnag@redhat.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.actions;

/**
 * ListStashAction
 */
public class ListStashAction extends RepositoryAction {

	/**
	 * Constructs this action
	 */
	public ListStashAction() {
		super(ActionCommands.LIST_STASH, new ListStashActionHandler());
	}
}
