/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Edwin Kempin <edwin.kempin@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.securestorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EGitSecureStoreTest {

	ISecurePreferences secureStoreForTest;

	EGitSecureStore store;

	@Before
	public void setUp() throws Exception {
		setupNewSecureStore();
		store = new EGitSecureStore(secureStoreForTest);
	}

	@After
	public void tearDown() throws Exception {
		secureStoreForTest.flush();
	}

	@Test
	public void testPutUserAndPassword() throws Exception {
		URIish uri = new URIish("http://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);

		ISecurePreferences node = secureStoreForTest.node(EGitSecureStore
				.calcNodePath(uri).toString());
		assertEquals("agitter", node.get("user", null));
		assertTrue(node.isEncrypted("password"));
		assertEquals("letmein", node.get("password", null));
	}

	@Test
	public void testGetUserAndPassword() throws Exception {
		URIish uri = new URIish("http://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);

		UserPasswordCredentials storedCredentials = store.getCredentials(uri);
		assertEquals("agitter", storedCredentials.getUser());
		assertEquals("letmein", storedCredentials.getPassword());
	}

	@Test
	public void testGetUserAndPasswordUnknownURI() throws Exception {
		URIish uri = new URIish("http://testRepo.example.com/testrepo");

		UserPasswordCredentials storedCredentials = store.getCredentials(uri);
		assertNull(storedCredentials);
	}

	@Test
	public void testPutUserAndPasswordURIContainingUserAndPass()
			throws Exception {
		URIish uri = new URIish(
				"http://user:pass@testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);

		ISecurePreferences node = secureStoreForTest.node(EGitSecureStore
				.calcNodePath(uri).toString());
		assertEquals("agitter", node.get("user", null));
		assertTrue(node.isEncrypted("password"));
		assertEquals("letmein", node.get("password", null));
	}

	@Test
	public void testGetUserAndPasswordURIContainingUserAndPass()
			throws Exception {
		store.putCredentials(
				new URIish("http://testRepo.example.com/testrepo"),
				new UserPasswordCredentials("agitter", "letmein"));
		UserPasswordCredentials credentials = store.getCredentials(new URIish(
				"http://agitter:letmein@testRepo.example.com/testrepo"));
		assertEquals("agitter", credentials.getUser());
		assertEquals("letmein", credentials.getPassword());
	}

	@Test
	public void testGetUserAndPasswordURIContainingOtherUserAndPass()
			throws Exception {
		store.putCredentials(
				new URIish("http://testRepo.example.com/testrepo"),
				new UserPasswordCredentials("agitter", "letmein"));
		assertNull(store.getCredentials(new URIish(
				"http://otheruser:otherpass@testRepo.example.com/testrepo")));
	}

	@Test
	public void testClearCredentials() throws Exception {
		URIish uri = new URIish("http://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		store.clearCredentials(uri);
		assertEquals(null, store.getCredentials(uri));
	}

	@Test
	public void testEnsureDefaultPortHttp() throws Exception {
		URIish uri = new URIish("http://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		URIish uri2 = new URIish("http://testRepo.example.com:80/testrepo");
		assertEquals(credentials.getUser(), store.getCredentials(uri2).getUser());
		assertEquals(credentials.getPassword(), store.getCredentials(uri2).getPassword());
	}

	@Test
	public void testEnsureDefaultPortHttps() throws Exception {
		URIish uri = new URIish("https://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		URIish uri2 = new URIish("https://testRepo.example.com:443/testrepo");
		assertEquals(credentials.getUser(), store.getCredentials(uri2).getUser());
		assertEquals(credentials.getPassword(), store.getCredentials(uri2).getPassword());
	}

	@Test
	public void testEnsureDefaultPortSftp() throws Exception {
		URIish uri = new URIish("sftp://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		URIish uri2 = new URIish("sftp://testRepo.example.com:22/testrepo");
		assertEquals(credentials.getUser(), store.getCredentials(uri2).getUser());
		assertEquals(credentials.getPassword(), store.getCredentials(uri2).getPassword());
	}

	@Test
	public void testEnsureDefaultPortFtp() throws Exception {
		URIish uri = new URIish("ftp://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		URIish uri2 = new URIish("ftp://testRepo.example.com:21/testrepo");
		assertEquals(credentials.getUser(), store.getCredentials(uri2).getUser());
		assertEquals(credentials.getPassword(), store.getCredentials(uri2).getPassword());
	}

	@Test
	public void testEnsureDefaultPortSsh() throws Exception {
		URIish uri = new URIish("ssh://agitter@testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		URIish uri2 = new URIish("ssh://testRepo.example.com:22/testrepo");
		assertEquals(credentials.getUser(), store.getCredentials(uri2).getUser());
		assertEquals(credentials.getPassword(), store.getCredentials(uri2).getPassword());
	}

	@Test
	public void testClearCredentialsTwice() throws Exception {
		URIish uri = new URIish("http://testRepo.example.com/testrepo");
		UserPasswordCredentials credentials = new UserPasswordCredentials(
				"agitter", "letmein");
		store.putCredentials(uri, credentials);
		store.clearCredentials(uri);
		assertEquals(null, store.getCredentials(uri));
		store.clearCredentials(uri);
		assertEquals(null, store.getCredentials(uri));
	}

	private void setupNewSecureStore() throws IOException,
			MalformedURLException {
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(IProviderHints.DEFAULT_PASSWORD, new PBEKeySpec(
				"masterpass".toCharArray()));
		String secureStorePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().append("testSecureStore").toOSString();
		File file = new File(secureStorePath);
		if (file.exists())
			FileUtils.delete(file, FileUtils.RECURSIVE | FileUtils.RETRY);
		URL url = file.toURI().toURL();
		secureStoreForTest = SecurePreferencesFactory.open(url, options);
		secureStoreForTest.node("/GIT").removeNode();
	}

}
