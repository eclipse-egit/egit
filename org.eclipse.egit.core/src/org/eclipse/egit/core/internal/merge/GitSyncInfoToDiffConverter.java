/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.storage.WorkspaceFileRevision;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * The default implementation of SyncInfoToDiffConverter uses inaccurate
 * information with regards to some of EGit features.
 * <p>
 * SyncInfoToDiffConverter#asFileRevision(IResourceVariant) is called when a
 * user double-clicks a revision from the synchronize view (among others).
 * However, the default implementation returns an IFileRevision with no comment,
 * author or timestamp information.
 * </p>
 * <p>
 * SyncInfoToDiffConverter#getDeltaFor(SyncInfo) had been originally thought by
 * Team to be used for synchronizations that considered local changes. This is
 * not always the case with EGit. For example, a user might try and compare two
 * refs together from the Git repository explorer (right click > synchronize
 * with each other). In such a case, the local files must not be taken into
 * account.
 * </p>
 * <p>
 * Most of the private methods here were copy/pasted from the super
 * implementation.
 * </p>
 */
public class GitSyncInfoToDiffConverter extends SyncInfoToDiffConverter {
	private GitResourceVariantTreeProvider variantTreeProvider;

	/**
	 * Creates our diff converter given the provider of our variant trees.
	 *
	 * @param variantTreeProvider
	 *            Provides the resource variant trees that should be used to
	 *            query file revisions.
	 */
	public GitSyncInfoToDiffConverter(
			GitResourceVariantTreeProvider variantTreeProvider) {
		this.variantTreeProvider = variantTreeProvider;
	}

	@Override
	public IDiff getDeltaFor(SyncInfo info) {
		if (info.getComparator().isThreeWay()) {
			ITwoWayDiff local = getLocalDelta(info);
			ITwoWayDiff remote = getRemoteDelta(info);
			return new ThreeWayDiff(local, remote);
		} else {
			if (info.getKind() != SyncInfo.IN_SYNC) {
				IResourceVariant remote = info.getRemote();
				IResource local = info.getLocal();

				return computeResourceDiff(info, remote, local);
			}
			return null;
		}
	}

	private ITwoWayDiff getLocalDelta(SyncInfo info) {
		int direction = SyncInfo.getDirection(info.getKind());
		if (direction == SyncInfo.OUTGOING || direction == SyncInfo.CONFLICTING) {
			IResourceVariant ancestor = info.getBase();
			IResource local = info.getLocal();

			return computeResourceDiff(info, ancestor, local);
		}
		return null;
	}

	private ResourceDiff computeResourceDiff(SyncInfo info,
			IResourceVariant variant, IResource local) {
		int kind;
		if (variant == null)
			kind = IDiff.REMOVE;
		else if (!local.exists())
			kind = IDiff.ADD;
		else
			kind = IDiff.CHANGE;

		if (local.getType() == IResource.FILE) {
			IFileRevision after = asFileState(variant);
			IFileRevision before = getLocalFileRevision((IFile) local);
			return new ResourceDiff(info.getLocal(), kind, 0, before, after);
		}
		// For folders, we don't need file states
		return new ResourceDiff(info.getLocal(), kind);
	}

	/**
	 * Returns a file revision from the source tree for this local file.
	 *
	 * @param local
	 *            The local file.
	 * @return The file revision that should be considered for the local (left)
	 *         side a delta
	 */
	public IFileRevision getLocalFileRevision(IFile local) {
		try {
			return asFileState(variantTreeProvider.getSourceTree()
					.getResourceVariant(local));
		} catch (TeamException e) {
			String error = NLS
					.bind(CoreText.GitResourceVariantTreeSubscriber_CouldNotFindSourceVariant,
							local.getName());
			Activator.logError(error, e);
			// fall back to the working tree version
			return new WorkspaceFileRevision(local);
		}
	}

	/*
	 * copy-pasted from the private implementation in SyncInfoToDiffConverter
	 */
	private ITwoWayDiff getRemoteDelta(SyncInfo info) {
		int direction = SyncInfo.getDirection(info.getKind());
		if (direction == SyncInfo.INCOMING || direction == SyncInfo.CONFLICTING) {
			IResourceVariant ancestor = info.getBase();
			IResourceVariant remote = info.getRemote();

			int kind;
			if (ancestor == null)
				kind = IDiff.ADD;
			else if (remote == null)
				kind = IDiff.REMOVE;
			else
				kind = IDiff.CHANGE;

			// For folders, we don't need file states
			if (info.getLocal().getType() == IResource.FILE) {
				IFileRevision before = asFileState(ancestor);
				IFileRevision after = asFileState(remote);
				return new ResourceDiff(info.getLocal(), kind, 0, before, after);
			}

			return new ResourceDiff(info.getLocal(), kind);
		}
		return null;
	}

	/*
	 * copy-pasted from the private implementation in SyncInfoToDiffConverter
	 */
	private IFileRevision asFileState(final IResourceVariant variant) {
		if (variant == null)
			return null;
		return asFileRevision(variant);
	}

	@Override
	protected ResourceVariantFileRevision asFileRevision(
			IResourceVariant variant) {
		return new GitResourceVariantFileRevision(variant);
	}
}
