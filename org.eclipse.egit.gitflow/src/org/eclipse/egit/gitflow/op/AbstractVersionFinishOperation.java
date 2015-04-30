/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.TagOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.revwalk.RevCommit;

import static java.lang.String.format;

/**
 * common logic for git flow * finish
 */
@SuppressWarnings("restriction")
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
		RevCommit head = repository.findHead();
		RevCommit commitForTag;
		try {
			commitForTag = repository.findCommitForTag(versionName);
			if (commitForTag == null) {
				createTag(monitor, head, tagName, tagMessage);
			} else if (!head.equals(commitForTag)) {
				throw new CoreException(error(format(
						CoreText.AbstractVersionFinishOperation_tagNameExists,
						versionName)));
			}
		} catch (MissingObjectException e) {
			throw new CoreException(error(e));
		} catch (IncorrectObjectTypeException e) {
			throw new CoreException(error(e));
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
		tag.setMessage(message);
		tag.setObjectId(head);
		new TagOperation(repository.getRepository(), tag, false)
				.execute(monitor);
	}
}
