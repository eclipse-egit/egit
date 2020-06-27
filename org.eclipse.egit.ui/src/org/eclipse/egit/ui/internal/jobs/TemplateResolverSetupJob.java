/*******************************************************************************
 * Copyright (C) 2020, Alex Blewitt <alex.blewitt@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.jobs;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.variables.GitTemplateVariableResolver;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Sets up the templates in JDT
 */
@SuppressWarnings("restriction")
@Component
public class TemplateResolverSetupJob extends WorkbenchJob {
	private static final long DELAY = 1000;

	/**
	 * Instantiated by DS from TemplateResolverSetupJob.xml
	 */
	public TemplateResolverSetupJob() {
		super(UIText.Activator_setupJdtTemplateResolver);
		setSystem(true);
		setUser(false);
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			final ContextTypeRegistry codeTemplateContextRegistry = JavaPlugin
					.getDefault().getCodeTemplateContextRegistry();
			final Iterator<?> ctIter = codeTemplateContextRegistry
					.contextTypes();

			while (ctIter.hasNext()) {
				final TemplateContextType contextType = (TemplateContextType) ctIter
						.next();
				contextType.addResolver(new GitTemplateVariableResolver(
						"git_config", //$NON-NLS-1$
						UIText.GitTemplateVariableResolver_GitConfigDescription));
			}
		} catch (Throwable e) {
			// while catching Throwable is an anti-pattern, we may
			// experience NoClassDefFoundErrors here
			Activator.logError("Cannot register git support for Java templates", //$NON-NLS-1$
					e);
		}
		return Status.OK_STATUS;
	}

	private static final boolean hasJavaPlugin() {
		return Platform.getBundle("org.eclipse.jdt.ui") != null; //$NON-NLS-1$
	}

	@Activate
	void start() {
		if (hasJavaPlugin()) {
			schedule(DELAY);
		}
	}

	@Deactivate
	void stop() {
		cancel();
	}
}