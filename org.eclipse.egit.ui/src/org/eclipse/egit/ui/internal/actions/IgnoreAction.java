/*******************************************************************************
 * Copyright (C) 2009, Alex Blewitt <alex.blewitt@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/** Action for ignoring files via .gitignore. */
public class IgnoreAction extends RepositoryAction {
	/**
	 *
	 */
	public IgnoreAction() {
		super(ActionCommands.IGNORE_ACTION, new IgnoreActionHandler());
	}
}
