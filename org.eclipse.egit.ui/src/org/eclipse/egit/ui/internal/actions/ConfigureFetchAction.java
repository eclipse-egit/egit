/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * Configure Fetch
 */
public class ConfigureFetchAction extends RepositoryAction {

	/**
	 * Constructs this action
	 */
	public ConfigureFetchAction() {
		super(ActionCommands.CONFIGURE_FETCH, new ConfigureFetchActionHandler());
	}
}
