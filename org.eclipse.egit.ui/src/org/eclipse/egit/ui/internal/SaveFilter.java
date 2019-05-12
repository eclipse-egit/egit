/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.Activator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISaveableFilter;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.ide.ResourceUtil;

/*
 * Copied from org.eclipse.ui.ide.IDE.SaveFilter, see
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=386609
 * Replace with the above when we can depend on Eclipse 4.3.
 */
/**
 * A saveable filter that selects savables that contain resources that
 * are descendants of the roots of the filter.
 * @since 3.3
 *
 */
class SaveFilter implements ISaveableFilter {
	private final IResource[] roots;

	/**
	 * Create the filter
	 * @param roots the save roots
	 */
	public SaveFilter(IResource[] roots) {
		this.roots = roots;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveableFilter#select(org.eclipse.ui.Saveable, org.eclipse.ui.IWorkbenchPart[])
	 */
	@Override
	public boolean select(Saveable saveable,
			IWorkbenchPart[] containingParts) {
		if (isDescendantOfRoots(saveable)) {
			return true;
		}
		// For backwards compatibility, we need to check the parts
		for (IWorkbenchPart workbenchPart : containingParts) {
			if (workbenchPart instanceof IEditorPart) {
				IEditorPart editorPart = (IEditorPart) workbenchPart;
				if (isEditingDescendantOf(editorPart)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return whether the given saveable contains any resources that
	 * are descendants of the root resources.
	 * @param saveable the saveable
	 * @return whether the given saveable contains any resources that
	 * are descendants of the root resources
	 */
	private boolean isDescendantOfRoots(Saveable saveable) {
		// First, try and adapt the saveable to a resource mapping.
		ResourceMapping mapping = ResourceUtil.getResourceMapping(saveable);
		if (mapping != null) {
			try {
				ResourceTraversal[] traversals = mapping.getTraversals(
						ResourceMappingContext.LOCAL_CONTEXT, null);
				for (ResourceTraversal traversal : traversals) {
					IResource[] resources = traversal.getResources();
					for (IResource resource : resources) {
						if (isDescendantOfRoots(resource)) {
							return true;
						}
					}
				}
			} catch (CoreException e) {
				Activator
				.logError(
						NLS
						.bind(
								"An internal error occurred while determining the resources for {0}", saveable.getName()), e); //$NON-NLS-1$
			}
		} else {
			// If there is no mapping, try to adapt to a resource or file directly
			IFile file = ResourceUtil.getFile(saveable);
			if (file != null) {
				return isDescendantOfRoots(file);
			}
		}
		return false;
	}

	/**
	 * Return whether the given resource is either equal to or a descendant of
	 * one of the given roots.
	 *
	 * @param resource the resource to be tested
	 * @return whether the given resource is either equal to or a descendant of
	 *         one of the given roots
	 */
	private boolean isDescendantOfRoots(IResource resource) {
		for (IResource root : roots) {
			if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return whether the given dirty editor part is editing resources that are
	 * descendants of the given roots.
	 *
	 * @param part the dirty editor part
	 * @return whether the given dirty editor part is editing resources that are
	 *         descendants of the given roots
	 */
	private boolean isEditingDescendantOf(IEditorPart part) {
		IFile file = ResourceUtil.getFile(part.getEditorInput());
		if (file != null) {
			return isDescendantOfRoots(file);
		}
		return false;
	}

}
