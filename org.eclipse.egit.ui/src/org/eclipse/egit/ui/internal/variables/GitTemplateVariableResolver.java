/*******************************************************************************
 * Copyright (c) 2012, 2015 Kyle J. Harms <harmsk@seas.wustl.edu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.variables;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

/**
 * Resolves Git variables within templates
 */
public class GitTemplateVariableResolver extends TemplateVariableResolver {

	/**
	 * Creates a template variable resolver for Git variables
	 */
	public GitTemplateVariableResolver() {
		super("git_config", //$NON-NLS-1$
				UIText.GitTemplateVariableResolver_GitConfigDescription);
	}

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		GitTemplateVariableResolverHelper.resolve(variable, context);
	}
}
