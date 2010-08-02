/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * Copyright (C) 2007, Martin Oberhuber (martin.oberhuber@windriver.com)
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2010, Wim Jongman <wim.jongman@remainsoftware.com>
 * Copyright (C) 2010, Benjamin Muskalla <bmuskalla@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.clone;

import java.io.File;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.ui.UIText;
import org.eclipse.osgi.util.NLS;

class ProjectRecord {
	File projectSystemFile;

	String projectName;

	IProjectDescription description;

	/**
	 * Create a record for a project based on the info in the file.
	 *
	 * @param file
	 */
	ProjectRecord(File file) {
		projectSystemFile = file;
		initProjectDescription();
	}

	/**
	 * Set the name of the project based on the projectFile.
	 */
	private void initProjectDescription() {
		try {
				IPath path = new Path(projectSystemFile.getPath());
				description = ResourcesPlugin.getWorkspace()
						.loadProjectDescription(path);
				projectName = description.getName();
		} catch (CoreException e) {
			// no good couldn't get the name
		}
	}

	/**
	 * Get the name of the project
	 *
	 * @return String
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Gets the label to be used when rendering this project record in the
	 * UI.
	 *
	 * @return String the label
	 */
	public String getProjectLabel() {
		String path = projectSystemFile.getParent();

		return NLS.bind(UIText.WizardProjectsImportPage_projectLabel,
				projectName, path);
	}

	@Override
	public String toString() {
		return projectName;
	}
}