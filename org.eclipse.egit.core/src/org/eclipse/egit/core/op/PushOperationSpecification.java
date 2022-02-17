/*******************************************************************************
 * Copyright (C) 2008, 2022 Marek Zawirski <marek.zawirski@gmail.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;

/**
 * Data class storing push operation update specifications for each remote
 * repository.
 * <p>
 * One instance is dedicated for one push operation: either push to one URI or
 * to many URIs.
 *
 * @see PushOperation
 */
public class PushOperationSpecification {
	private LinkedHashMap<URIish, Collection<RemoteRefUpdate>> urisRefUpdates;

	/**
	 * Create empty instance of specification.
	 * <p>
	 * URIs and ref updates should be configured
	 * {@link #addURIRefUpdates(URIish, Collection)} method.
	 */
	public PushOperationSpecification() {
		this.urisRefUpdates = new LinkedHashMap<>();
	}

	/**
	 * Add remote repository URI with ref updates specification.
	 * <p>
	 * Ref updates are not in constructor - pay attention to not share them
	 * between different URIs ref updates or push operations.
	 * <p>
	 * Note that refUpdates can differ between URIs <b>only</b> by expected old
	 * object id field: {@link RemoteRefUpdate#getExpectedOldObjectId()}.
	 *
	 * @param uri
	 *            remote repository URI.
	 * @param refUpdates
	 *            collection of remote ref updates specifications.
	 */
	public void addURIRefUpdates(final URIish uri,
			Collection<RemoteRefUpdate> refUpdates) {
		urisRefUpdates.put(uri, refUpdates);
	}

	/**
	 * @return set of remote repositories URIish. Set is ordered in addition
	 *         sequence.
	 */
	public Set<URIish> getURIs() {
		return Collections.unmodifiableSet(urisRefUpdates.keySet());
	}

	/**
	 * @return number of remote repositories URI for this push operation.
	 */
	public int getURIsNumber() {
		return urisRefUpdates.size();
	}

	/**
	 * @param uri
	 *            remote repository URI.
	 * @return remote ref updates as specified by user for this URI.
	 */
	public Collection<RemoteRefUpdate> getRefUpdates(final URIish uri) {
		return Collections.unmodifiableCollection(urisRefUpdates.get(uri));
	}

	/**
	 * Creates a {@link PushOperationSpecification} for the given
	 * {@link RemoteConfig} and {@link RefSpec}s.
	 *
	 * @param repository
	 *            {@link Repository} to push from
	 * @param config
	 *            {@link RemoteConfig} to push to
	 * @param refSpecs
	 *            the {@link RefSpec}s defining what to push
	 * @return the {@link PushOperationSpecification}
	 * @throws IOException
	 *             if an error occurs
	 */
	public static PushOperationSpecification create(Repository repository,
			RemoteConfig config, Collection<RefSpec> refSpecs)
			throws IOException {
		PushOperationSpecification result = new PushOperationSpecification();
		Collection<RemoteRefUpdate> remoteRefUpdates = Transport
				.findRemoteRefUpdatesFor(repository, refSpecs,
						config.getFetchRefSpecs());
		boolean added = false;
		for (URIish uri : config.getPushURIs()) {
			result.addURIRefUpdates(uri, copyUpdates(remoteRefUpdates, false));
			added = true;
		}
		if (!added && !config.getURIs().isEmpty()) {
			result.addURIRefUpdates(config.getURIs().get(0), remoteRefUpdates);
		}
		return result;
	}

	/**
	 * Creates a copy of a collection of {@link RemoteRefUpdate}s. The copy
	 * contains copies of the original updates.
	 *
	 * @param refUpdates
	 *            {@link RemoteRefUpdate}s to copy
	 * @param withExpectedOid
	 *            {@code true} if the expected OIDs of the original
	 *            {@link RemoteRefUpdate}s shall be retained, {@code false} if
	 *            not
	 * @return the copied collection
	 * @throws IOException
	 *             if the
	 */
	public static Collection<RemoteRefUpdate> copyUpdates(
			Collection<RemoteRefUpdate> refUpdates, boolean withExpectedOid)
			throws IOException {
		Collection<RemoteRefUpdate> copy = new ArrayList<>(refUpdates.size());
		for (RemoteRefUpdate rru : refUpdates) {
			copy.add(new RemoteRefUpdate(rru,
					withExpectedOid ? rru.getExpectedOldObjectId() : null));
		}
		return copy;
	}

}
