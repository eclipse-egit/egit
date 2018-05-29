/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.nio.charset.StandardCharsets;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.variants.IResourceVariant;

/**
 * Base class of the git-related resource variants.
 */
public abstract class AbstractGitResourceVariant implements IResourceVariant {
	/** Repository in which this variant's content will be accessed. */
	protected final Repository repository;

	/** Repository-relative path of this resource. */
	protected final String path;

	/**
	 * Whether this resource is a container or not in this particular variant.
	 * This may be different than the local resource's state (if there is a
	 * file/folder conflict for example).
	 */
	protected final boolean isContainer;

	/** Object id of this variant in its repository. */
	protected final ObjectId objectId;

	/** Raw mode bits of this variant. */
	protected final int rawMode;

	/**
	 * @param repository
	 *            Repository in which this variant's content will be accessed.
	 * @param path
	 *            Repository-relative path of this resource.
	 * @param isContainer
	 *            Whether this resource is a container or not in this particular
	 *            variant.
	 * @param objectId
	 *            Object id of this variant in its repository.
	 * @param rawMode
	 *            Raw mode bits of this variant.
	 */
	protected AbstractGitResourceVariant(Repository repository, String path,
			boolean isContainer, ObjectId objectId, int rawMode) {
		this.repository = repository;
		this.path = path;
		this.isContainer = isContainer;
		this.objectId = objectId;
		this.rawMode = rawMode;
	}

	@Override
	public String getName() {
		int lastSeparator = path.lastIndexOf('/');
		return path.substring(lastSeparator + 1);
	}

	@Override
	public boolean isContainer() {
		return isContainer;
	}

	@Override
	public String getContentIdentifier() {
		return objectId.name();
	}

	@Override
	public byte[] asBytes() {
		return objectId.name().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * @return the object id of this variant in its backing repository.
	 */
	public ObjectId getObjectId() {
		return objectId;
	}

	/**
	 * @return the raw mode of this variant.
	 */
	public int getRawMode() {
		return rawMode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof AbstractGitResourceVariant) {
			AbstractGitResourceVariant other = (AbstractGitResourceVariant) obj;
			return this.path.equals(other.path)
					&& this.repository.equals(other.repository)
					&& this.objectId.equals(other.objectId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 37;
		hash = 37 * hash + (path != null ? path.hashCode() : 0);
		hash = 37 * hash + (repository != null ? repository.hashCode() : 0);
		hash = 37 * hash + (objectId != null ? objectId.hashCode() : 0);
		return hash;
	}
}
