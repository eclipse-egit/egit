/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.egit.core.JobFamilies;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.selection.SelectionRepositoryStateCache;
import org.eclipse.egit.ui.internal.variables.GitTemplateVariableResolver;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Initializes some EGit components that rely on the workbench having been
 * created.
 */
@Component(property = EventConstants.EVENT_TOPIC + '='
		+ UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class StartEventListener implements EventHandler {

	private final AtomicBoolean started = new AtomicBoolean();

	// Perform expensive operations in this job.
	private Job backgroundWork = new Job(
			UIText.ConfigurationChecker_checkConfiguration) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ConfigurationChecker.checkConfiguration();
			return Status.OK_STATUS;
		}
	};

	private void startInternalComponents() {
		if (started.compareAndSet(false, true)) {
			SelectionRepositoryStateCache.INSTANCE.initialize();
			registerCoreJobFamilyIcons();
			registerTemplateVariableResolvers();
			backgroundWork.setSystem(true);
			backgroundWork.setUser(false);
			backgroundWork.schedule();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (UIEvents.UILifeCycle.APP_STARTUP_COMPLETE
				.equals(event.getTopic())) {
			startInternalComponents();
		}
	}

	@Deactivate
	void shutDown() {
		if (started.get()) {
			SelectionRepositoryStateCache.INSTANCE.dispose();
			backgroundWork.cancel();
			try {
				backgroundWork.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void runAsync(Runnable action) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(() -> {
				if (!display.isDisposed() && PlatformUI.isWorkbenchRunning()) {
					action.run();
				}
			});
		}
	}

	private void registerCoreJobFamilyIcons() {
		runAsync(() -> {
			IProgressService service = PlatformUI.getWorkbench()
					.getProgressService();
			if (service == null) {
				return;
			}
			service.registerIconForFamily(UIIcons.PULL, JobFamilies.PULL);
			service.registerIconForFamily(UIIcons.REPOSITORY,
					JobFamilies.AUTO_IGNORE);
			service.registerIconForFamily(UIIcons.REPOSITORY,
					JobFamilies.AUTO_SHARE);
			service.registerIconForFamily(UIIcons.REPOSITORY,
					JobFamilies.INDEX_DIFF_CACHE_UPDATE);
			service.registerIconForFamily(UIIcons.REPOSITORY,
					JobFamilies.REPOSITORY_CHANGED);
		});
	}

	private void registerTemplateVariableResolvers() {
		if (!Activator.hasJavaPlugin()) {
			return;
		}
		runAsync(() -> {
			try {
				ContextTypeRegistry codeTemplateContextRegistry = org.eclipse.jdt.internal.ui.JavaPlugin
						.getDefault().getCodeTemplateContextRegistry();
				Iterator<?> ctIter = codeTemplateContextRegistry.contextTypes();

				while (ctIter.hasNext()) {
					TemplateContextType contextType = (TemplateContextType) ctIter
							.next();
					contextType.addResolver(new GitTemplateVariableResolver(
							"git_config", //$NON-NLS-1$
							UIText.GitTemplateVariableResolver_GitConfigDescription));
				}
			} catch (Throwable e) {
				// while catching Throwable is an anti-pattern, we may
				// experience NoClassDefFoundErrors here
				Activator.logError(
						"Cannot register git support for Java templates", //$NON-NLS-1$
						e);
			}
		});
	}
}
