/*******************************************************************************
 * Copyright (C) 2026, Lars Vogel <Lars.Vogel@vogella.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link CommitProposalProcessor}.
 * <p>
 * Regression coverage for a bug where old commit messages that did not start
 * with the literal lower-cased prefix were silently filtered out of the
 * commit-message content assist. The content assist lower-cased the prefix
 * but compared it against the raw, mixed-case message text, so a user typing
 * "fix" would not see the proposal "Fix NPE in parser".
 */
public class CommitProposalProcessorTest {

	@Test
	public void emptyPrefixMatchesAnyCandidate() {
		assertTrue(CommitProposalProcessor.matchesPrefix("Anything", ""));
		assertTrue(CommitProposalProcessor.matchesPrefix("", ""));
	}

	@Test
	public void prefixMatchIsCaseInsensitive() {
		assertTrue(CommitProposalProcessor.matchesPrefix("Fix NPE", "fix"));
		assertTrue(CommitProposalProcessor.matchesPrefix("FIX NPE", "fix"));
		assertTrue(CommitProposalProcessor.matchesPrefix("fix NPE", "fix"));
	}

	@Test
	public void prefixMatchPreservesUmlautCase() {
		assertTrue(CommitProposalProcessor.matchesPrefix("Über flow", "über"));
		assertTrue(CommitProposalProcessor.matchesPrefix("ÜBER flow", "über"));
	}

	@Test
	public void prefixLongerThanCandidateDoesNotMatch() {
		assertFalse(CommitProposalProcessor.matchesPrefix("fix", "fixed"));
	}

	@Test
	public void nonMatchingPrefixReturnsFalse() {
		assertFalse(
				CommitProposalProcessor.matchesPrefix("Add feature", "fix"));
	}

	@Test
	public void nullInputsAreSafe() {
		assertFalse(CommitProposalProcessor.matchesPrefix(null, "fix"));
		assertFalse(CommitProposalProcessor.matchesPrefix("Fix NPE", null));
	}
}
