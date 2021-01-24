/*******************************************************************************
 *  Copyright (c) 2014 Maik Schreiber
 *  Copyright (C) 2015 Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 *  Copyright (C) 2020 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Maik Schreiber - initial implementation
 *    Stephan Hackstedt - bug 477695
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.UnsupportedSigningFormatException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.GpgObjectSigner;
import org.eclipse.jgit.lib.GpgSigner;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.team.core.TeamException;

/** An operation that rewords a commit's message. */
public class RewordCommitOperation implements IEGitOperation {

	private Repository repository;
	private RevCommit commit;
	private String newMessage;

	private ObjectId headId;

	/**
	 * Constructs a new reword commit operation.
	 *
	 * @param repository
	 *            the repository to work on
	 * @param commit
	 *            the commit
	 * @param newMessage
	 *            the new message to set for the commit
	 */
	public RewordCommitOperation(Repository repository, RevCommit commit,
			String newMessage) {
		this.repository = repository;
		this.commit = commit;
		this.newMessage = newMessage;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		try (RevWalk walk = new RevWalk(repository)) {
			// Lock the index. We don't touch the index, but this guards against
			// most other operations that might interfere with the in-memory
			// rebase that we're going to perform. (Would be good if HEAD
			// remained unchanged while we rebase. If it does change all the
			// same, the RefUpdate at the end will fail and we will have done
			// the work in vain.)
			DirCache index = repository.lockDirCache();
			try {
				reword(monitor, walk);
			} finally {
				index.unlock();
			}
		} catch (IOException e) {
			throw new TeamException(e.getMessage(), e);
		}
	}

