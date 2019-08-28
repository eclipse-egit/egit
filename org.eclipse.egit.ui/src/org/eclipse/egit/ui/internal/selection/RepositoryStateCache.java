/*******************************************************************************
 * Copyright (C) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;

/**
 * A global cache of some state of repositories. The cache is automatically
 * cleared whenever the workbench selection or the menu selection changes.
 * <p>
 * Intended for use in property testers and in other handler activation or
 * enablement code, such as {@code isEnabled()} methods. Using this cache can
 * massively reduce the number of file system accesses done in the UI thread to
 * evaluate some git repository state. The first handler evaluations will fill
 * the cache, and subsequent enablement expressions can then re-use these cached
 * values.
 * </p>
 */
public class RepositoryStateCache {

	private enum RepositoryItem {
		CONFIG, HEAD, HEAD_REF, FULL_BRANCH_NAME, STATE
	}

	/** The singleton instance of the {@link RepositoryStateCache}. */
	public static final RepositoryStateCache INSTANCE = new RepositoryStateCache();

	/** "null" marker in the maps. */
	private static final Object NOTHING = new Object();

	private final AtomicBoolean stopped = new AtomicBoolean();

	private final Map<File, Map<RepositoryItem, Object>> cache = new ConcurrentHashMap<>();

	private RepositoryStateCache() {
		// No creation from outside
	}

	/**
	 * Initializes the {@link RepositoryStateCache} and makes it listen to changes
	 * that may affect the cache.
	 */
	public void initialize() {
		// Clear the cache whenever the selection changes.
		IEclipseContext applicationContext = PlatformUI.getWorkbench()
				.getService(IEclipseContext.class);
		// A RunAndTrack on the workbench context runs *before* any E3 or E4
		// selection listener on the selection service, which is how expression
		// re-evaluations for expressions based on the current selection are
		// triggered. So we can ensure here that re-evaluations don't use stale
		// cached values.
		applicationContext.runAndTrack(new ContextListener(stopped, cache));
	}

	/**
	 * Disposes the {@link RepositoryStateCache} and makes it stop listening to
	 * changes in the workbench context.
	 */
	public void dispose() {
		stopped.set(true);
	}

	private static class ContextListener extends RunAndTrack {

		private AtomicBoolean stopped;

		private Map<?, ?> cache;

		ContextListener(AtomicBoolean stopped, Map<?, ?> cache) {
			super();
			this.stopped = stopped;
			this.cache = cache;
		}

		private Object lastSelection;

		private Object lastMenuSelection;

		@Override
		public boolean changed(IEclipseContext context) {
			if (stopped.get()) {
				cache.clear();
				return false;
			}
			Object selection = context
					.get(ISources.ACTIVE_CURRENT_SELECTION_NAME);
			if (selection instanceof ITextSelection) {
				selection = getInput(context);
			}
			Object menuSelection = context
					.getActive(ISources.ACTIVE_MENU_SELECTION_NAME);
			if (menuSelection instanceof ITextSelection) {
				menuSelection = getInput(context);
			}
			// Clearing the cache on every workbench _or_ menu selection
			// change is defensive. It might be possible to not clear the
			// cache if the menuSelection == lastSelection.
			if (selection != lastSelection
					|| menuSelection != lastMenuSelection) {
				cache.clear();
			}
			lastSelection = selection;
			lastMenuSelection = menuSelection;
			return true;
		}

		private Object getInput(IEclipseContext context) {
			Object[] input = { null };
			runExternalCode(() -> {
				IEditorInput e = getEditorInput(context);
				input[0] = e != null ? e : StructuredSelection.EMPTY;
			});
			return input[0];
		}

		private IEditorInput getEditorInput(IEclipseContext context) {
			Object part = context.get(ISources.ACTIVE_PART_NAME);
			if (!(part instanceof IEditorPart)) {
				return null;
			}
			Object object = context.get(ISources.ACTIVE_EDITOR_INPUT_NAME);
			Object editor = context.get(ISources.ACTIVE_EDITOR_NAME);
			if (editor instanceof MultiPageEditorPart) {
				Object nestedEditor = ((MultiPageEditorPart) editor)
						.getSelectedPage();
				if (nestedEditor instanceof IEditorPart) {
					object = ((IEditorPart) nestedEditor).getEditorInput();
				}
			}
			if (!(object instanceof IEditorInput)
					&& (editor instanceof IEditorPart)) {
				object = ((IEditorPart) editor).getEditorInput();
			}
			if (object instanceof IEditorInput) {
				return (IEditorInput) object;
			}
			return null;
		}
	}

