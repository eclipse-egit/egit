package org.eclipse.egit.gitflow;

import static org.junit.Assert.assertNotNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.junit.Test;

public class BreeSmokeTest {

	@Test
	public void testByteBuffer() {
		// This test will fail if compiled with target=1.8 against a Java 11
		// library and then run on a Java 8 JVM with Java 8 libraries
		ByteBuffer buffer = ByteBuffer.allocate(10);
		Buffer flipped = buffer.flip();
		assertNotNull(flipped);
	}
}
