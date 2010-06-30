/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Based on: {@link org.eclipse.team.core.variants.SessionResourceVariantByteStore}
 *
 *******************************************************************************/
package org.eclipse.egit.core.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.ResourceVariantByteStore;

class GitResourceVariantByteStore extends ResourceVariantByteStore {

	private static final byte[] NO_REMOTE = new byte[0];

	private final Map<IPath, byte[]> contentMap;

	private final Map<IPath, List<IResource>> parentMap;

	public GitResourceVariantByteStore() {
		contentMap = new HashMap<IPath, byte[]>();
		parentMap = new HashMap<IPath, List<IResource>>();
	}

	@Override
	public void dispose() {
		parentMap.clear();
		contentMap.clear();
	}

	@Override
	public byte[] getBytes(IResource resource) throws TeamException {
		if (resource == null)
			return null;

		byte[] syncBytes = contentMap.get(resource.getFullPath());
		if (syncBytes != null && equals(syncBytes, NO_REMOTE))
			// If it is known that there is no remote, return null
			return null;

		return syncBytes;
	}

	@Override
	public boolean setBytes(IResource resource, byte[] bytes)
			throws TeamException {
		Assert.isNotNull(bytes);
		IPath fullPath = resource.getFullPath();
		byte[] oldBytes = contentMap.get(fullPath);

		if (oldBytes != null && Arrays.equals(oldBytes, bytes))
			return false;

		contentMap.put(fullPath, bytes);
		addToParent(resource);

		return true;
	}

	@Override
	public boolean flushBytes(IResource resource, int depth)
			throws TeamException {
		IPath fullPath = resource.getFullPath();

		if (contentMap.containsKey(fullPath)) {
			if (depth != IResource.DEPTH_ZERO)
				flushBytesWithNonZeroDepth(resource, depth);

			contentMap.remove(fullPath);
			internalRemoveFromParent(resource);
			return true;
		}

		return false;
	}

	@Override
	public boolean deleteBytes(IResource resource) throws TeamException {
		return flushBytes(resource, IResource.DEPTH_ZERO);
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		List<IResource> members = parentMap.get(resource.getFullPath());
		if (members == null) {
			return new IResource[0];
		}
		return members.toArray(new IResource[members.size()]);
	}

	private void internalRemoveFromParent(IResource resource) {
		IPath fullPath = resource.getParent().getFullPath();
		List<IResource> members = parentMap.get(fullPath);

		if (members != null) {
			members.remove(resource);
			if (members.isEmpty()) {
				parentMap.remove(fullPath);
			}
		}
	}

	private void addToParent(IResource resource) {
		IContainer parent = resource.getParent();

		if (parent == null)
			return;

		IPath fullPath = parent.getFullPath();
		List<IResource> members = parentMap.get(fullPath);

		if (members == null) {
			members = new ArrayList<IResource>();
			parentMap.put(fullPath, members);
		}

		members.add(resource);
	}

	private void flushBytesWithNonZeroDepth(IResource resource, int depth)
			throws TeamException {
		IResource[] members = members(resource);
		for (int i = 0; i < members.length; i++) {
			IResource child = members[i];
			flushBytes(
					child,
					(depth == IResource.DEPTH_INFINITE) ? IResource.DEPTH_INFINITE
							: IResource.DEPTH_ZERO);
		}
	}

}
