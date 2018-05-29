/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
