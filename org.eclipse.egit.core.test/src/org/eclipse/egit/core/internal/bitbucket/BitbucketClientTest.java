/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.bitbucket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

/**
 * Tests for {@link BitbucketClient}
 */
public class BitbucketClientTest {

	@Test
	public void testClientConstruction() {
		BitbucketClient client = new BitbucketClient(
				"https://bitbucket.example.com", "test-token"); //$NON-NLS-1$ //$NON-NLS-2$
		assertThat(client, notNullValue());
	}

	@Test
	public void testClientConstructionWithTrailingSlash() {
		BitbucketClient client = new BitbucketClient(
				"https://bitbucket.example.com/", "test-token"); //$NON-NLS-1$ //$NON-NLS-2$
		assertThat(client, notNullValue());
	}

	@Test
	public void testPullRequestModel() {
		PullRequest pr = new PullRequest();
		pr.setId(123);
		pr.setTitle("Test PR"); //$NON-NLS-1$
		pr.setState("OPEN"); //$NON-NLS-1$

		assertThat(pr.getId(), equalTo(123L));
		assertThat(pr.getTitle(), equalTo("Test PR")); //$NON-NLS-1$
		assertThat(pr.getState(), equalTo("OPEN")); //$NON-NLS-1$
	}

	@Test
	public void testPullRequestParticipant() {
		PullRequest.User user = new PullRequest.User();
		user.setName("jdoe"); //$NON-NLS-1$
		user.setDisplayName("John Doe"); //$NON-NLS-1$
		user.setEmailAddress("jdoe@example.com"); //$NON-NLS-1$

		PullRequest.PullRequestParticipant participant = new PullRequest.PullRequestParticipant();
		participant.setUser(user);
		participant.setRole("AUTHOR"); //$NON-NLS-1$
		participant.setApproved(false);

		assertThat(participant.getUser().getName(), equalTo("jdoe")); //$NON-NLS-1$
		assertThat(participant.getUser().getDisplayName(),
				equalTo("John Doe")); //$NON-NLS-1$
		assertThat(participant.getRole(), equalTo("AUTHOR")); //$NON-NLS-1$
		assertThat(participant.isApproved(), equalTo(false));
	}

	@Test
	public void testPullRequestRef() {
		PullRequest.Project project = new PullRequest.Project();
		project.setKey("PROJ"); //$NON-NLS-1$
		project.setName("Test Project"); //$NON-NLS-1$

		PullRequest.Repository repo = new PullRequest.Repository();
		repo.setSlug("test-repo"); //$NON-NLS-1$
		repo.setName("Test Repository"); //$NON-NLS-1$
		repo.setProject(project);

		PullRequest.PullRequestRef ref = new PullRequest.PullRequestRef();
		ref.setId("refs/heads/feature/test"); //$NON-NLS-1$
		ref.setDisplayId("feature/test"); //$NON-NLS-1$
		ref.setRepository(repo);

		assertThat(ref.getId(), equalTo("refs/heads/feature/test")); //$NON-NLS-1$
		assertThat(ref.getDisplayId(), equalTo("feature/test")); //$NON-NLS-1$
		assertThat(ref.getRepository().getSlug(), equalTo("test-repo")); //$NON-NLS-1$
		assertThat(ref.getRepository().getProject().getKey(),
				equalTo("PROJ")); //$NON-NLS-1$
	}
}
