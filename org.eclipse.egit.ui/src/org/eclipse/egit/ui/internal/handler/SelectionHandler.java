/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.handler;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Base handler with helpers for working with selections
 */
public abstract class SelectionHandler extends AbstractHandler {

	/**
	 * Get structured selection for event
	 *
	 * @param event
	 * @return selection, never null
	 */
	protected IStructuredSelection getSelection(final ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection == null || selection.isEmpty())
			selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection) selection;
		return StructuredSelection.EMPTY;
	}

	/**
	 * Get current selection as target item class
	 *
	 * @param itemClass
	 * @param event
	 * @param <V> type of itemClass
	 * @return non-null but possibly empty list
	 */
	protected <V> V getSelectedItem(final Class<V> itemClass,
			final ExecutionEvent event) {
		final Object selected = getSelection(event).getFirstElement();
		return AdapterUtils.adapt(selected, itemClass);
	}

	/**
	 * Get current selection as list of target item class objects
	 *
	 * @param itemClass
	 * @param event
	 * @param <V> type of itemClass
	 * @return non-null but possibly empty list
	 */
	protected <V> List<V> getSelectedItems(Class<V> itemClass,
			ExecutionEvent event) {
		final List<V> items = new LinkedList<>();
		for (Object selected : getSelection(event).toArray()) {
			V adapted = AdapterUtils.adapt(selected, itemClass);
			if (adapted != null)
				items.add(adapted);
		}
		return items;
	}

	/**
	 * Get workbench part for event
	 *
	 * @param event
	 * @return part
	 * @throws ExecutionException
	 */
	protected IWorkbenchPart getPart(ExecutionEvent event)
			throws ExecutionException {
		return HandlerUtil.getActivePartChecked(event);
	}
}
