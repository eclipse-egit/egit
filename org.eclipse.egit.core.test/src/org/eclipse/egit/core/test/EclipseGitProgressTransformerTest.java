/*******************************************************************************
 * Copyright (C) 2011, Adrian G&ouml;rler <adrian.goerler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.test;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EclipseGitProgressTransformerTest {

	@Mock
	IProgressMonitor eclipseMonitor;

	ProgressMonitor classUnderTest;

	@Before
	public void setup() {
		classUnderTest = new EclipseGitProgressTransformer(eclipseMonitor);
	}

	@Test
	public void testUnboundedMonitor() throws InterruptedException {
		final String title = "Title";

		classUnderTest.beginTask(title, ProgressMonitor.UNKNOWN);
		Mockito.verify(eclipseMonitor).subTask("Title");

		classUnderTest.update(10);
		classUnderTest.update(0);
		Mockito.verify(eclipseMonitor, Mockito.times(1)).subTask("Title, 10");
		// If updated too quickly, the EclipseGitProgressTransformar may skip
		// updates for performance reasons.
		Thread.sleep(TimeUnit.SECONDS.toMillis(1));
		classUnderTest.update(20);
		Mockito.verify(eclipseMonitor).subTask("Title, 30");

	}

	@Test
	public void testUnboundedMonitorFastUpdate() throws InterruptedException {
		final String title = "Title";

		classUnderTest.beginTask(title, ProgressMonitor.UNKNOWN);
		Mockito.verify(eclipseMonitor).subTask("Title");

		classUnderTest.update(10);
		classUnderTest.update(0);
		Mockito.verify(eclipseMonitor, Mockito.times(1)).subTask("Title, 10");
		for (int i = 0; i < 10; i++) {
			classUnderTest.update(10);
		}
		// If updated too quickly, the EclipseGitProgressTransformar may skip
		// updates for performance reasons. But even then the final count
		// when an update has occurred should include intermediary updates
		// that were not reported in the UI.
		Thread.sleep(TimeUnit.SECONDS.toMillis(1));
		classUnderTest.update(20);
		Mockito.verify(eclipseMonitor).subTask("Title, 130");

	}

	@Test
	public void testBoundedMonitor() {
		final String title = "Title";

		classUnderTest.beginTask(title, 50);
		Mockito.verify(eclipseMonitor).subTask("Title");

		classUnderTest.update(10);
		classUnderTest.update(0);
		Mockito.verify(eclipseMonitor, Mockito.times(1)).subTask("Title:                    20% (10/50)");
		classUnderTest.update(20);
		Mockito.verify(eclipseMonitor).subTask("Title:                    60% (30/50)");

	}

}
