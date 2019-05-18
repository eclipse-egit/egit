/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.RebaseTodoLine;

/**
 * Content provider feeding rebase interactive plan
 */
public enum RebaseInteractivePlanContentProvider implements ITreeContentProvider {

	/** Singleton instance */
	INSTANCE;

	private RebaseInteractivePlanContentProvider() {
	}

	@Override
	public void dispose() {
		// empty
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// empty
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RebaseInteractivePlan) {
			RebaseInteractivePlan plan = (RebaseInteractivePlan) parentElement;
			List<RebaseInteractivePlan.PlanElement> linesToDisplay = new LinkedList<>();
			int i = 0;
			for (RebaseInteractivePlan.PlanElement line : plan.getList()) {
				if (line.isComment())
					continue;
				linesToDisplay.add(line);
				line.setUserData(Integer.valueOf(++i));
			}

			if (RebaseInteractivePreferences.isOrderReversed())
				Collections.reverse(linesToDisplay);

			return linesToDisplay.toArray();
		}

		if (parentElement instanceof RebaseTodoLine) {
			// TODO: return touched files
			return new Object[0];
		}
		// TODO:return grandchildren - hunks in files

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof RebaseInteractivePlan);
		// TODO:add children as touched files; grandchildren as hunks in files
	}
}
