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

/**
 * Resolves Git variables within templates
 */
public class GitTemplateVariableResolver extends org.eclipse.jface.text.templates.TemplateVariableResolver {

	/**
	 * Creates a template variable resolver for Git variables
	 */
	public GitTemplateVariableResolver() {
		super("git_config", //$NON-NLS-1$
				UIText.GitTemplateVariableResolver_GitConfigDescription);
	}

	@Override
	public void resolve(org.eclipse.jface.text.templates.TemplateVariable variable, org.eclipse.jface.text.templates.TemplateContext context) {
		GitTemplateVariableResolverHelper.resolve(variable, context);
	}
}
