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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.egit.ui.internal.RepositoryStateCache;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
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
public class SelectionRepositoryStateCache extends RepositoryStateCache {

	/** The singleton instance of the {@link SelectionRepositoryStateCache}. */
	public static final SelectionRepositoryStateCache INSTANCE = new SelectionRepositoryStateCache();

	private final AtomicBoolean stopped = new AtomicBoolean();

	private SelectionRepositoryStateCache() {
		// No creation from outside
	}

	@Override
	public void initialize() {
		// Clear the cache whenever the selection changes.
		IEclipseContext applicationContext = PlatformUI.getWorkbench()
				.getService(IEclipseContext.class);
		// A RunAndTrack on the workbench context runs *before* any E3 or E4
		// selection listener on the selection service, which is how expression
		// re-evaluations for expressions based on the current selection are
		// triggered. So we can ensure here that re-evaluations don't use stale
		// cached values.
		applicationContext
				.runAndTrack(new ContextListener(stopped, this::clear));
	}

	@Override
	public void dispose() {
		stopped.set(true);
		super.dispose();
	}

	private static class ContextListener extends RunAndTrack {

		private AtomicBoolean stopped;

		private final Runnable clearCache;

		ContextListener(AtomicBoolean stopped, Runnable clearCache) {
			super();
			this.stopped = stopped;
			this.clearCache = clearCache;
		}

		private Object lastSelection;

		private Object lastMenuSelection;

		@Override
		public boolean changed(IEclipseContext context) {
			if (stopped.get()) {
				clearCache.run();
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
				clearCache.run();
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
}
