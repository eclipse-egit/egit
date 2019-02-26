/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.IOException;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;

/**
 * Content proposal class for refs names, specifically Ref objects - name with
 * optionally associated object id. This class can be used for Eclipse field
 * assist as content proposal.
 * <p>
 * Content of this proposal is simply a ref name, but description and labels
 * tries to be smarter - showing easier to read label for user (stripping
 * prefixes) and information about pointed object if it exists locally.
 */
public class RefContentProposal implements IContentProposal {
	private static final String PREFIXES[] = new String[] { Constants.R_HEADS,
			Constants.R_REMOTES, Constants.R_TAGS };

	private final static String branchPF = " ["   //$NON-NLS-1$
		+ UIText.RefContentProposal_branch
		+ "]";  //$NON-NLS-1$

	private final static String trackingBranchPF = " ["  //$NON-NLS-1$
		+ UIText.RefContentProposal_trackingBranch
		+ "]";  //$NON-NLS-1$

	private final static String tagPF = " ["  //$NON-NLS-1$
		+ UIText.RefContentProposal_tag
		+ "]"; //$NON-NLS-1$

	private static final String PREFIXES_DESCRIPTIONS[] = new String[] {
			branchPF, trackingBranchPF, tagPF };

	private static void appendObjectSummary(final StringBuilder sb,
			final String type, final PersonIdent author, final String message) {
		sb.append(type);
		PreferenceBasedDateFormatter dateFormatter = PreferenceBasedDateFormatter
				.create();
		if (author != null) {
			sb.append(" "); //$NON-NLS-1$
			sb.append(UIText.RefContentProposal_by);
			sb.append(" "); //$NON-NLS-1$
			sb.append(author.getName());
			sb.append("\n"); //$NON-NLS-1$
			sb.append(dateFormatter.formatDate(author));
		}
		sb.append("\n\n");  //$NON-NLS-1$
		final int newLine = message.indexOf('\n');
		final int last = (newLine != -1 ? newLine : message.length());
		sb.append(message.substring(0, last));
	}

	private final Repository db;

	private final String refName;

	private final ObjectId objectId;

	/**
	 * Whether the ref is an upstream ref. For upstream refs, it's OK to have a
	 * missing object; it just means we haven't fetched yet.
	 */
	private final boolean upstream;

	/**
	 * Create content proposal for specified ref.
	 *
	 * @param repo
	 *            repository for accessing information about objects. Could be a
	 *            local repository even for remote objects.
	 * @param ref
	 *            ref being a content proposal. May have null or locally
	 *            non-existent object id.
	 * @param upstream
	 *            {@code true} if the ref comes from an upstream repository
	 */
	public RefContentProposal(Repository repo, Ref ref, boolean upstream) {
		this(repo, ref.getName(), ref.getObjectId(), upstream);
	}

	/**
	 * Create content proposal for specified ref name and object id.
	 *
	 * @param repo
	 *            repository for accessing information about objects. Could be a
	 *            local repository even for remote objects.
	 * @param refName
	 *            ref name being a content proposal.
	 * @param objectId
	 *            object being pointed by this ref name. May be null or locally
	 *            non-existent object.
	 * @param upstream
	 *            {@code true} if the ref comes from an upstream repository
	 */
	public RefContentProposal(Repository repo, String refName,
			ObjectId objectId, boolean upstream) {
		this.db = repo;
		this.refName = refName;
		this.objectId = objectId;
		this.upstream = upstream;
	}

	@Override
	public String getContent() {
		return refName;
	}

	@Override
	public int getCursorPosition() {
		return refName.length();
	}

	@Override
	public String getDescription() {
		if (objectId == null) {
			return null;
		} else if (upstream && objectId.equals(ObjectId.zeroId())) {
			return refName + '\n' + UIText.RefContentProposal_newRemoteObject;
		}
		try (ObjectReader reader = db.newObjectReader()) {
			ObjectLoader loader = null;
			try {
				loader = reader.open(objectId);
			} catch (MissingObjectException e) {
				if (upstream) {
					return refName + '\n' + objectId.abbreviate(7).name()
							+ " - " //$NON-NLS-1$
							+ UIText.RefContentProposal_unknownRemoteObject;
				}
				throw e;
			}
			final StringBuilder sb = new StringBuilder();
			sb.append(refName);
			sb.append('\n');
			sb.append(reader.abbreviate(objectId).name());
			sb.append(" - "); //$NON-NLS-1$

			switch (loader.getType()) {
			case Constants.OBJ_COMMIT:
				try (RevWalk rw = new RevWalk(db)) {
					RevCommit c = rw.parseCommit(objectId);
					appendObjectSummary(sb, UIText.RefContentProposal_commit,
							c.getAuthorIdent(), c.getFullMessage());
				}
				break;
			case Constants.OBJ_TAG:
				try (RevWalk rw = new RevWalk(db)) {
					RevTag t = rw.parseTag(objectId);
					appendObjectSummary(sb, UIText.RefContentProposal_tag,
							t.getTaggerIdent(), t.getFullMessage());
				}
				break;
			case Constants.OBJ_TREE:
				sb.append(UIText.RefContentProposal_tree);
				break;
			case Constants.OBJ_BLOB:
				sb.append(UIText.RefContentProposal_blob);
				break;
			default:
				sb.append(UIText.RefContentProposal_unknownObject);
			}
			return sb.toString();
		} catch (IOException e) {
			Activator.logError(NLS.bind(
					UIText.RefContentProposal_errorReadingObject, objectId,
					refName), e);
			return null;
		}
	}

	@Override
	public String getLabel() {
		for (int i = 0; i < PREFIXES.length; i++)
			if (refName.startsWith(PREFIXES[i]))
				return refName.substring(PREFIXES[i].length())
						+ PREFIXES_DESCRIPTIONS[i];
		return refName;

	}
}
