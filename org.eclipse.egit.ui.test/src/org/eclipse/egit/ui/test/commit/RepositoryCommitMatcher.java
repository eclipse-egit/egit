/*******************************************************************************
 * Copyright (C) 2014 Andreas Hermann <a.v.hermann@gmail.com>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test.commit;

import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher that checks that the fields of two repository commits are the same.
 */
public final class RepositoryCommitMatcher extends
		TypeSafeMatcher<RepositoryCommit> {
	private final RepositoryCommit wanted;

	public static TypeSafeMatcher<? super RepositoryCommit> isSameCommit(
			final RepositoryCommit wanted) {
		return new RepositoryCommitMatcher(wanted);
	}

	public RepositoryCommitMatcher(RepositoryCommit wanted) {
		this.wanted = wanted;
	}

	@Override
	protected boolean matchesSafely(RepositoryCommit actual) {
		if (!actual.getRepository().getDirectory()
				.equals(wanted.getRepository().getDirectory()))
			return false;

		if (!actual.getRevCommit().name()
				.equals(wanted.getRevCommit().name()))
			return false;

		if (actual.isStash() != wanted.isStash())
			return false;

		return true;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("commit id=")
				.appendValue(wanted.abbreviate())
				.appendText(", isStash=")
				.appendValue(Boolean.valueOf(wanted.isStash()));
	}

	@Override
	protected void describeMismatchSafely(RepositoryCommit actual,
			Description mismatchDescription) {
		mismatchDescription.appendText("was commit id=")
				.appendValue(actual.abbreviate())
				.appendText(", isStash=")
				.appendValue(Boolean.valueOf(actual.isStash()));
	}
}
