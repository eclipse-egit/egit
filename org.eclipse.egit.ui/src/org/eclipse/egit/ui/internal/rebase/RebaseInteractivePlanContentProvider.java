/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.lib.RebaseTodoLine;

/**
 *
 */
public enum RebaseInteractivePlanContentProvider implements ITreeContentProvider {
	/** Singleton instance */
	INSTANCE;
	/**
	 *
	 */
	private RebaseInteractivePlanContentProvider() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		// if (inputElement instanceof InteractiveRebasePlan) {
		// return reverse(((InteractiveRebasePlan)
		// inputElement).plan.toArray());
		// }
		return getChildren(inputElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RebaseInteractivePlan) {
			RebaseInteractivePlan plan = (RebaseInteractivePlan) parentElement;
			List<RebaseInteractivePlan.PlanElement> linesToDisplay = new LinkedList<RebaseInteractivePlan.PlanElement>();
			for (RebaseInteractivePlan.PlanElement line : plan.getList()) {
				if (line.isComment())
					continue;
				linesToDisplay.add(line);
			}
			return linesToDisplay.toArray();
		}
		if (parentElement instanceof RebaseTodoLine) {
			// TODO: return touched files
			return new Object[0];
		}
		// TODO:return grandchildren - hunks in files

		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof RebaseInteractivePlan) {
			return null;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return (element instanceof RebaseInteractivePlan);
		// TODO:add children as touched files; grandchildren as hunks in files
	}

}
