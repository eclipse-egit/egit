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

import java.util.Iterator;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

abstract public class AbstractFeatureOperationTest extends
		AbstractGitFlowOperationTest {

	protected GitFlowRepository init(String initalCommit) throws Exception {
		testRepository.createInitialCommit(initalCommit);
		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		return new GitFlowRepository(repository);
	}

	protected int countCommits(Repository repository) throws GitAPIException,
			NoHeadException {
		int count = 0;
		Iterable<RevCommit> commits = Git.wrap(repository).log().call();
		Iterator<RevCommit> iterator = commits.iterator();
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
	}
}
