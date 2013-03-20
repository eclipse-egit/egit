/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.internal.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.internal.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelObject;

/**
 * Represents the provider of Git logical model.
 */
public class GitChangeSetModelProvider extends ModelProvider {

	/**
	 * Id of model provider
	 */
	public static final String ID = "org.eclipse.egit.ui.changeSetModel"; //$NON-NLS-1$

	private static GitChangeSetModelProvider provider;

	/**
	 * @return provider instance or <code>null</code> if provider was not
	 *         initialized
	 */
	public static GitChangeSetModelProvider getProvider() {
		if (provider == null) {
			ModelProvider modelProvider;
			try {
				modelProvider = ModelProvider.getModelProviderDescriptor(ID)
						.getModelProvider();
				provider = (GitChangeSetModelProvider) modelProvider;
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}

		return provider;
	}

	@Override
	public ResourceMapping[] getMappings(IResource resource,
			ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {
		if (context instanceof GitSubscriberResourceMappingContext) {
			GitSubscriberResourceMappingContext gitContext = (GitSubscriberResourceMappingContext) context;
			GitSynchronizeDataSet gsds = gitContext.getSyncData();
			GitSynchronizeData data = gsds.getData(resource.getProject());

			if (data != null) {
				GitModelObject object = null;
				try {
					object = GitModelObject.createRoot(data);
				} catch (IOException e) {
					Activator.logError(e.getMessage(), e);
				}

				if (object != null) {
					ResourceMapping rm = (ResourceMapping) object
							.getAdapter(ResourceMapping.class);
					return new ResourceMapping[] { rm };
				}
			}
		}

		return super.getMappings(resource, context, monitor);
	}
}
