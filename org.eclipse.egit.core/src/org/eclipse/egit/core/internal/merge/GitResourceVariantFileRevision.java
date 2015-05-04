/*******************************************************************************
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import org.eclipse.egit.core.synchronize.GitRemoteResource;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;

/**
 * The default implementation of ResourceVariantFileRevision has no author,
 * comment, timestamp... or any information that could be provided by the Git
 * resource variant. This implementation uses the variant's information.
 */
class GitResourceVariantFileRevision extends ResourceVariantFileRevision {

	public GitResourceVariantFileRevision(GitRemoteResource variant) {
		super(variant);
	}

	@Override
	public GitRemoteResource getVariant() {
		return (GitRemoteResource) super.getVariant();
	}

	@Override
	public String getContentIdentifier() {
		// Use the same ID as would CommitFileRevision
		return getVariant().getCommitId().getId().getName();
	}

	@Override
	public long getTimestamp() {
		final PersonIdent author = getVariant().getCommitId().getAuthorIdent();
		if (author != null) {
			return author.getWhen().getTime();
		}
		return super.getTimestamp();
	}

	@Override
	public String getAuthor() {
		final PersonIdent author = getVariant().getCommitId().getAuthorIdent();
		if (author != null) {
			return author.getName();
		}
		return super.getAuthor();
	}

	@Override
	public String getComment() {
		return getVariant().getCommitId().getFullMessage();

	}
}
