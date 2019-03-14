/*******************************************************************************
 * Copyright (C) 2015, 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.resources;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

/**
 * A property tester testing the {@IResourceState} of a file under EGit control.
 * Assumes a {@link Collection} of elements, typically a selection. Skips any
 * resources not in a repository.
 */
public class ResourceStatePropertyTester extends AbstractPropertyTester {

	private enum Property {
		/**
		 * {@code true} if the collection contains at least one item with staged
		 * changes.
		 */
		HAS_STAGED_CHANGES,

		/**
		 * {@code true} if the collection contains at least one item with
		 * unstaged changes.
		 */
		HAS_UNSTAGED_CHANGES,

		/**
		 * {@code true} if the collection contains at least one item that is not
		 * ignored.
		 */
		HAS_NOT_IGNORED_RESOURCES,

		/**
		 * {@code true} if the collection contains at least one item that is
		 * tracked (i.e., that is neither ignored nor untracked).
		 */
		HAS_TRACKED_RESOURCES
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		Property prop = property != null ? toProperty(property) : null;
		if (prop == null || receiver == null) {
			return false;
		}
		return computeResult(expectedValue, internalTest(receiver, prop));
	}

	private boolean internalTest(@NonNull Object receiver,
			@NonNull Property property) {
		Collection<?> collection = (Collection<?>) receiver;
		if (collection.isEmpty()) {
			return false;
		}
		IStructuredSelection selection = null;
		Object first = collection.iterator().next();
		if (collection.size() == 1 && first instanceof ITextSelection) {
			selection = SelectionUtils
					.getStructuredSelection((ITextSelection) first);
		} else {
			selection = new StructuredSelection(new ArrayList<>(collection));
		}
		for (IPath path : SelectionUtils.getSelectedLocations(selection)) {
			if (path == null || ResourceUtil.getRepository(path) == null) {
				continue;
			}
			IResourceState state = ResourceStateFactory.getInstance()
					.get(path.toFile());
			switch (property) {
			case HAS_STAGED_CHANGES:
				if (state.isStaged()) {
					return true;
				}
				break;
			case HAS_UNSTAGED_CHANGES:
				if (state.hasUnstagedChanges()) {
					return true;
				}
				break;
			case HAS_NOT_IGNORED_RESOURCES:
				if (!state.isIgnored()) {
					return true;
				}
				break;
			case HAS_TRACKED_RESOURCES:
				if (state.isTracked()) {
					return true;
				}
				break;
			}
		}
		return false;
	}

	@Nullable
	private Property toProperty(@NonNull String value) {
		if ("hasStagedChanges".equals(value)) { //$NON-NLS-1$
			return Property.HAS_STAGED_CHANGES;
		} else if ("hasUnstagedChanges".equals(value)) { //$NON-NLS-1$
			return Property.HAS_UNSTAGED_CHANGES;
		} else if ("hasNotIgnoredResources".equals(value)) { //$NON-NLS-1$
			return Property.HAS_NOT_IGNORED_RESOURCES;
		} else if ("hasTrackedResources".equals(value)) { //$NON-NLS-1$
			return Property.HAS_TRACKED_RESOURCES;
		}
		return null;
	}

}
