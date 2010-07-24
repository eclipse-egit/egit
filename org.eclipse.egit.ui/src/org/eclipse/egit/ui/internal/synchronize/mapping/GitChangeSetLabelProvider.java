/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelBlob;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelRepository;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelTree;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Label provider for Git ChangeSet model.
 */
public class GitChangeSetLabelProvider extends SynchronizationLabelProvider {

	private static final ILabelProvider workbenchLabelProvider = WorkbenchLabelProvider
			.getDecoratingWorkbenchLabelProvider();

	private LabelProvider delegateLabelProvider;

	@Override
	protected ILabelProvider getDelegateLabelProvider() {
		if (delegateLabelProvider == null)
			delegateLabelProvider = new DelegateLabelProvider();

		return delegateLabelProvider;
	}

	private static class DelegateLabelProvider extends LabelProvider {

		private final ResourceManager fImageCache = new LocalResourceManager(
				JFaceResources.getResources());

		public String getText(Object element) {
			if (element instanceof GitModelObject)
				return ((GitModelObject) element).getName();

			return null;
		}

		public Image getImage(Object element) {
			if (element instanceof GitModelBlob) {
				Object adapter = ((GitModelBlob) element)
						.getAdapter(IResource.class);
				return workbenchLabelProvider.getImage(adapter);
			}

			if (element instanceof GitModelTree) {
				Object adapter = ((GitModelTree) element)
						.getAdapter(IResource.class);
				return workbenchLabelProvider.getImage(adapter);
			}

			if (element instanceof GitModelCommit)
				return fImageCache.createImage(UIIcons.PUSH);

			if (element instanceof GitModelRepository)
				return fImageCache.createImage(UIIcons.REPOSITORY);

			return super.getImage(element);
		}

		@Override
		public void dispose() {
			fImageCache.dispose();
			super.dispose();
		}
	}

}
