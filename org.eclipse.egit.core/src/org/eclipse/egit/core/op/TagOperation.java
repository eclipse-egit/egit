/*******************************************************************************
 * Copyright (C) 2010, Darusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;

/**
 * Tags repository with given {@link Tag} object.
 *
 */
public class TagOperation implements IEGitOperation {

	private final Tag tag;
	private final String tagName;
	private final Repository repo;
	private final String refName;
	private final boolean shouldMoveTag;

	/**
	 *
	 * @param repo
	 * @param tag
	 * @param shouldMoveTag
	 */
	public TagOperation(Repository repo, Tag tag, boolean shouldMoveTag) {
		this.tag = tag;
		this.repo = repo;
		tagName = tag.getTag();
		this.shouldMoveTag = shouldMoveTag;
		refName = Constants.R_TAGS + tagName;
	}


	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(NLS.bind(CoreText.TagOperation_performingTagging,
					tagName), 3);

			updateTagObject();
			monitor.worked(1);

			updateRepo();
			monitor.worked(1);

			ProjectUtil.refreshProjects(repo, SubMonitor.convert(monitor, 1));
		} finally {
			monitor.done();
		}
	}

	private void updateRepo() throws TeamException {
		try {
			RefUpdate tagRef = repo.updateRef(refName);
			tagRef.setNewObjectId(tag.getTagId());
			tagRef.setRefLogMessage("tag: " + refName, false); //$NON-NLS-1$

			tagRef.setForceUpdate(shouldMoveTag);
			Result updateResult = tagRef.update();

			if (updateResult != Result.NEW && updateResult != Result.FORCED)
				throw new TeamException(NLS.bind(CoreText.TagOperation_taggingFailure,
						tagName));
		} catch (IOException e) {
			throw new TeamException(NLS.bind(CoreText.TagOperation_taggingFailure,
					tagName), e);
		}
	}

	private void updateTagObject() throws TeamException {
		ObjectId startPointRef = tag.getObjId();

		try {
			ObjectLoader object = repo.openObject(startPointRef);
			tag.setType(Constants.typeString(object.getType()));
			ObjectWriter objWriter = new ObjectWriter(repo);
			tag.setTagId(objWriter.writeTag(tag));
		} catch (IOException e) {
			throw new TeamException(NLS.bind(CoreText.TagOperation_objectIdNotFound,
					tagName), e);
		}
	}

}
