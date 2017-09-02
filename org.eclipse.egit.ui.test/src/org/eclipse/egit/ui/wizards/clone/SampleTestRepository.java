/*******************************************************************************
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.http.SimpleHttpServer;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.transport.Daemon;
import org.eclipse.jgit.transport.DaemonClient;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.util.FileUtils;

/**
 * Creates an on disk sample repository with some generated content and starts a
 * git daemon on a free port.
 *
 * If the system property <code>test-repo-no-cleanup</code> is defined the
 * source repository data will not be deleted from disk to enable testing the
 * test.
 */
public class SampleTestRepository {
	/**
	 * Name of the test repository
	 */
	public static final String REPO_NAME = "test";

	/**
	 * Name of a branch in the sample repository
	 */
	public static final String FIX = "fix";

	/**
	 * Name of a tag in the sample repository
	 */
	public static final String v1_0_name = "v1_0";

	/**
	 * Name of a tag in the sample repository
	 */
	public static final String v2_0_name = "v2_0";

	/**
	 * Name of a file in the sample repository
	 */
	public static final String A_txt_name = "A_txt";

	private static final File trash = new File("target/trash");

	private final TestRepository<Repository> src;

	private Daemon d;

	private SimpleHttpServer httpServer;

	private String uri;

	private String secureUri;

	private RevBlob A_txt;

	private RevCommit A, B, C;

	private RevTag v1_0, v2_0;

	private final boolean serveHttp;

	public String getUri() {
		return uri;
	}

	public String getSecureUri() {
		return secureUri;
	}

	/**
	 * Create a bare repository, generate some sample data and start git daemon
	 * on a free port
	 *
	 * @param n
	 *            hint how many random commits should be generated
	 * @param serveHttp
	 *            whether to serve the repository over HTTP
	 *
	 * @throws Exception
	 */
	public SampleTestRepository(int n, boolean serveHttp) throws Exception {
		this(n, serveHttp, false);
	}

	/**
	 * Create a bare repository, generate some sample data and start git daemon
	 * on a free port
	 *
	 * @param n
	 *            hint how many random commits should be generated
	 * @param serveHttp
	 *            whether to serve the repository over HTTP
	 * @param withSsl
	 *            whether to server the repository also over HTTPS if
	 *            {@code serveHttp} is {@code true}
	 *
	 * @throws Exception
	 */
	public SampleTestRepository(int n, boolean serveHttp, boolean withSsl)
			throws Exception {
		this.serveHttp = serveHttp;
		src = createRepository();
		generateSampleData(n);
		if (serveHttp)
			serveHttp(withSsl);
		else
			serve();
	}

	private TestRepository<Repository> createRepository() throws Exception {
		String gitdirName = "test" + System.currentTimeMillis()
				+ Constants.DOT_GIT;
		File gitdir = new File(trash, gitdirName).getCanonicalFile();
		Repository db = new RepositoryBuilder().setGitDir(gitdir).build();
		assertFalse(gitdir.exists());
		db.create(true);
		return new TestRepository<>(db);
	}

	private void generateSampleData(int n) throws Exception {
		A_txt = src.blob("A");
		A = src.commit().add(A_txt_name, A_txt).create();
		src.update(Constants.R_HEADS + Constants.MASTER, A);

		// create some random commits
		RevCommit X = A;
		for (int i = 0; i < n; i++) {
			X = src.commit().parent(X)
					.add(randomAsciiString(), randomAsciiString()).create();
		}

		B = src.commit().parent(X).add(A_txt_name, "C").add("B", "B").create();
		src.update(Constants.R_HEADS + Constants.MASTER, B);

		v1_0 = src.tag(v1_0_name, B);
		src.update(Constants.R_TAGS + v1_0_name, v1_0);

		C = src.commit().parent(A).add(A_txt_name, "D").add("C", "C").create();
		src.update(Constants.R_HEADS + FIX, C);

		v2_0 = src.tag(v2_0_name, C);
		src.update(Constants.R_TAGS + v2_0_name, v2_0);
	}

	private String randomAsciiString() {
		StringBuilder randstring = new StringBuilder("");
		Random rand = new Random();
		int strlen = rand.nextInt(20) + 10;
		for (int i = 0, j = 0; i < strlen; i++) {
			if (rand.nextInt(2) == 1)
				j = 97;
			else
				j = 65;
			randstring.append((char) (rand.nextInt(26) + j));
		}
		return randstring.toString();
	}

	private void serve() throws IOException {
		d = new Daemon();
		FileResolver<DaemonClient> resolver = new FileResolver<>();
		resolver.exportRepository(REPO_NAME, src.getRepository());
		d.setRepositoryResolver(resolver);
		d.start();
		uri = "git://localhost:" + d.getAddress().getPort() + "/" + REPO_NAME
				+ Constants.DOT_GIT_EXT;
	}

	private void serveHttp(boolean withSsl) throws Exception {
		httpServer = new SimpleHttpServer(src.getRepository(), withSsl);
		httpServer.start();
		uri = httpServer.getUri().toString();
		if (withSsl) {
			secureUri = httpServer.getSecureUri().toString();
		}
	}

	/**
	 * Stop the git daemon and delete test data from disk. If the system
	 * property <code>test-repo-no-cleanup</code> is defined the test data will
	 * be left on disk for analysis.
	 *
	 * @throws Exception
	 *             deletion of test repository failed
	 * @throws IOException
	 *             deletion of test repository failed
	 */
	public void shutDown() throws Exception {
		src.getRepository().close();
		if (serveHttp)
			httpServer.stop();
		else
			d.stop();

		if (!System.getProperties().contains("test-repo-no-cleanup"))
			FileUtils.delete(trash, FileUtils.RECURSIVE | FileUtils.RETRY);
	}

}
