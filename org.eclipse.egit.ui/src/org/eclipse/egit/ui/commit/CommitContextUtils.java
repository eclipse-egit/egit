/*******************************************************************************
 * Copyright (C) 2025, Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.commit;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Utility operations on {@link CommitContext}s.
 *
 * @since 7.4
 */
public final class CommitContextUtils {

	private CommitContextUtils() {
		// No instantiation
	}

	/**
	 * Obtains the {@link CommitContext} from an {@link ExecutionEvent}. Useful
	 * in command handlers to get the context.
	 *
	 * @param event
	 *            {@link ExecutionEvent} of the command invocation
	 * @return the {@link CommitContext}, or {@code null} if none is found
	 */
	public static CommitContext getCommitContext(ExecutionEvent event) {
		CommitContext context = null;
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part != null) {
			context = Adapters.adapt(part, CommitContext.class);
			// If non-null now, we're in the staging view.
		}
		if (context == null) {
			// In the commit dialog?
			Shell shell = HandlerUtil.getActiveShell(event);
			context = Adapters.adapt(
					shell.getData(CommitContext.class.getCanonicalName()),
					CommitContext.class);
		}
		return context;
	}
}
