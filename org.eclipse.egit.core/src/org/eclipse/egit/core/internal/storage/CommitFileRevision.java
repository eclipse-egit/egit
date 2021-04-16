/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2017, 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.storage;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitTag;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.ITag;

/**
 * An {@link IFileRevision} for a version of a specified resource in the
 * specified commit (revision).
 */
public class CommitFileRevision extends GitFileRevision implements
		OpenWorkspaceVersionEnabled {

	private final RevCommit commit;

	private final PersonIdent author;

	private ObjectId blobId;

	private CheckoutMetadata metadata;

	CommitFileRevision(final Repository repo, final RevCommit rc,
			final String path) {
		this(repo, rc, path, null, null);
	}

	CommitFileRevision(final Repository repo, final RevCommit rc,
			final String path, final ObjectId blob, CheckoutMetadata metadata) {
		super(repo, path);
		commit = rc;
		author = rc.getAuthorIdent();
		blobId = blob;
		this.metadata = metadata;
	}

	@Override
	public IStorage getStorage(final IProgressMonitor monitor)
			throws CoreException {
		if (blobId == null) {
			locateBlobObjectId();
		}
		return new CommitBlobStorage(getRepository(), getGitPath(), blobId,
				commit,
				metadata);
	}

	@Override
	public long getTimestamp() {
		return author != null ? author.getWhen().getTime() : 0;
	}

	@Override
	public String getContentIdentifier() {
		return commit.name();
	}

	@Override
	public String getAuthor() {
		return author != null ? author.getName() : null;
	}

	@Override
	public String getComment() {
		return commit.getShortMessage();
	}

	@Override
	public String toString() {
		return commit.name() + ':' + getGitPath();
	}

	@Override
	public ITag[] getTags() {
		Repository db = getRepository();
		final Collection<GitTag> ret = new ArrayList<>();
		RefDatabase refs = db.getRefDatabase();
		try {
			for (Ref tag : refs.getRefsByPrefix(Constants.R_TAGS)) {
				Ref ref = refs.peel(tag);
				ObjectId refId = ref.getPeeledObjectId();
				if (refId == null)
					refId = ref.getObjectId();
				if (AnyObjectId.isEqual(refId, commit)) {
					String tagName = tag.getName()
							.substring(Constants.R_TAGS.length());
					ret.add(new GitTag(tagName));
				}
			}
		} catch (IOException e) {
			Activator.logError(MessageFormat.format(
					CoreText.CommitFileRevision_errorLookingUpTags,
					db.getDirectory().getAbsolutePath()), e);
		}
		return ret.toArray(new ITag[0]);
	}

	/**
	 * Get the commit that introduced this file revision.
	 *
	 * @return the commit we most recently noticed this file in.
	 */
	public RevCommit getRevCommit() {
		return commit;
	}

	@Override
	public Source getSource() {
		return Source.COMMIT;
	}

	@Override
	public AnyObjectId getCommitId() {
		return getRevCommit();
	}

	private void locateBlobObjectId() throws CoreException {
		try (TreeWalk w = TreeWalk.forPath(getRepository(), getGitPath(),
				commit.getTree())) {
			if (w == null)
				throw new CoreException(Activator.error(NLS.bind(
						CoreText.CommitFileRevision_pathNotIn, commit.name(),
						getGitPath()), null));
			blobId = w.getObjectId(0);
			final EolStreamType eolStreamType = w
					.getEolStreamType(TreeWalk.OperationType.CHECKOUT_OP);
			final String filterCommand = w
					.getFilterCommand(Constants.ATTR_FILTER_TYPE_SMUDGE);
			metadata = new CheckoutMetadata(eolStreamType, filterCommand);
		} catch (IOException e) {
			throw new CoreException(Activator.error(NLS.bind(
					CoreText.CommitFileRevision_errorLookingUpPath,
					commit.name(), getGitPath()), e));
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(commit, getRepository(), getGitPath());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		CommitFileRevision other = (CommitFileRevision) obj;
		return Objects.equals(commit, other.commit)
				&& Objects.equals(getRepository(), other.getRepository())
				&& Objects.equals(getGitPath(), other.getGitPath());
	}
}
