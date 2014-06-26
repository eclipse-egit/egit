/*******************************************************************************
 * Copyright (C) 2014, Matthias Sohn <matthias.sohn@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Action creating a stash
 */
public class StashCreateAction extends RepositoryAction {
	/**
	 * Constructs this action
	 */
	public StashCreateAction() {
		super(ActionCommands.STASH_CREATE_ACTION,
				new StashCreateActionHandler());
	}
}
