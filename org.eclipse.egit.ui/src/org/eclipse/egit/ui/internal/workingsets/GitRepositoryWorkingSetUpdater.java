/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel <lars.vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.workingsets;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;

/**
 * Marks repository working sets as self-updating. The content is maintained by
 * {@link GitRepositoryWorkingSets}, which this updater starts as soon as the
 * workbench restores or adds such a working set.
 */
public class GitRepositoryWorkingSetUpdater implements IWorkingSetUpdater {

	private final Set<IWorkingSet> workingSets = ConcurrentHashMap.newKeySet();

	@Override
	public void add(IWorkingSet workingSet) {
		workingSets.add(workingSet);
		GitRepositoryWorkingSets.getInstance().install();
	}

	@Override
	public boolean remove(IWorkingSet workingSet) {
		return workingSets.remove(workingSet);
	}

	@Override
	public boolean contains(IWorkingSet workingSet) {
		return workingSets.contains(workingSet);
	}

	@Override
	public void dispose() {
		workingSets.clear();
	}
}
