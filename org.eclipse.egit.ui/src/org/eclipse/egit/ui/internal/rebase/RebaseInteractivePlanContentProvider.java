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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.ui.internal.rebase.RebaseInteractivePlan.PlanEntry;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

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

	private Object[] reverse(List<PlanEntry> list) {
		List<Object> copy = new ArrayList<Object>(list);
		Collections.reverse(copy);
		return copy.toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RebaseInteractivePlan) {
			return reverse(((RebaseInteractivePlan) parentElement).getTodo());
		}
		if (parentElement instanceof PlanEntry) {
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
