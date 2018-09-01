/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.WrongGitFlowStateException;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;

/**
 * common logic for git flow * finish
 */
abstract public class AbstractVersionFinishOperation extends GitFlowOperation {
	/** */
	protected String versionName;

	/**
	 * @param repository
	 * @param versionName
	 */
	public AbstractVersionFinishOperation(GitFlowRepository repository,
			String versionName) {
		super(repository);
		this.versionName = versionName;
	}

	/**
	 * Check if tag exists before trying to create it.
	 *
	 * @param monitor
	 * @param tagName
	 * @param tagMessage
	 * @throws CoreException
	 */
	protected void safeCreateTag(IProgressMonitor monitor, String tagName,
			String tagMessage) throws CoreException {
		RevCommit head;
		try {
			head = repository.findHead();
		} catch (WrongGitFlowStateException e) {
			throw new CoreException(error(e));
		}
		RevCommit commitForTag;
		try {
			commitForTag = repository.findCommitForTag(versionName);
			if (commitForTag == null) {
				createTag(monitor, head, tagName, tagMessage);
			} else if (!head.equals(commitForTag)) {
				throw new CoreException(error(NLS.bind(
						CoreText.AbstractVersionFinishOperation_tagNameExists,
						versionName)));
			}
		} catch (IOException e) {
			throw new CoreException(error(e));
		}
	}

	/**
	 * @param monitor
	 * @param head
	 * @param name
	 * @param message
	 * @throws CoreException
	 */
	protected void createTag(IProgressMonitor monitor, RevCommit head,
			String name, String message) throws CoreException {
		TagBuilder tag = new TagBuilder();
		tag.setTag(name);
		tag.setTagger(new PersonIdent(repository.getRepository()));
		tag.setMessage(message);
		tag.setObjectId(head);
		new TagOperation(repository.getRepository(), tag, false)
				.execute(monitor);
	}
}
