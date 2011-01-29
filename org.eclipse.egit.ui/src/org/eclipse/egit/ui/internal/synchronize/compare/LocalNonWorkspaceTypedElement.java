/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.compare;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.LocalResourceTypedElement;

class LocalNonWorkspaceTypedElement extends LocalResourceTypedElement {

	private final String path;

	private static final IWorkspaceRoot ROOT = ResourcesPlugin.getWorkspace().getRoot();

	public LocalNonWorkspaceTypedElement(String path) {
		super(ROOT.getFile(new Path(path)));
		this.path = path;
	}

	@Override
	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(path);
		} catch (FileNotFoundException e) {
			Activator.error(e.getMessage(), e);
		}

		return null;
	}

}
