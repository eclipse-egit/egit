/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jgit.lib.Repository;

abstract public class AbstractFeatureOperationTest extends
		AbstractGitFlowOperationTest {

	protected GitFlowRepository init(String initalCommit) throws Exception {
		testRepository.createInitialCommit(initalCommit);
		Repository repository = testRepository.getRepository();
		new InitOperation(repository).execute(null);
		return new GitFlowRepository(repository);
	}
}
