/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * Tags repository with given {@link TagBuilder} object.
 */
public class TagOperation implements IEGitOperation {

	private final TagBuilder tag;
	private final Repository repo;
	private final boolean shouldMoveTag;

	/**
	 * Construct TagOperation
	 *
	 * @param repo
	 * @param tag
	 * @param shouldMoveTag if <code>true</code> it will replace tag with same name
	 */
	public TagOperation(Repository repo, TagBuilder tag, boolean shouldMoveTag) {
		this.tag = tag;
		this.repo = repo;
		this.shouldMoveTag = shouldMoveTag;
	}


	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		progress.setTaskName(NLS.bind(CoreText.TagOperation_performingTagging,
				tag.getTag()));
		ObjectId tagId = updateTagObject();
		progress.worked(1);
		updateRepo(tagId);
		progress.worked(1);
	}

	private void updateRepo(ObjectId tagId) throws TeamException {
		String refName = Constants.R_TAGS + tag.getTag();

		try {
			RefUpdate tagRef = repo.updateRef(refName);
			tagRef.setNewObjectId(tagId);

			tagRef.setForceUpdate(shouldMoveTag);
			Result updateResult = tagRef.update();

			if (updateResult != Result.NEW && updateResult != Result.FORCED)
				throw new TeamException(NLS.bind(CoreText.TagOperation_taggingFailure,
						tag.getTag(), updateResult));
		} catch (IOException e) {
			throw new TeamException(NLS.bind(CoreText.TagOperation_taggingFailure,
					tag.getTag(), e.getMessage()), e);
		}
	}

	private ObjectId updateTagObject() throws TeamException {
		ObjectId startPointRef = tag.getObjectId();

		try {
			ObjectId tagId;
			repo.open(startPointRef);
			try (ObjectInserter inserter = repo.newObjectInserter()) {
				tagId = inserter.insert(tag);
				inserter.flush();
			}
			return tagId;
		} catch (IOException e) {
			throw new TeamException(NLS.bind(CoreText.TagOperation_objectIdNotFound,
					tag.getTag(), e.getMessage()), e);
		}
	}


	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

}
