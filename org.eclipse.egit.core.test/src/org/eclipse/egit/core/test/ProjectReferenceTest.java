package org.eclipse.egit.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;

import org.eclipse.egit.core.ProjectReference;
import org.junit.Test;

public class ProjectReferenceTest {

	@Test
	public void testCreateProjectReference() throws IllegalArgumentException, URISyntaxException {
		String version = "1.0";
		String url = "git://egit.eclipse.org/egit.git";
		String branch = "master";
		String project = "org.eclipse.egit.core";
		String reference = version + "," + url + "," + branch + "," + project;

		ProjectReference projectReference = new ProjectReference(reference);

		assertNotNull(projectReference);
		assertEquals(url, projectReference.getRepository().toString());
		assertEquals(branch, projectReference.getBranch());
		assertEquals(project, projectReference.getProjectDir());
	}

}
