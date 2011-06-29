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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Implements "Link With Selection" toggle
 */
public class LinkWithSelectionCommand extends
		RepositoriesViewCommandHandler<RepositoryTreeNode> {
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		// is there a better way?
		Event evt = (Event) event.getTrigger();
		Widget w = evt.widget;
		final boolean selected;
		if (w instanceof ToolItem) {
			ToolItem item = (ToolItem) w;
			selected = item.getSelection();
		} else if (w instanceof MenuItem) {
			MenuItem item = (MenuItem) w;
			selected = item.getSelection();
		} else
			throw new IllegalStateException(
					"unexpected type: " + w.getClass().toString()); //$NON-NLS-1$
		getView(event).setReactOnSelection(selected);
		return null;
	}
}
