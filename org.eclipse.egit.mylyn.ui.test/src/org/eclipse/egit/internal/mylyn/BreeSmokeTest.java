/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn;

import static org.junit.Assert.assertNotNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.junit.Test;

/**
 * Simple test to guard against build setup errors such as --release not being
 * effective.
 */
public class BreeSmokeTest {

	@Test
	public void testByteBuffer() {
		// This test will fail if compiled against a Java 11 library without
		// --release 8 and then run on a Java 8 JVM with Java 8 libraries
		ByteBuffer buffer = ByteBuffer.allocate(10);
		Buffer flipped = buffer.flip();
		assertNotNull(flipped);
	}
}
