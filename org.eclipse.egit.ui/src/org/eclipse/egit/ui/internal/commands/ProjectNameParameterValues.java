/*******************************************************************************
 * Copyright (C) 2009, Mykola Nikishov <mn@mn.com.ua>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commands;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.team.core.RepositoryProvider;

/**
 * Provides list of accessible and non-shared projects' names for the Share
 * Project command.
 *
 * @since 0.6.0
 */
public class ProjectNameParameterValues implements IParameterValues {

	@Override
	public Map getParameterValues() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		return Stream.of(projects)
				.parallel()
				.filter(IProject::isAccessible)
				.filter(project -> RepositoryProvider
						.getProvider(project) == null)
				.collect(
						Collectors.toMap(IProject::getName, IProject::getName));
	}

}
