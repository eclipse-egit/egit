/*******************************************************************************
 * Copyright (C) 2011, Adrian G&ouml;rler <adrian.goerler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.test;

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
	public void testUnboundedMonitor() {
		final String title = "Title";

		classUnderTest.beginTask(title, ProgressMonitor.UNKNOWN);
		Mockito.verify(eclipseMonitor).subTask("Title");

		classUnderTest.update(10);
		classUnderTest.update(0);
		Mockito.verify(eclipseMonitor, Mockito.times(1)).subTask("Title, 10");
		classUnderTest.update(20);
		Mockito.verify(eclipseMonitor).subTask("Title, 30");

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
