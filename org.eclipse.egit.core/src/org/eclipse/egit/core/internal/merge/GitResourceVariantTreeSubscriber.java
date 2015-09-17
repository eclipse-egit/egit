/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.ResourceVariantTreeSubscriber;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * This implementation of a {@link ResourceVariantTreeSubscriber} takes its
 * input from a {@link GitResourceVariantTreeProvider}.
 * <p>
 * This allows us to hijack all calls from the default subscriber for "local"
 * resources to our actual source tree, which could be the local working
 * directory as well as it could be a branch.
 * </p>
 */
public class GitResourceVariantTreeSubscriber extends
		ResourceVariantTreeSubscriber {
	private GitResourceVariantTreeProvider variantTreeProvider;

	private final SyncInfoToDiffConverter syncInfoConverter;

	private final IResourceVariantComparator comparator;

	/**
	 * @param variantTreeProvider
	 *            The instance that will provide the base, source and remote
	 *            trees to this subscriber.
	 */
	public GitResourceVariantTreeSubscriber(
			GitResourceVariantTreeProvider variantTreeProvider) {
		this.variantTreeProvider = variantTreeProvider;
		syncInfoConverter = new GitSyncInfoToDiffConverter(variantTreeProvider);
		comparator = new GitVariantComparator(
				variantTreeProvider.getSourceTree());
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		return variantTreeProvider.getBaseTree();
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		return variantTreeProvider.getRemoteTree();
	}

	/**
	 * @return the source resource variant tree.
	 */
	protected IResourceVariantTree getSourceTree() {
		return variantTreeProvider.getSourceTree();
	}

	@Override
	public IDiff getDiff(IResource resource) throws CoreException {
		final SyncInfo info = getSyncInfo(resource);
		if (info == null || info.getKind() == SyncInfo.IN_SYNC)
			return null;
		return syncInfoConverter.getDeltaFor(info);
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		// Overridden here to properly catch and re-throw the forwarded
		// TeamException
		try {
			return super.getSyncInfo(resource);
		} catch (ForwardedTeamException e) {
			throw (TeamException) e.getCause();
		}
	}

	@Override
	public String getName() {
		return CoreText.GitResourceVariantTreeSubscriber_name;
	}

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		return variantTreeProvider.getKnownResources().contains(resource);
	}

	@Override
	public IResource[] roots() {
		final Set<IResource> roots = variantTreeProvider.getRoots();
		return roots.toArray(new IResource[roots.size()]);
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return comparator;
	}

	/**
	 * We have a source tree whereas Team only knows about "local" files. This
	 * will always use said {@link #oursTree source tree} when comparing
	 * variants.
	 */
	private static class GitVariantComparator implements
			IResourceVariantComparator {
		private final IResourceVariantTree oursTree;

		public GitVariantComparator(IResourceVariantTree oursTree) {
			this.oursTree = oursTree;
		}

		@Override
		public boolean compare(IResource local, IResourceVariant remote) {
			try {
				final IResourceVariant oursVariant = oursTree
						.getResourceVariant(local);
				if (oursVariant == null)
					return remote == null;
				return compare(oursVariant, remote);
			} catch (TeamException e) {
				// We can't throw the TeamException from here, but we can't let
				// the comparison go through either.
				// This is only called from "getSyncInfo", we'll forward this
				// exception and rethrow it from there.
				throw new ForwardedTeamException(e);
			}
		}

		@Override
		public boolean compare(IResourceVariant base, IResourceVariant remote) {
			return Arrays.equals(base.asBytes(), remote.asBytes());
		}

		@Override
		public boolean isThreeWay() {
			return true;
		}
	}

	/**
	 * This should never be thrown outside of this class. The only purpose of
	 * this exception is to encapsulate a TeamException where it cannot be
	 * thrown.
	 */
	private static class ForwardedTeamException extends RuntimeException {
		/** Generated SUID. */
		private static final long serialVersionUID = 4074010396155542178L;

		public ForwardedTeamException(TeamException e) {
			super(e);
		}
	}
}
