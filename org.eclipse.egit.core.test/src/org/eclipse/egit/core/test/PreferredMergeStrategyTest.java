/*******************************************************************************
 * Copyright (C) 2015 Obeo and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.jgit.merge.MergeStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PreferredMergeStrategyTest {

	private Activator a;

	/**
	 * Removes any preference about preferred merge strategy before any test.
	 */
	@Before
	public void setUp() {
		a = Activator.getDefault();
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).remove(
				GitCorePreferences.core_preferredMergeStrategy);
	}

	/**
	 * Removes any preference about preferred merge strategy after any test.
	 */
	@After
	public void tearDown() {
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).remove(
				GitCorePreferences.core_preferredMergeStrategy);
	}

	@Test
	public void testGetDefaultPreferredMergeStrategy() {
		assertNull(a.getPreferredMergeStrategy());
	}

	@Test
	public void testGetPreferredMergeStrategyWhenNoPref() {
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).remove(
				GitCorePreferences.core_preferredMergeStrategy);

		assertNull(a.getPreferredMergeStrategy());
	}

	@Test
	public void testGetPreferredMergeStrategyWhenInvalidPreference() {
		// Using "invalid value" simulates a property set to a merge strategy
		// that's no longer registered
		InstanceScope.INSTANCE.getNode(Activator.getPluginId())
				.put(GitCorePreferences.core_preferredMergeStrategy,
						"invalid value");

		assertNull(a.getPreferredMergeStrategy());
	}

	@Test
	public void testGetPreferredMergeStrategyWhenValidPreference() {
		// Using "resolve" here because there's no need for more configuration
		// in this test, it is a registered MergeStrategy
		InstanceScope.INSTANCE.getNode(Activator.getPluginId()).put(
				GitCorePreferences.core_preferredMergeStrategy, "resolve");

		assertSame(MergeStrategy.RESOLVE, a.getPreferredMergeStrategy());
	}
}
