/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jgit.lib.Repository;

abstract class AbstractRevObjectOperation implements IWorkspaceRunnable {

	Repository repository;

	AbstractRevObjectOperation(final Repository repository) {
		this.repository = repository;
	}

}
