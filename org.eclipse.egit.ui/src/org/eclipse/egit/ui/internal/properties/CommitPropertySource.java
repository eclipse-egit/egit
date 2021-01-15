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
package org.eclipse.egit.ui.internal.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Properties for a commit (read-only).
 */
public class CommitPropertySource implements IPropertySource {

	private static final String PROPERTY_COMMIT_MESSAGE = "commit_message"; //$NON-NLS-1$

	private static final String PROPERTY_COMMIT_ID = "commit_id"; //$NON-NLS-1$

	private static final String PROPERTY_COMMIT_AUTHOR = "commit_author"; //$NON-NLS-1$

	private static final String PROPERTY_COMMITTER = "committer"; //$NON-NLS-1$

	private RevCommit commit;

	private final IPropertyDescriptor[] descriptors;

	/**
	 * Create a property source for a tag.
	 *
	 * @param commit
	 *            to show
	 * @param page
	 *            to show the commit in
	 */
	public CommitPropertySource(RevCommit commit, PropertySheetPage page) {
		this.commit = commit;
		List<PropertyDescriptor> result = new ArrayList<>();
		result.add(new PropertyDescriptor(PROPERTY_COMMIT_ID,
				UIText.CommitPropertySource_CommitId));
		result.add(new MessagePropertyDescriptor(PROPERTY_COMMIT_MESSAGE,
				UIText.CommitPropertySource_CommitMessage,
				commit.getFullMessage(), page));
		result.add(new PropertyDescriptor(PROPERTY_COMMIT_AUTHOR,
				UIText.CommitPropertySource_CommitAuthor));
		result.add(new PropertyDescriptor(PROPERTY_COMMITTER,
				UIText.CommitPropertySource_Committer));
		for (PropertyDescriptor p : result) {
			p.setCategory(UIText.CommitPropertySource_CommitCategory);
		}
		descriptors = result.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return descriptors;
	}

	@Override
	public Object getPropertyValue(Object id) {
		switch (id.toString()) {
		case PROPERTY_COMMIT_ID:
			return commit.getName();
		case PROPERTY_COMMIT_MESSAGE:
			return commit.getShortMessage();
		case PROPERTY_COMMIT_AUTHOR:
			return new PersonIdentPropertySource(commit.getAuthorIdent());
		case PROPERTY_COMMITTER:
			return new PersonIdentPropertySource(commit.getCommitterIdent());
		default:
			return null;
		}
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

	@Override
	public String toString() {
		return Constants.TYPE_COMMIT + ' ' + commit.name();
	}
}
