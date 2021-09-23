package org.eclipse.egit.ui.internal.repository.tree.command;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class PasteCommandTest {

	@Test
	public void testUrlOnly() {
		assertPasted(
				"https://git.eclipse.org/r/gmf-runtime/org.eclipse.gmf-runtime");
	}

	@Test
	public void testGitCloneCommand() {
		assertPasted(
				"git clone https://git.eclipse.org/r/gmf-runtime/org.eclipse.gmf-runtime");
	}

	@Test
	public void testGitCloneCommandWithQuotes() {
		assertPasted(
				"git clone \"https://git.eclipse.org/r/gmf-runtime/org.eclipse.gmf-runtime\"");
	}

	private void assertPasted(String clipboard) {
		assertNotNull(PasteCommand.getCloneURI(clipboard));
	}

}
