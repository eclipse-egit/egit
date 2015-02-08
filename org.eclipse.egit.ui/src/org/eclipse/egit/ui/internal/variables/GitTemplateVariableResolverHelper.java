/*******************************************************************************
 * Copyright (c) 2012, 2015 Kyle J. Harms <harmsk@seas.wustl.edu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.variables;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * Retrieves template variables from Git config
 */
public class GitTemplateVariableResolverHelper {

	/**
	 * Resolves the git_config variable
	 *
	 * @param variable
	 *            the current template variable.
	 * @param context
	 *            the current template context.
	 */
	public static void resolve(TemplateVariable variable,
			TemplateContext context) {
		IProject project = getProject(context);
		if (project != null) {
			resolve(variable, project);
		}
	}

	/**
	 * Resolves the git_config variable
	 *
	 * @param variable
	 *            the current template variable.
	 * @param project
	 *            the current project.
	 */
	public static void resolve(TemplateVariable variable, IProject project) {
		final List<String> params = variable.getVariableType().getParams();
		if (params.isEmpty()) {
			return;
		}

		final String gitKey = params.get(0);
		if (gitKey == null || gitKey.length() == 0) {
			return;
		}

		// Get git's config
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		Repository repository = null;

		if (mapping != null) {
			repository = mapping.getRepository();
		}
		if (repository == null) {
			return;
		}

		StoredConfig config = repository.getConfig();
		if (config == null) {
			return;
		}

		// Get the value of the key
		final String[] splits = gitKey.split("\\."); //$NON-NLS-1$
		String section = null;
		String subSection = null;
		String name = null;

		if (splits.length == 3) {
			section = splits[0];
			subSection = splits[1];
			name = splits[2];
		} else if (splits.length == 2) {
			section = splits[0];
			name = splits[1];
		} else {
			return;
		}

		String gitValue = config.getString(section, subSection, name);
		if (gitValue != null) {
			variable.setValue(gitValue);
		}
	}

	/**
	 * Retrieves the current project from a template context.
	 *
	 * @param context
	 *            the current template context.
	 * @return the current project
	 */
	public static IProject getProject(TemplateContext context) {
		IProject project = null;
		if (context instanceof CodeTemplateContext) {
			project = ((CodeTemplateContext) context).getJavaProject()
					.getProject();
		}
		return project;
	}
}
