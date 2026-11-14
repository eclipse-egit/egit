/*******************************************************************************
 * Copyright (C) 2026, Lars Vogel <Lars.Vogel@vogella.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.egit.ui.internal.staging.StagingEntry.Action;
import org.eclipse.egit.ui.internal.staging.StagingEntry.State;
import org.junit.Test;

public class StagingEntryTest {

	@Test
	public void fileOffersReplaceActions() {
		StagingEntry entry = new StagingEntry(null, State.CONFLICTING, "file",
				e -> null);
		assertEquals(State.CONFLICTING.getAvailableActions(),
				entry.getAvailableActions());
	}

	@Test
	public void submoduleOffersNoReplaceActions() {
		for (State state : State.values()) {
			StagingEntry entry = new StagingEntry(null, state, "sub",
					e -> null);
			entry.setSubmodule(true);
			Set<Action> actions = entry.getAvailableActions();
			assertFalse(state.name(),
					actions.contains(Action.REPLACE_WITH_FILE_IN_GIT_INDEX));
			assertFalse(state.name(),
					actions.contains(Action.REPLACE_WITH_HEAD_REVISION));
			assertFalse(state.name(),
					actions.contains(Action.REPLACE_WITH_OURS_THEIRS_MENU));
		}
	}

	@Test
	public void updateSubmoduleOnlyForWorkTreeChanges() {
		Set<State> withWorkTreeChange = EnumSet.of(State.MISSING,
				State.MISSING_AND_CHANGED, State.MODIFIED,
				State.MODIFIED_AND_CHANGED, State.MODIFIED_AND_ADDED);
		for (State state : State.values()) {
			StagingEntry entry = new StagingEntry(null, state, "sub",
					e -> null);
			entry.setSubmodule(true);
			assertEquals(state.name(), withWorkTreeChange.contains(state),
					entry.getAvailableActions()
					.contains(Action.UPDATE_SUBMODULE));
			entry.setSubmodule(false);
			assertFalse(state.name(), entry.getAvailableActions()
					.contains(Action.UPDATE_SUBMODULE));
		}
	}

	@Test
	public void submoduleKeepsOtherActions() {
		StagingEntry entry = new StagingEntry(null, State.MODIFIED, "sub",
				e -> null);
		entry.setSubmodule(true);
		assertTrue(entry.getAvailableActions().contains(Action.STAGE));
		// The shared set of the state must not be modified
		assertTrue(State.MODIFIED.getAvailableActions()
				.contains(Action.REPLACE_WITH_HEAD_REVISION));
	}
}
