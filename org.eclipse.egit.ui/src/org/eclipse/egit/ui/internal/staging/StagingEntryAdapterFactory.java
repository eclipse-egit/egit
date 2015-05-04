/*******************************************************************************
 * Copyright (C) 2011, 2015 Bernard Leach <leachbj@bouncycastle.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.ui.internal.CommonUtils;

/**
 * An adapter factory for <code>StagingEntry</code>s so that the property page
 * handler can open property pages on them correctly.
 */
public class StagingEntryAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		return CommonUtils.getAdapter(((StagingEntry)adaptableObject), IResource.class);
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IResource.class };
	}

}
