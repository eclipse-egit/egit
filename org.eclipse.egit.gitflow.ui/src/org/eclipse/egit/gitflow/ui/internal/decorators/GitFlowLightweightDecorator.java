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
import org.eclipse.swt.graphics.ImageData;
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
			config = new GitFlowConfig(DecoratorRepositoryStateCache.INSTANCE
					.getConfig(((RepositoryNode) element).getRepository()));
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
		final DecorationHelper helper = new DecorationHelper();
		helper.decorate(decoration, config);
	}


	/**
	 * Helper class for doing repository decoration
	 *
	 * Used for real-time decoration, as well as in the decorator preview
	 * preferences page
	 */
	private static class DecorationHelper {

		/**
		 * Define a cached image descriptor which only creates the image data
		 * once
		 */
		private static class CachedImageDescriptor extends ImageDescriptor {
			private ImageDescriptor descriptor;

			private ImageData data;

			public CachedImageDescriptor(ImageDescriptor descriptor) {
				this.descriptor = descriptor;
			}

			@Override
			public ImageData getImageData() {
				if (data == null) {
					data = descriptor.getImageData();
				}
				return data;
			}
		}

		private final static ImageDescriptor INITIALIZED_IMAGE = new CachedImageDescriptor(
				UIIcons.OVR_GITFLOW);

		/**
		 * Decorates the given <code>decoration</code> based on the state of the
		 * given <code>repository</code>.
		 *
		 * @param decoration
		 *            the decoration to decorate
		 * @param config
		 *            the config to retrieve state from
		 * @throws IOException
		 */
		public void decorate(IDecoration decoration, GitFlowConfig config)
				throws IOException {
			decorateIcons(decoration, config);
		}

		private void decorateIcons(IDecoration decoration,
				GitFlowConfig config) {
			ImageDescriptor overlay = null;

			if (config.isInitialized()) {
				overlay = INITIALIZED_IMAGE;
			}

			// TODO: change decoration depending on branch type, e.g. "F"-icon
			// for feature branch

			// Overlays can only be added once, so do it at the end
			if (overlay != null) {
				decoration.addOverlay(overlay);
			}
		}

	}
}
