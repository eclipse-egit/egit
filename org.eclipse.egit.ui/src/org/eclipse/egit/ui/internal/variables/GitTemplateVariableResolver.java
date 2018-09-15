/*******************************************************************************
 * Copyright (c) 2012, 2015 Kyle J. Harms <harmsk@seas.wustl.edu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.variables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * Resolves Git variables within templates
 *
 * @see <a
 *      href="http://help.eclipse.org/topic/org.eclipse.jdt.doc.user/concepts/concept-template-variables.htm">http://help.eclipse.org/topic/org.eclipse.jdt.doc.user/concepts/concept-template-variables.htm</a>
 */
public class GitTemplateVariableResolver extends TemplateVariableResolver {

	/**
	 * Creates an instance of <code>GitTemplateVariableResolver</code>.
	 *
	 * @param type
	 *            the name of the type
	 * @param description
	 *            the description for the type
	 */
	public GitTemplateVariableResolver(String type, String description) {
		super(type, description);
	}

	/**
	 * Creates an instance of <code>GitTemplateVariableResolver</code>.
	 */
	public GitTemplateVariableResolver() {
		super();
	}

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		resolveVariable(variable, context);
	}

	/**
	 * Resolves the git_config variable for a project
	 *
	 * @param variable
	 *            the current template variable.
	 * @param project
	 *            the current project.
	 */
	protected static void resolveVariable(TemplateVariable variable,
			IProject project) {
		final List<String> params = variable.getVariableType().getParams();
		if (params.isEmpty()) {
			variable.setValue(""); //$NON-NLS-1$
			return;
		}

		final String gitKey = params.get(0);
		if (gitKey == null || gitKey.length() == 0) {
			variable.setValue(""); //$NON-NLS-1$
			return;
		}

		// Get git's config
		RepositoryMapping mapping = RepositoryMapping.getMapping(project);
		Repository repository = null;

		if (mapping != null) {
			repository = mapping.getRepository();
		}
		if (repository == null) {
			variable.setValue(""); //$NON-NLS-1$
			return;
		}

		StoredConfig config = repository.getConfig();

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
			variable.setValue(""); //$NON-NLS-1$
			return;
		}

		String gitValue = config.getString(section, subSection, name);
		if (gitValue != null) {
			variable.setValue(gitValue);
		}
	}

	/**
	 * Resolves the git_config variable
	 *
	 * @param variable
	 *            the current template variable.
	 * @param context
	 *            the current template context.
	 */
	protected static void resolveVariable(TemplateVariable variable,
			TemplateContext context) {
		IProject project = getProject(context);
		if (project != null) {
			resolveVariable(variable, project);
		}
	}

	/**
	 * Retrieves the current project from a template context.
	 *
	 * @param context
	 *            the current template context.
	 * @return the current project
	 */
	@Nullable
	protected static IProject getProject(TemplateContext context) {
		// We can't use instanceof here because of the compiler error on 4.10
		// platform saying that TemplateContext is always IAdaptable
		if (IAdaptable.class.isInstance(context)) {
			return AdapterUtils.adapt(context, IProject.class);
		}
		// Note: block below can be removed after EGit minimum target platform
		// will be 4.10, see bug 539095 for details
		if (Activator.hasJavaPlugin()) {
			boolean hasPublicMethod = context.getClass().getSimpleName()
					.equals("CodeTemplateContext"); //$NON-NLS-1$
			try {
				Method method;
				if (hasPublicMethod) {
					// CodeTemplateContext has public getJavaProject() method
					method = context.getClass().getMethod("getJavaProject"); //$NON-NLS-1$
				} else {
					// JavaContext inherits from CompilationUnitContext which
					// has protected getJavaProject() method
					method = context.getClass().getSuperclass()
							.getDeclaredMethod("getJavaProject"); //$NON-NLS-1$
					method.setAccessible(true);
				}
				Object result = method.invoke(context);
				if (result instanceof IJavaProject) {
					IJavaProject javaProject = (IJavaProject) result;
					return javaProject.getProject();
				}
			} catch (NoSuchMethodException | SecurityException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				return null;
			}
		}
		return null;
	}
}
