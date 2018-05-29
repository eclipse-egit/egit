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
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.ui.Activator;
import org.eclipse.egit.gitflow.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.Repository;
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
		// Don't decorate if UI plugin is not running
		if (Activator.getDefault() == null) {
			return;
		}

		// Don't decorate if the workbench is not running
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}


		final GitFlowRepository repository = getRepository(element);
		try {
			if (repository != null) {
				decorateRepository(repository, decoration);
			}
		} catch (Exception e) {
			handleException(repository, e);
		}
	}

	private static @Nullable GitFlowRepository getRepository(Object element) {
		GitFlowRepository repository = null;
		if (element instanceof GitFlowRepository) {
			repository = (GitFlowRepository) element;
		}

		if (element instanceof RepositoryNode) {
			RepositoryNode node = (RepositoryNode) element;
			Repository repo = node.getObject();
			if (repo != null) {
				repository = new GitFlowRepository(repo);
			}
		}

		return repository;
	}

	/**
	 * Decorates a single repository.
	 *
	 * @param repository
	 *            the repository to decorate
	 * @param decoration
	 *            the decoration
	 * @throws IOException
	 */
	private void decorateRepository(GitFlowRepository repository,
			IDecoration decoration) throws IOException {
		final DecorationHelper helper = new DecorationHelper();
		helper.decorate(decoration, repository);
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

		private final static ImageDescriptor INITIALIZED_IMAGE;

		static {
			INITIALIZED_IMAGE = new CachedImageDescriptor(UIIcons.OVR_GITFLOW);
		}

		/**
		 * Decorates the given <code>decoration</code> based on the state of the
		 * given <code>repository</code>.
		 *
		 * @param decoration
		 *            the decoration to decorate
		 * @param repository
		 *            the repository to retrieve state from
		 * @throws IOException
		 */
		public void decorate(IDecoration decoration, GitFlowRepository repository)
				throws IOException {
			decorateIcons(decoration, repository);
		}

		private void decorateIcons(IDecoration decoration,
				GitFlowRepository repository) throws IOException {
			ImageDescriptor overlay = null;

			if (repository.getConfig().isInitialized()) {
				overlay = INITIALIZED_IMAGE;
			}

			// TODO: change decoration depending on branch type, e.g. "F"-icon
			// for feature branch

			// Overlays can only be added once, so do it at the end
			decoration.addOverlay(overlay);
		}

	}

	/**
	 * Handle exceptions that occur in the decorator.
	 *
	 * @param repository
	 *            The repository that triggered the exception
	 * @param e
	 *            The exception that occurred
	 */
	private void handleException(GitFlowRepository repository, Exception e) {
		if (repository != null) {
			log.log(Activator.error(e.getMessage(), e));
		 }
	}
}
