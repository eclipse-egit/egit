/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.relengtools.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.releng.tools.IRepositoryProviderCopyrightAdapterFactory;
import org.eclipse.releng.tools.RepositoryProviderCopyrightAdapter;
import org.eclipse.team.core.RepositoryProviderType;

@SuppressWarnings("rawtypes")
public class GitCopyrightAdapterFactory implements IAdapterFactory,
		IRepositoryProviderCopyrightAdapterFactory {

	private static final Class[] ADAPTER_LIST = new Class[] { IRepositoryProviderCopyrightAdapterFactory.class };

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (IRepositoryProviderCopyrightAdapterFactory.class
				.equals(adapterType)) {
			return getCvsCopyrightAdapter(adaptableObject);
		}
		return null;
	}

	private Object getCvsCopyrightAdapter(Object adaptableObject) {
		if (!(adaptableObject instanceof RepositoryProviderType))
			return null;
		return this;
	}

	@Override
	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

	@Override
	public RepositoryProviderCopyrightAdapter createAdapater(
			IResource[] resources) {
		return new GitCopyrightAdapter(resources);
	}

}
