/******************************************************************************
 *  Copyright (c) 2024 Thomas Wolf <twolf@apache.org> and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.egit.ui.internal.filediff;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract super class of all command handlers for {@link FileDiff}s.
 */
public abstract class AbstractFileDiffHandler extends AbstractHandler {

	/**
	 * Retrieves the current selection.
	 *
	 * @param event
	 *            {@link ExecutionEvent} that triggered the handler execution
	 * @return the {@link IStructuredSelection}
	 * @throws ExecutionException
	 */
	protected IStructuredSelection getSelection(ExecutionEvent event)
			throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection) {
			return (IStructuredSelection) selection;
		}
		return StructuredSelection.EMPTY;
	}

	/**
	 * Retrieves the single {@link FileDiff} from a selection.
	 *
	 * @param selection
	 *            to get the {@link FileDiff} from
	 * @return the {@link FileDiff}, if the selection has exactly one element
	 *         and it adapts to {@link FileDiff}, {@code null} otherwise
	 */
	protected FileDiff getDiff(IStructuredSelection selection) {
		if (selection.size() == 1) {
			return Adapters.adapt(selection.getFirstElement(), FileDiff.class);
		}
		return null;
	}

	/**
	 * Retrieves all {@link FileDiff}s from a selection that match a certain
	 * {@link Predicate}.
	 *
	 * @param selection
	 *            to get {@link FileDiff}s from
	 * @param filter
	 *            to choose which {@link FileDiff}s to return
	 * @return the filtered {@link FileDiff}s
	 */
	protected List<FileDiff> getDiffs(IStructuredSelection selection,
			Predicate<FileDiff> filter) {
		List<FileDiff> result = new ArrayList<>();
		Iterator<?> items = selection.iterator();
		while (items.hasNext()) {
			FileDiff diff = Adapters.adapt(items.next(), FileDiff.class);
			if (diff != null && filter.test(diff)) {
				result.add(diff);
			}
		}
		return result;
	}

}
