/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariantTree;

/**
 * Resource variant trees are in charge of providing the
 * {@link org.eclipse.team.core.subscribers.Subscriber Subscribers} with
 * resource variants, allowing them to retrieve the content of a given file in
 * different states (remote, local, index, workspace...).
 */
public interface GitResourceVariantTreeProvider {
	/**
	 * Returns the base resource variant tree. This should provide access to the
	 * common ancestor of the "source" and "remote" resource variants.
	 *
	 * @return The base resource variant tree.
	 */
	IResourceVariantTree getBaseTree();

	/**
	 * Returns the remote resource variant tree. This is traditionally the
	 * remote data, or 'right' side of a comparison. In git terms, this is the
	 * "theirs" side.
	 *
	 * @return The remote resource variant tree.
	 */
	IResourceVariantTree getRemoteTree();

	/**
	 * Returns the source resource variant tree. This is traditionally the local
	 * data, or 'left' side of a comparison. In git terms, this is the "ours"
	 * side.
	 *
	 * @return The source resource variant tree.
	 */
	IResourceVariantTree getSourceTree();

	/**
	 * @return The list of root resources for which this provider's trees may
	 *         hold variants.
	 */
	Set<IResource> getRoots();

	/**
	 * Returns the whole set of resources for which this provider's trees hold
	 * variants. The returned resources may not necessarily exist in all three
	 * underlying trees.
	 *
	 * @return The whole set of resources for which this provider's trees hold
	 *         variants.
	 */
	Set<IResource> getKnownResources();
}
