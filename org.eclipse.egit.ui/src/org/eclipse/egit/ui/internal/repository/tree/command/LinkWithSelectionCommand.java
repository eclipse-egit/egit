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
package org.eclipse.egit.ui.internal.repository.tree.command;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

/**
 * Implements "Link With Selection" toggle
 */
public class LinkWithSelectionCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {

	/** Command ID for the {@link LinkWithSelectionCommand}. */
	public static final String ID = "org.eclipse.egit.ui.RepositoriesLinkWithSelection"; //$NON-NLS-1$

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Command command = event.getCommand();
		HandlerUtil.toggleCommandState(command);
		boolean test = ((Boolean) command.getState(RegistryToggleState.STATE_ID)
				.getValue()).booleanValue();
		getView(event).setReactOnSelection(test);
		return null;
	}
}
