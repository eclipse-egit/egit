/*******************************************************************************
 * Copyright (C) 2015, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;

/**
 * A property tester testing the {@IResourceState} of a file under EGit control.
 * Assumes a {@link Collection} of elements, typically a selection, and requires
 * for all properties that the selected elements belong to one single
 * repository.
 */
public class ResourceStatePropertyTester extends PropertyTester {

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
		 * {@code true} if the collection contain at least one item that is not
		 * ignored.
		 */
		HAS_NOT_IGNORED_RESOURCES,
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		Property prop = property != null ? toProperty(property) : null;
		if (prop == null || receiver == null) {
			return false;
		}
		boolean result = internalTest(receiver, prop);
		if (expectedValue != null) {
			return expectedValue.equals(Boolean.valueOf(result));
		} else {
			return result;
		}
	}

	private boolean internalTest(@NonNull Object receiver,
			@NonNull Property property) {
		Collection<?> collection = (Collection<?>) receiver;
		if (collection.isEmpty()) {
			return false;
		}
		Repository repository;
		IStructuredSelection selection = null;
		Object first = collection.iterator().next();
		if (collection.size() == 1 && first instanceof ITextSelection) {
			selection = SelectionUtils
					.getStructuredSelection((ITextSelection) first);
		} else {
			selection = new StructuredSelection(new ArrayList<>(collection));
		}
		repository = SelectionUtils.getCurrentRepository();
		if (repository == null || repository.isBare()) {
			return false;
		}
		IndexDiffCacheEntry diffCacheEntry = Activator.getDefault()
				.getIndexDiffCache().getIndexDiffCacheEntry(repository);
		if (diffCacheEntry == null) {
			return false;
		}
		IndexDiffData indexDiffData = diffCacheEntry.getIndexDiff();
		if (indexDiffData == null) {
			return false;
		}

		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			IResource resource = AdapterUtils.adapt(iterator.next(),
					IResource.class);
			if (resource == null || !resource.isAccessible()) {
				continue;
			}
			IResourceState state = ResourceStateFactory.getInstance()
					.get(indexDiffData, resource);
			switch (property) {
			case HAS_STAGED_CHANGES:
				if (state.isStaged()) {
					return true;
				}
				break;
			case HAS_UNSTAGED_CHANGES:
				if (!state.isTracked() || state.isDirty()
						|| state.isMissing()) {
					return true;
				}
				break;
			case HAS_NOT_IGNORED_RESOURCES:
				if (!state.isIgnored()) {
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
		}
		return null;
	}

}
