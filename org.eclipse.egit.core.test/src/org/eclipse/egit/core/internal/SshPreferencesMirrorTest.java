/*******************************************************************************
 * Copyright (C) 2019 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SshPreferencesMirrorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Before
	public void prepareMirror() throws Exception {
		SshPreferencesMirror.INSTANCE.start();
	}

	@After
	public void resetPreferences() throws Exception {
		set("PRIVATEKEY", null);
	}

	private void set(String key, String value) throws Exception {
		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode("org.eclipse.jsch.core");
		if (value == null) {
			preferences.remove(key);
		} else {
			preferences.put(key, value);
		}
		preferences.flush();
	}

	@Test
	public void testAbsoluteKeyPath() throws Exception {
		File fakeSshHome = tmp.newFolder("faks_ssh_home");
		File otherDirectory = tmp.newFolder("other");
		File fakeDefaultKey = File.createTempFile("id_", "key", fakeSshHome);
		File fakeOtherKey = File.createTempFile("other", "key", otherDirectory);
		set("PRIVATEKEY", fakeDefaultKey.getName() + ','
				+ fakeOtherKey.getAbsolutePath());
		assertEquals(
				'[' + fakeDefaultKey.getAbsolutePath() + ", "
						+ fakeOtherKey.getAbsolutePath() + ']',
				SshPreferencesMirror.INSTANCE.getDefaultIdentities(fakeSshHome)
						.toString());
	}
}
