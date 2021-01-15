/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.properties.ListPropertySource;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Read-only view of remote configuration
 */
public class RepositoryRemotePropertySource implements IPropertySource {

	private static final String PUSHURL = "pushurl"; //$NON-NLS-1$

	private static final String PUSH = "push"; //$NON-NLS-1$

	private static final String FETCH = "fetch"; //$NON-NLS-1$

	private final StoredConfig myConfig;

	private final String myName;

	private final IPropertyDescriptor[] descriptors;

	/**
	 * @param config
	 * @param remoteName
	 * @param page
	 *
	 */
	public RepositoryRemotePropertySource(StoredConfig config,
			String remoteName, PropertySheetPage page) {
		myConfig = config;
		myName = remoteName;
		List<IPropertyDescriptor> resultList = new ArrayList<>();
		PropertyDescriptor desc = new PropertyDescriptor(
				ConfigConstants.CONFIG_KEY_URL,
				UIText.RepositoryRemotePropertySource_RemoteFetchURL_label);
		resultList.add(desc);
		desc = new PropertyDescriptor(FETCH,
				UIText.RepositoryRemotePropertySource_FetchLabel);
		resultList.add(desc);
		desc = new PropertyDescriptor(PUSHURL,
				UIText.RepositoryRemotePropertySource_RemotePushUrl_label);
		resultList.add(desc);
		desc = new PropertyDescriptor(PUSH,
				UIText.RepositoryRemotePropertySource_PushLabel);
		resultList.add(desc);
		descriptors = resultList.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		try {
			myConfig.load();
		} catch (IOException | ConfigInvalidException e) {
			Activator.handleError(
					UIText.RepositoryRemotePropertySource_ErrorHeader, e, true);
		}
		return descriptors;
	}

	@Override
	public Object getPropertyValue(Object id) {
		String[] list = myConfig.getStringList(
				ConfigConstants.CONFIG_REMOTE_SECTION, myName, (String) id);
		if (list != null && list.length > 0) {
			return list.length > 1 ? new ListPropertySource(Arrays.asList(list))
					: list[0];
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		// no default values
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// nothing to do
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// read-only
	}

}