	private void reword(IProgressMonitor monitor, RevWalk walk)
			throws IOException, CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, MessageFormat.format(
				CoreText.RewordCommitOperation_rewording,
				Utils.getShortObjectId(commit)), 100);

		commit = walk.parseCommit(commit);
		if (newMessage.equals(commit.getFullMessage())) {
			// Nothing to do.
			return;
		}
		Ref ref = repository.exactRef(Constants.HEAD);
		if (ref == null) {
			// No HEAD: cannot reword
			throw new TeamException(CoreText.RewordCommitOperation_noHead);
		}
		headId = ref.getObjectId();
		if (headId == null || ObjectId.zeroId().equals(headId)) {
			throw new TeamException(CoreText.RewordCommitOperation_noHead);
		}
		String headName = ref.isSymbolic() ? ref.getLeaf().getName()
				: headId.name();
		Deque<RevCommit> commits = new LinkedList<>();
		walk.setRetainBody(true);
		if (!commit.getId().equals(headId)) {
			walk.sort(RevSort.TOPO);
			walk.sort(RevSort.COMMIT_TIME_DESC, true);
			walk.markStart(walk.parseCommit(headId));
			for (RevCommit p : commit.getParents()) {
				RevCommit parsed = walk.parseCommit(p);
				walk.markUninteresting(parsed);
			}
			RevCommit c;
			while ((c = walk.next()) != null) {
				if (c.getId().equals(commit.getId())) {
					break;
				}
				commits.push(c);
			}
			if (c == null) {
				throw new TeamException(MessageFormat.format(
						CoreText.RewordCommitOperation_notReachable,
						Utils.getShortObjectId(commit)));
			}
		}
		progress.worked(10);
		progress.setWorkRemaining(commits.size() + 2);
		PersonIdent committer = new PersonIdent(repository);
		// Rewrite the message
		CommitBuilder builder = copy(commit, commit.getParents(), committer,
				newMessage);
		// Signature will be invalid for the new commit. Try to re-sign.
		File gpgProgram = GitSettings.getGpgExecutable();
		GpgConfig gpgConfig = new GpgConfig(repository.getConfig()) {

			@Override
			public String getProgram() {
				return gpgProgram != null ? gpgProgram.getAbsolutePath()
						: super.getProgram();
			}
		};
		boolean signAllCommits = gpgConfig.isSignCommits();
		GpgSigner gpgSigner = GpgSigner.getDefault();
		if (gpgSigner != null
				&& (signAllCommits || commit.getRawGpgSignature() != null)) {
			gpgSigner = sign(builder, gpgSigner, gpgConfig, committer, commit);
		}
		Map<ObjectId, ObjectId> rewritten = new HashMap<>();
		String newCommitId = null;
		try (ObjectInserter inserter = repository.newObjectInserter()) {
			ObjectId newCommit = inserter.insert(builder);
			rewritten.put(commit.getId(), newCommit);
			newCommitId = newCommit.name();
			progress.worked(1);
			// Now rewrite all others: fill in new parents
			for (RevCommit c : commits) {
				RevCommit[] parents = c.getParents();
				ObjectId[] newParents = new ObjectId[parents.length];
				int i = 0;
				boolean hadNew = false;
				for (RevCommit p : parents) {
					ObjectId newId = rewritten.get(p.getId());
					if (newId != null) {
						hadNew = true;
					}
					newParents[i++] = newId != null ? newId : p.getId();
				}
				if (!hadNew) {
					continue;
				}
				committer = new PersonIdent(committer); // Update when
				builder = copy(c, newParents, committer, c.getFullMessage());
				if (gpgSigner != null
						&& (signAllCommits || c.getRawGpgSignature() != null)) {
					gpgSigner = sign(builder, gpgSigner, gpgConfig, committer,
							c);
				}
				rewritten.put(c.getId(), inserter.insert(builder));
				progress.worked(1);
			}
			inserter.flush();
		}
		// Update HEAD, and write ORIG_HEAD
		ObjectId newHeadId = rewritten.get(headId);
		RefUpdate ru = repository.updateRef(Constants.HEAD);
		ru.setExpectedOldObjectId(headId);
		ru.setNewObjectId(newHeadId);
		ru.setForceUpdate(true);
		ru.setRefLogMessage("rebase finished: returning to " + headName //$NON-NLS-1$
				+ " after having reworded commit " + newCommitId, false); //$NON-NLS-1$
		switch (ru.update(walk)) {
		case NEW:
		case NO_CHANGE:
		case FORCED:
		case FAST_FORWARD:
			break;
		default:
			throw new TeamException(MessageFormat.format(
					CoreText.RewordCommitOperation_cannotUpdateHead,
					Utils.getShortObjectId(commit)));
		}
		ObjectId origHead = ru.getOldObjectId();
		if (origHead != null) {
			// TODO: JGit should handle ORIG_HEAD as a ref to guard against
			// concurrent modifications, or there should be a way to copy
			// the old head ObjectId atomically to ORIG_HEAD on the HEAD
			// update.
			repository.writeOrigHead(origHead);
		}
		progress.worked(1);
	}

	private CommitBuilder copy(RevCommit toCopy, ObjectId[] parents,
			PersonIdent committer, String message) {
		CommitBuilder builder = new CommitBuilder();
		builder.setParentIds(parents);
		builder.setAuthor(toCopy.getAuthorIdent());
		builder.setCommitter(committer);
		builder.setEncoding(toCopy.getEncoding());
		builder.setTreeId(toCopy.getTree());
		builder.setMessage(message);
		return builder;
	}

	private GpgSigner sign(CommitBuilder builder, GpgSigner signer,
			GpgConfig config, PersonIdent committer, RevCommit original)
			throws JGitInternalException {
		PersonIdent oldCommitter = original.getCommitterIdent();
		if (committer.getName().equals(oldCommitter.getName()) && committer
				.getEmailAddress().equals(oldCommitter.getEmailAddress())) {
			// We don't sign commits that were committed by someone else. If
			// they were signed, the signature will be dropped.
			try {
				if (signer instanceof GpgObjectSigner) {
					((GpgObjectSigner) signer).signObject(builder,
							config.getSigningKey(), committer,
							CredentialsProvider.getDefault(), config);
				} else {
					signer.sign(builder, config.getSigningKey(), committer,
							CredentialsProvider.getDefault());
				}
			} catch (CanceledException e) {
				// User cancelled signing: don't sign and assume he doesn't want
				// to sign any other commit.
				return null;
			} catch (JGitInternalException
					| UnsupportedSigningFormatException e) {
				if (config.isSignCommits()) {
					if (e instanceof JGitInternalException) {
						throw (JGitInternalException) e;
					} else {
						throw new JGitInternalException(e.getMessage(), e);
					}
				}
				Activator.logWarning(MessageFormat.format(
						CoreText.RewordCommitOperation_cannotSign,
						Utils.getShortObjectId(commit),
						Utils.getShortObjectId(headId),
						Utils.getShortObjectId(original)), e);
				return null;
			}
		}
		return signer;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}
}
