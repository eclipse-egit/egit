/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.decorators;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.egit.gitflow.GitFlowConfig;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.decorators.DecoratorRepositoryStateCache;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;

/**
 * Supplies decorations for displayed repositories
 */
public class GitFlowLightweightDecorator extends LabelProvider implements
		ILightweightLabelDecorator {

	/**
	 * Property constant pointing back to the extension point id of the
	 * decorator
	 */
	public static final String DECORATOR_ID = "org.eclipse.egit.gitflow.ui.internal.decorators.GitflowLightweightDecorator"; //$NON-NLS-1$

	private ILog log;

	/** */
	public GitFlowLightweightDecorator() {
		log = Activator.getDefault().getLog();
	}

	/**
	 * This method should only be called by the decorator thread.
	 *
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object,
	 *      org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (!PlatformUI.isWorkbenchRunning() || Activator.getDefault() == null) {
			return;
		}

		GitFlowConfig config = null;
		if (element instanceof GitFlowRepository) {
			config = ((GitFlowRepository) element).getConfig();
		} else if (element instanceof RepositoryNode) {
			Repository repository = ((RepositoryNode) element).getRepository();
			if (repository != null) {
				config = new GitFlowConfig(
						DecoratorRepositoryStateCache.INSTANCE
								.getConfig(repository));
			}
		}
		try {
			if (config != null) {
				decorateRepository(config, decoration);
			}
		} catch (Exception e) {
			log.log(Activator.error(e.getMessage(), e));
		}
	}

	/**
	 * Decorates a single repository.
	 *
	 * @param config
	 *            of the repository to decorate
	 * @param decoration
	 *            the decoration
	 * @throws IOException
	 */
	private void decorateRepository(GitFlowConfig config,
			IDecoration decoration) throws IOException {
		ImageDescriptor overlay = null;

		if (config.isInitialized()) {
			overlay = UIIcons.OVR_GITFLOW;
		}

		// TODO: change decoration depending on branch type, e.g. "F"-icon
		// for feature branch

		// Overlays can only be added once, so do it at the end
		if (overlay != null) {
			decoration.addOverlay(overlay);
		}
	}
}
