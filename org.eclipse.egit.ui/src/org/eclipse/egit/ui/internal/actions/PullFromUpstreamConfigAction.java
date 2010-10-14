/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

/**
 * An action for pulling the currently checked branch
 */
public class PullFromUpstreamConfigAction extends RepositoryAction {
	/**
	 *
	 */
	public PullFromUpstreamConfigAction() {
		super(ActionCommands.PULL_FROM_UPSTREAM_CONFIG,
				new PullFromUpstreamActionHandler());
	}
}
