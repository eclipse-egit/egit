package org.eclipse.egit.ui.internal.commit;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

public class DiffEditorOutlinePageSortingTest {

	@Test
	public void testSort1() {
		String[] input = { "/", "org.eclipse.egit", "org.eclipse.egit.core",
				"org.eclipse.egit-feature", "org.eclipse.egit/META-INF",
				"org.eclipse.egit.core/META-INF" };
		String[] expected = { "/", "org.eclipse.egit",
				"org.eclipse.egit/META-INF", "org.eclipse.egit-feature",
				"org.eclipse.egit.core", "org.eclipse.egit.core/META-INF" };
		Arrays.sort(input, DiffEditorOutlinePage.CMP);
		assertArrayEquals(expected, input);
	}

}
