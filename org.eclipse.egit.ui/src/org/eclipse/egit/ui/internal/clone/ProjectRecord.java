package org.eclipse.egit.ui.internal.clone;

/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * Copyright (C) 2007, Martin Oberhuber (martin.oberhuber@windriver.com)
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 * Copyright (C) 2010, Wim Jongman <wim.jongman@remainsoftware.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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
		setProjectName();
	}

	/**
	 * Set the name of the project based on the projectFile.
	 */
	private void setProjectName() {
		try {
			// If we don't have the project name try again
			if (projectName == null) {
				IPath path = new Path(projectSystemFile.getPath());
				// if the file is in the default location, use the directory
				// name as the project name
				if (isDefaultLocation(path)) {
					projectName = path.segment(path.segmentCount() - 2);
					description = ResourcesPlugin.getWorkspace()
							.newProjectDescription(projectName);
				} else {
					description = ResourcesPlugin.getWorkspace()
							.loadProjectDescription(path);
					projectName = description.getName();
				}

			}
		} catch (CoreException e) {
			// no good couldn't get the name
		}
	}

	/**
	 * Returns whether the given project description file path is in the
	 * default location for a project
	 *
	 * @param path
	 *            The path to examine
	 * @return Whether the given path is the default location for a project
	 */
	private boolean isDefaultLocation(IPath path) {
		// The project description file must at least be within the project,
		// which is within the workspace location
		if (path.segmentCount() < 2)
			return false;
		return path.removeLastSegments(2).toFile().equals(
				Platform.getLocation().toFile());
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
	 * @since 3.4
	 */
	public String getProjectLabel() {
		if (description == null)
			return projectName;

		String path = projectSystemFile.getParent();

		return NLS.bind(UIText.WizardProjectsImportPage_projectLabel,
				projectName, path);
	}

	@Override
	public String toString() {
		return projectName;
	}
}