/*******************************************************************************
 * Copyright (C) 2021, Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository.tree.command;

import java.util.Collection;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Compares the working tree against HEAD.
 */
public class CompareWithHeadCommand extends CompareWithCommand {

	@Override
	protected String getRef(ExecutionEvent event, @NonNull Repository repository,
			Collection<IPath> paths) {
		return Constants.HEAD;
	}
}
