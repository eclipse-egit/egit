/*******************************************************************************
 * Copyright (c) 2018 Michael Keppler
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Michael Keppler - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * Properties for a tag (read-only).
 */
public class TagPropertySource implements IPropertySource {

	private static final String PROPERTY_MESSAGE_ID = "message"; //$NON-NLS-1$

	private static final String PROPERTY_NAME_ID = "name"; //$NON-NLS-1$

	private RevTag tag = null;

	private Ref ref;

	/**
	 * Create a property source for a tag.
	 *
	 * @param repo
	 *            repository
	 * @param ref
	 *            ref of the tag
	 */
	public TagPropertySource(Repository repo, Ref ref) {
		this.ref = ref;
		RevWalk refWalk = new RevWalk(repo);
		try {
			RevObject any = refWalk.parseAny(repo.resolve(ref.getName()));
			if (any instanceof RevTag) {
				tag = (RevTag) any;
			}
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		finally {
			refWalk.close();
		}
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> resultList = new ArrayList<>();

		PropertyDescriptor desc = new PropertyDescriptor(PROPERTY_NAME_ID,
				UIText.TagPropertySource_TagPropertyName);
		desc.setCategory(UIText.TagPropertySource_TagPropertyCategory);
		resultList.add(desc);
		desc = new PropertyDescriptor(PROPERTY_MESSAGE_ID,
				UIText.TagPropertySource_TagPropertyMessage);
		desc.setCategory(UIText.TagPropertySource_TagPropertyCategory);
		resultList.add(desc);

		return resultList.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (PROPERTY_NAME_ID.equals(id)) {
			if (tag != null) {
				return tag.getTagName();
			}
			return ref.getName().substring(Constants.R_TAGS.length());
		}
		if (PROPERTY_MESSAGE_ID.equals(id)) {
			if (tag != null) {
			return tag.getShortMessage();
			}
			return UIText.TagPropertySource_TagPropertyNoMessage;
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// read only
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// read only
	}

}
