/*******************************************************************************
 * Copyright (C) 2008, 2014 Marek Zawirski <marek.zawirski@gmail.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.DecorationOverlayDescriptor;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.ui.model.WorkbenchAdapter;

/**
 * Data class representing row (element) of table with push results.
 * <p>
 * Each row is associated with one ref update, while each column is associated
 * with one URI (remote repository).
 *
 * @see PushOperationResult
 * @see RefUpdateContentProvider
 */
class RefUpdateElement extends WorkbenchAdapter {
	private final RemoteRefUpdate update;

	private final PushOperationResult result;

	private final URIish uri;

	private final ObjectReader reader;

	private final Repository repo;

	private Object[] children;

	private final boolean tag;

	RefUpdateElement(final PushOperationResult result, RemoteRefUpdate update,
			URIish uri, ObjectReader reader, Repository repo) {
		this.result = result;
		this.update = update;
		this.uri = uri;
		this.reader = reader;
		this.repo = repo;
		String remote = update.getRemoteName();
		tag = remote != null && remote.startsWith(Constants.R_TAGS);
	}

	URIish getUri() {
		return uri;
	}

	String getSrcRefName() {
		return update.getSrcRef();
	}

	String getDstRefName() {
		return update.getRemoteName();
	}

	boolean isDelete() {
		// Assuming that we never use ObjectId.zeroId() in GUI.
		// (no need to compare to it).
		return getSrcRefName() == null;
	}

	boolean isAdd() {
		return getAdvertisedRemoteRef() == null;
	}

	boolean isRejected() {
		switch (getStatus()) {
		case REJECTED_NODELETE:
		case REJECTED_NONFASTFORWARD:
		case REJECTED_OTHER_REASON:
		case REJECTED_REMOTE_CHANGED:
			return true;
		default:
			return false;
		}
	}

	boolean isTag() {
		return tag;
	}

	PushOperationResult getPushOperationResult() {
		return result;
	}

	boolean isSuccessfulConnection() {
		return result.isSuccessfulConnection(uri);
	}

	String getErrorMessage() {
		return result.getErrorMessage(uri);
	}

	Status getStatus() {
		return update.getStatus();
	}

	RemoteRefUpdate getRemoteRefUpdate() {
		return update;
	}

	Ref getAdvertisedRemoteRef() {
		return result.getPushResult(uri).getAdvertisedRef(getDstRefName());
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		switch (getStatus()) {
		case OK:
			if (isDelete())
				return tag ? new DecorationOverlayDescriptor(UIIcons.TAG,
						UIIcons.OVR_STAGED_REMOVE, IDecoration.TOP_RIGHT)
						: new DecorationOverlayDescriptor(UIIcons.BRANCH,
								UIIcons.OVR_STAGED_REMOVE,
								IDecoration.TOP_RIGHT);

			if (isAdd())
				return tag ? UIIcons.CREATE_TAG : UIIcons.CREATE_BRANCH;
			else
				return tag ? UIIcons.TAG : UIIcons.BRANCH;
		case UP_TO_DATE:
			return tag ? UIIcons.TAG : UIIcons.BRANCH;
		case REJECTED_NODELETE:
		case REJECTED_NONFASTFORWARD:
		case REJECTED_OTHER_REASON:
		case REJECTED_REMOTE_CHANGED:
			return tag ? new DecorationOverlayDescriptor(UIIcons.TAG,
					UIIcons.OVR_ERROR, IDecoration.TOP_RIGHT)
					: new DecorationOverlayDescriptor(UIIcons.BRANCH,
							UIIcons.OVR_ERROR, IDecoration.TOP_RIGHT);
		default:
			return super.getImageDescriptor(object);
		}
	}

	@Override
	public String getLabel(Object object) {
		return getStyledText(object).getString();
	}

