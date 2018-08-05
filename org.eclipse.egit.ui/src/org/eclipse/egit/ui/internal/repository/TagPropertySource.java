/*******************************************************************************
 * Copyright (c) 2018, 2021 Michael Keppler and others.
 *
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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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

	private static final String PROPERTY_TAG_NAME = "tag_name"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_OBJECT_ID = "tag_id"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_OBJECT_TYPE = "tag_type"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_TAGGER = "tag_tagger"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_MESSAGE = "tag_message"; //$NON-NLS-1$

	private static final String PROPERTY_COMMIT_MESSAGE = "commit_message"; //$NON-NLS-1$

	private static final String PROPERTY_COMMIT_ID = "commit_id"; //$NON-NLS-1$

	private RevTag tag;

	private RevCommit commit;

	private final String name;

	private final IPropertyDescriptor[] descriptors;

	/**
	 * Create a property source for a tag.
	 *
	 * @param repo
	 *            repository
	 * @param ref
	 *            ref of the tag
	 */
	public TagPropertySource(Repository repo, Ref ref) {
		try (RevWalk refWalk = new RevWalk(repo)) {
			ObjectId objectId = repo.resolve(ref.getName());
			RevObject any = refWalk.parseAny(objectId);
			RevObject peeledObject = refWalk.peel(any);
			if (any instanceof RevTag) {
				tag = (RevTag) any;
			}
			if (peeledObject instanceof RevCommit) {
				commit = (RevCommit) peeledObject;
			}
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		name = tag != null ? tag.getTagName() : Repository.shortenRefName(ref.getName());
		String category;
		List<PropertyDescriptor> result = new ArrayList<>();
		result.add(new PropertyDescriptor(PROPERTY_TAG_NAME,
				UIText.TagPropertySource_TagName));
		if (tag == null) {
			category = UIText.TagPropertySource_LightweightTagCategory;
			if (commit != null) {
				result.add(new PropertyDescriptor(PROPERTY_COMMIT_ID,
						UIText.TagPropertySource_CommitId));
				result.add(new PropertyDescriptor(PROPERTY_COMMIT_MESSAGE,
						UIText.TagPropertySource_CommitMessage));
			}
		} else {
			category = UIText.TagPropertySource_TagCategory;
			result.add(new PropertyDescriptor(PROPERTY_TAG_OBJECT_ID, UIText.TagPropertySource_TagObjectId));
			result.add(new PropertyDescriptor(PROPERTY_TAG_OBJECT_TYPE, UIText.TagPropertySource_TagObjectType));
			result.add(new PropertyDescriptor(PROPERTY_TAG_TAGGER, UIText.TagPropertySource_TagTagger));
			result.add(new PropertyDescriptor(PROPERTY_TAG_MESSAGE, UIText.TagPropertySource_TagMessage));
		}
		for (PropertyDescriptor p : result) {
			p.setCategory(category);
		}
		if (tag != null && commit != null) {
			PropertyDescriptor desc = new PropertyDescriptor(PROPERTY_COMMIT_ID, UIText.TagPropertySource_CommitId);
			desc.setCategory(UIText.TagPropertySource_CommitCategory);
			result.add(desc);
			desc = new PropertyDescriptor(PROPERTY_COMMIT_MESSAGE, UIText.TagPropertySource_CommitMessage);
			desc.setCategory(UIText.TagPropertySource_CommitCategory);
			result.add(desc);
		}
		descriptors = result.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return descriptors;
	}

	@Override
	public Object getPropertyValue(Object id) {
		switch (id.toString()) {
		case PROPERTY_TAG_NAME:
			return name;
		case PROPERTY_TAG_OBJECT_ID:
			return tag.getObject().name();
		case PROPERTY_TAG_OBJECT_TYPE:
			return Constants.typeString(tag.getObject().getType());
		case PROPERTY_TAG_TAGGER:
			return new PersonIdentPropertySource(tag.getTaggerIdent());
		case PROPERTY_TAG_MESSAGE:
			// TODO: figure out a way to show the full message, if different
			// from the short message.
			return tag.getShortMessage();
		case PROPERTY_COMMIT_ID:
			return commit.name();
		case PROPERTY_COMMIT_MESSAGE:
			return commit.getShortMessage();
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

}
