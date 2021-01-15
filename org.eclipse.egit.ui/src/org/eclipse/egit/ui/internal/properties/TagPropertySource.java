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
package org.eclipse.egit.ui.internal.properties;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
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
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Properties for a tag (read-only).
 */
public class TagPropertySource implements IPropertySource {

	private static final String PROPERTY_TAG_ID = "tag_id"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_MESSAGE = "tag_message"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_NAME = "tag_name"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_TAGGER = "tag_tagger"; //$NON-NLS-1$

	private static final String PROPERTY_TAG_TARGET = "tag_target"; //$NON-NLS-1$

	private RevTag tag;

	private RevCommit commit;

	private Repository repository;

	private final String name;

	private final IPropertyDescriptor[] descriptors;

	private final PropertySheetPage page;

	/**
	 * Create a property source for a tag from a {@link Ref}.
	 *
	 * @param repo
	 *            repository
	 * @param ref
	 *            ref of the tag
	 * @param page
	 *            to show the property in
	 */
	public TagPropertySource(Repository repo, Ref ref, PropertySheetPage page) {
		this.repository = repo;
		this.page = page;
		try (RevWalk refWalk = new RevWalk(repo)) {
			ObjectId objectId = repo.resolve(ref.getName());
			RevObject any = refWalk.parseAny(objectId);
			if (any instanceof RevTag) {
				tag = (RevTag) any;
				refWalk.parseBody(tag.getObject());
			} else {
				RevObject peeledObject = refWalk.peel(any);
				if (peeledObject instanceof RevCommit) {
					commit = (RevCommit) peeledObject;
				}
			}
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		name = tag != null ? tag.getTagName() : Repository.shortenRefName(ref.getName());
		descriptors = createDescriptors();
	}

	/**
	 * Create a property source for a {@link RevTag}.
	 *
	 * @param repo
	 *            repository
	 * @param tag
	 *            to show
	 * @param page
	 *            to show the property in
	 */
	public TagPropertySource(Repository repo, RevTag tag,
			PropertySheetPage page) {
		this.repository = repo;
		this.tag = tag;
		this.page = page;
		name = tag.getName();
		try (RevWalk refWalk = new RevWalk(repo)) {
			refWalk.parseBody(tag.getObject());
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
		}
		descriptors = createDescriptors();
	}

	private IPropertyDescriptor[] createDescriptors() {
		List<PropertyDescriptor> result = new ArrayList<>();
		result.add(new PropertyDescriptor(PROPERTY_TAG_NAME,
				UIText.TagPropertySource_TagName));
		if (tag == null) {
			if (commit != null) {
				result.add(new CommitPropertyDescriptor(PROPERTY_TAG_TARGET,
						UIText.TagPropertySource_TagTarget,
						new RepositoryCommit(repository, commit)));
			}
		} else {
			result.add(new PropertyDescriptor(PROPERTY_TAG_ID,
					UIText.TagPropertySource_TagId));
			result.add(
					new MessagePropertyDescriptor(PROPERTY_TAG_MESSAGE,
							UIText.TagPropertySource_TagMessage,
							tag.getFullMessage(), page));
			result.add(new PropertyDescriptor(PROPERTY_TAG_TAGGER,
					UIText.TagPropertySource_TagTagger));
			if (tag.getObject() instanceof RevCommit) {
				result.add(new CommitPropertyDescriptor(PROPERTY_TAG_TARGET,
						UIText.TagPropertySource_TagTarget,
						new RepositoryCommit(repository,
								(RevCommit) tag.getObject())));
			} else {
				result.add(new PropertyDescriptor(PROPERTY_TAG_TARGET,
						UIText.TagPropertySource_TagTarget));
			}
		}
		for (PropertyDescriptor p : result) {
			p.setCategory(UIText.TagPropertySource_TagCategory);
		}
		return result.toArray(new IPropertyDescriptor[0]);
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
		case PROPERTY_TAG_ID:
			return tag.name();
		case PROPERTY_TAG_NAME:
			return name;
		case PROPERTY_TAG_TARGET:
			if (tag != null) {
				RevObject target = tag.getObject();
				if (target instanceof RevTag) {
					return new TagPropertySource(repository, (RevTag) target,
							page);
				} else if (target instanceof RevCommit) {
					return new CommitPropertySource((RevCommit) target, page);
				}
				return Constants.typeString(target.getType()) + ' '
						+ target.name();
			}
			if (commit != null) {
				return new CommitPropertySource(commit, page);
			}
			return null;
		case PROPERTY_TAG_TAGGER:
			return new PersonIdentPropertySource(tag.getTaggerIdent());
		case PROPERTY_TAG_MESSAGE:
			return tag.getShortMessage();
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
		return tag != null ? Constants.TYPE_TAG + ' ' + tag.name()
				: MessageFormat.format(UIText.TagPropertySource_LightweightTag,
						name);
	}
}