	private Map<RepositoryItem, Object> getItems(Repository repository) {
		return cache.computeIfAbsent(repository.getDirectory(),
				gitDir -> new ConcurrentHashMap<>());
	}

	/**
	 * Retrieves the given repository's {@link StoredConfig}.
	 *
	 * @param repository
	 * @return the {@link StoredConfig} of the repository
	 */
	public StoredConfig getConfig(Repository repository) {
		Object value = getItems(repository).computeIfAbsent(
				RepositoryItem.CONFIG, key -> repository.getConfig());
		return (StoredConfig) value;
	}

	private ObjectId getHead(Repository repository,
			String[] fullName, Ref[] ref) {
		ObjectId head = ObjectId.zeroId();
		String name = null;
		Ref r = null;
		try {
			r = repository.exactRef(Constants.HEAD);
		} catch (IOException e) {
			Activator.logError(e.getLocalizedMessage(), e);
		}
		ref[0] = r;
		if (r != null) {
			if (r.isSymbolic()) {
				name = r.getTarget().getName();
			}
			head = r.getObjectId();
			if (head != null) {
				if (name == null) {
					name = head.name();
				}
			} else {
				head = ObjectId.zeroId();
			}
		}
		fullName[0] = name != null ? name : ""; //$NON-NLS-1$
		return head;
	}

	/**
	 * Retrieves the {@link ObjectId} of the current HEAD.
	 *
	 * @param repository
	 * @return ObjectId of HEAD, or {@code null} if none
	 */
	public ObjectId getHead(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object value = items.get(RepositoryItem.HEAD);
		if (value == null) {
			String[] fullName = { null };
			Ref[] headRef = { null };
			value = items.computeIfAbsent(RepositoryItem.HEAD,
					key -> getHead(repository, fullName, headRef));
			items.computeIfAbsent(RepositoryItem.FULL_BRANCH_NAME,
					key -> fullName[0]);
			items.computeIfAbsent(RepositoryItem.HEAD_REF,
					key -> headRef[0] == null ? NOTHING : headRef[0]);
		}
		ObjectId head = (ObjectId) value;
		if (head == null || head.equals(ObjectId.zeroId())) {
			return null;
		}
		return head;
	}


	/**
	 * Retrieves the current HEAD ref.
	 *
	 * @param repository
	 * @return the HEAD ref, or {@code null} if none
	 */
	public Ref getHeadRef(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object value = items.get(RepositoryItem.HEAD_REF);
		if (value == null) {
			String[] fullName = { null };
			Ref[] headRef = { null };
			items.computeIfAbsent(RepositoryItem.HEAD,
					key -> getHead(repository, fullName, headRef));
			items.computeIfAbsent(RepositoryItem.FULL_BRANCH_NAME,
					key -> fullName[0]);
			value = items.computeIfAbsent(RepositoryItem.HEAD_REF,
					key -> headRef[0] == null ? NOTHING : headRef[0]);
		}
		if (value == null || value == NOTHING) {
			return null;
		}
		return (Ref) value;
	}

	/**
	 * Retrieves the full name of the current branch.
	 *
	 * @param repository
	 * @return the full branch name
	 */
	public String getFullBranchName(Repository repository) {
		if (repository == null) {
			return null;
		}
		Map<RepositoryItem, Object> items = getItems(repository);
		Object fullBranchName = items.get(RepositoryItem.FULL_BRANCH_NAME);
		if (fullBranchName == null) {
			String[] fullName = { null };
			Ref[] headRef = { null };
			items.computeIfAbsent(RepositoryItem.HEAD,
					key -> getHead(repository, fullName, headRef));
			fullBranchName = items.computeIfAbsent(
					RepositoryItem.FULL_BRANCH_NAME, key -> fullName[0]);
			items.computeIfAbsent(RepositoryItem.HEAD_REF,
					key -> headRef[0] == null ? NOTHING : headRef[0]);
		}
		String name = (String) fullBranchName;
		if (name == null || name.isEmpty()) {
			return null;
		}
		return name;
	}

	/**
	 * Retrieves the repository state.
	 *
	 * @param repository
	 * @return the {@link RepositoryState}
	 */
	public @NonNull RepositoryState getRepositoryState(Repository repository) {
		Object value = getItems(repository).computeIfAbsent(
				RepositoryItem.STATE, key -> repository.getRepositoryState());
		assert value != null; // Keep the compiler happy.
		return (RepositoryState) value;
	}

}