	private RepositoryCommit[] getCommits(Ref end) {
		try (final RevWalk walk = new RevWalk(reader)) {
			walk.setRetainBody(true);
			walk.markStart(walk.parseCommit(update.getNewObjectId()));
			walk.markUninteresting(walk.parseCommit(end.getObjectId()));
			List<RepositoryCommit> commits = new ArrayList<>();
			for (RevCommit commit : walk)
				commits.add(new RepositoryCommit(repo, commit));
			return commits.toArray(new RepositoryCommit[commits.size()]);
		} catch (IOException e) {
			Activator.logError("Error parsing commits from push result", e); //$NON-NLS-1$
			return new RepositoryCommit[0];
		}
	}

	@Override
	public Object[] getChildren(Object object) {
		if (children != null)
			return children;

		switch (update.getStatus()) {
		case OK:
			if (!isDelete()) {
				final Ref ref = getAdvertisedRemoteRef();
				if (ref != null) {
					children = getCommits(ref);
					break;
				}
			}
			//$FALL-THROUGH$
		default:
			children = super.getChildren(object);
		}
		return children;
	}

	/**
	 * Shorten ref name
	 *
	 * @param ref
	 * @return shortened ref name
	 */
	protected String shortenRef(final String ref) {
		return NoteMap.shortenRefName(Repository.shortenRefName(ref));
	}

	/**
	 * Get styled text
	 *
	 * @param object
	 * @return styled string
	 */
	@Override
	public StyledString getStyledText(Object object) {
		StyledString styled = new StyledString();
		final String remote = getDstRefName();
		final String local = getSrcRefName();

		if (!tag && local != null) {
			styled.append(shortenRef(local));
			styled.append(" \u2192 " /* → */); //$NON-NLS-1$
		}
		styled.append(shortenRef(remote));

		styled.append(' ');
		// Include uri if more than one
		if (result.getURIs().size() > 1) {
			styled.append(MessageFormat.format(
					UIText.RefUpdateElement_UrisDecoration, uri.toString()),
					StyledString.QUALIFIER_STYLER);
			styled.append(' ');
		}

		switch (getStatus()) {
		case OK:
			if (update.isDelete())
				styled.append(UIText.PushResultTable_statusOkDeleted,
						StyledString.DECORATIONS_STYLER);
			else {
				final Ref oldRef = getAdvertisedRemoteRef();
				if (oldRef == null) {
					if (tag)
						styled.append(UIText.PushResultTable_statusOkNewTag,
								StyledString.DECORATIONS_STYLER);
					else
						styled.append(UIText.PushResultTable_statusOkNewBranch,
								StyledString.DECORATIONS_STYLER);
				} else {
					String separator = update.isFastForward() ? ".." : "..."; //$NON-NLS-1$ //$NON-NLS-2$
					ObjectId objectId = oldRef.getObjectId();
					Object oldName = objectId != null
							? objectId.abbreviate(7).name() : "?"; //$NON-NLS-1$
					styled.append(MessageFormat.format(
							UIText.RefUpdateElement_CommitRangeDecoration,
							update.getNewObjectId().abbreviate(7).name(),
									separator, oldName),
							StyledString.DECORATIONS_STYLER);
					styled.append(' ');
					styled.append(MessageFormat.format(
							UIText.RefUpdateElement_CommitCountDecoration,
							Integer.valueOf(getChildren(this).length)),
							StyledString.COUNTER_STYLER);
				}
			}
			break;
		case UP_TO_DATE:
			styled.append(UIText.PushResultTable_statusUpToDate,
					StyledString.DECORATIONS_STYLER);
			break;
		case NON_EXISTING:
			styled.append(UIText.PushResultTable_statusNoMatch,
					StyledString.DECORATIONS_STYLER);
			break;
		case REJECTED_NODELETE:
		case REJECTED_REMOTE_CHANGED:
			styled.append(UIText.PushResultTable_statusRejected,
					StyledString.DECORATIONS_STYLER);
			break;
		case REJECTED_NONFASTFORWARD:
			styled.append(UIText.RefUpdateElement_statusRejectedNonFastForward,
					StyledString.DECORATIONS_STYLER);
			break;
		case REJECTED_OTHER_REASON:
			styled.append(UIText.PushResultTable_statusRemoteRejected,
					StyledString.DECORATIONS_STYLER);
			break;
		default:
			break;
		}
		return styled;
	}
}
