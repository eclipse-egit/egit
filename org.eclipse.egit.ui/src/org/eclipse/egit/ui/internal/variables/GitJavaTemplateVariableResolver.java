/*******************************************************************************
 * Copyright (c) 2012, 2015 Kyle J. Harms <harmsk@seas.wustl.edu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.variables;

/**
 * Resolves Git variables within Java templates
 */
public class GitJavaTemplateVariableResolver extends org.eclipse.jface.text.templates.TemplateVariableResolver {
	@Override
	public void resolve(org.eclipse.jface.text.templates.TemplateVariable variable, org.eclipse.jface.text.templates.TemplateContext context) {
		GitTemplateVariableResolverHelper.resolve(variable, context);
	}
}
