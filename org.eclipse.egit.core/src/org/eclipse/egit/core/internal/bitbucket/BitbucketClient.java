/*******************************************************************************
 * Copyright (C) 2026, Eclipse EGit contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.internal.bitbucket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

/**
 * REST client for Bitbucket Data Center API
 */
public class BitbucketClient {

	private static final String API_BASE_PATH = "/rest/api/1.0"; //$NON-NLS-1$

	private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds

	private final String serverUrl;

	private final String token;

	/**
	 * Creates a new Bitbucket client
	 *
	 * @param serverUrl
	 *            the Bitbucket server URL (e.g., https://bitbucket.example.com)
	 * @param token
	 *            the personal access token for authentication
	 */
	public BitbucketClient(@NonNull String serverUrl, @NonNull String token) {
		this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl; //$NON-NLS-1$
		this.token = token;
	}

	/**
	 * Retrieves pull requests for a specific repository
	 *
	 * @param projectKey
	 *            the project key (e.g., "PROJ")
	 * @param repositorySlug
	 *            the repository slug (e.g., "my-repo")
	 * @param state
	 *            the PR state filter ("OPEN", "MERGED", "DECLINED", or null for
	 *            all)
	 * @param limit
	 *            the maximum number of results per page (max 1000)
	 * @param start
	 *            the start index for pagination
	 * @return the JSON response as a string
	 * @throws IOException
	 *             if the request fails
	 */
	public String getPullRequests(@NonNull String projectKey,
			@NonNull String repositorySlug, @Nullable String state,
			int limit, int start) throws IOException {
		return getPullRequests(projectKey, repositorySlug, state, null, null,
				limit, start);
	}

	/**
	 * Retrieves pull requests for a specific repository with user filters
	 *
	 * @param projectKey
	 *            the project key (e.g., "PROJ")
	 * @param repositorySlug
	 *            the repository slug (e.g., "my-repo")
	 * @param state
	 *            the PR state filter ("OPEN", "MERGED", "DECLINED", or null for
	 *            all)
	 * @param authorUsername
	 *            filter by author username (or null for all)
	 * @param reviewerUsername
	 *            filter by reviewer username (or null for all)
	 * @param limit
	 *            the maximum number of results per page (max 1000)
	 * @param start
	 *            the start index for pagination
	 * @return the JSON response as a string
	 * @throws IOException
	 *             if the request fails
	 */
	public String getPullRequests(@NonNull String projectKey,
			@NonNull String repositorySlug, @Nullable String state,
			@Nullable String authorUsername, @Nullable String reviewerUsername,
			int limit, int start) throws IOException {
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append(serverUrl).append(API_BASE_PATH)
				.append("/projects/").append(projectKey) //$NON-NLS-1$
				.append("/repos/").append(repositorySlug) //$NON-NLS-1$
				.append("/pull-requests"); //$NON-NLS-1$

		urlBuilder.append("?limit=").append(Math.min(limit, 1000)); //$NON-NLS-1$
		urlBuilder.append("&start=").append(start); //$NON-NLS-1$

		if (state != null && !state.isEmpty()) {
			urlBuilder.append("&state=").append(state); //$NON-NLS-1$
		}

		if (authorUsername != null && !authorUsername.isEmpty()) {
			urlBuilder.append("&username.1=").append(authorUsername); //$NON-NLS-1$
			urlBuilder.append("&role.1=AUTHOR"); //$NON-NLS-1$
		}

		if (reviewerUsername != null && !reviewerUsername.isEmpty()) {
			urlBuilder.append("&participant.username=") //$NON-NLS-1$
					.append(reviewerUsername);
		}

		String url = urlBuilder.toString();
		return executeGet(url);
	}

	/**
	 * Retrieves a specific pull request by ID
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param pullRequestId
	 *            the pull request ID
	 * @return the JSON response as a string
	 * @throws IOException
	 *             if the request fails
	 */
	public String getPullRequest(@NonNull String projectKey,
			@NonNull String repositorySlug, long pullRequestId)
			throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/pull-requests/" + pullRequestId; //$NON-NLS-1$

		return executeGet(url);
	}

	/**
	 * Tests the connection to the Bitbucket server
	 *
	 * @return true if the connection is successful
	 */
	public boolean testConnection() {
		try {
			String url = serverUrl + API_BASE_PATH + "/application-properties"; //$NON-NLS-1$
			executeGet(url);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Gets the current authenticated user's information
	 *
	 * @return JSON response with user information including "name" field
	 * @throws IOException
	 *             if the request fails
	 */
	public String getCurrentUser() throws IOException {
		String url = serverUrl + API_BASE_PATH + "/users/current"; //$NON-NLS-1$
		return executeGet(url);
	}

	/**
	 * Retrieves changed files for a pull request
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param pullRequestId
	 *            the pull request ID
	 * @return the JSON response containing changed files
	 * @throws IOException
	 *             if the request fails
	 */
	public String getPullRequestChanges(@NonNull String projectKey,
			@NonNull String repositorySlug, long pullRequestId)
			throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/pull-requests/" + pullRequestId + "/changes"; //$NON-NLS-1$ //$NON-NLS-2$

		return executeGet(url);
	}

	/**
	 * Retrieves raw file content at a specific commit
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param commitId
	 *            the commit SHA or branch name
	 * @param path
	 *            the file path
	 * @return raw file content as byte array
	 * @throws IOException
	 *             if the request fails
	 */
	public byte[] getFileContent(@NonNull String projectKey,
			@NonNull String repositorySlug, @NonNull String commitId,
			@NonNull String path) throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/raw/" + path + "?at=" + commitId; //$NON-NLS-1$ //$NON-NLS-2$

		return executeGetBinary(url);
	}

	/**
	 * Retrieves activities for a pull request, including comments
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param pullRequestId
	 *            the pull request ID
	 * @return the JSON response containing activities
	 * @throws IOException
	 *             if the request fails
	 */
	public String getPullRequestActivities(@NonNull String projectKey,
			@NonNull String repositorySlug, long pullRequestId)
			throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/pull-requests/" + pullRequestId + "/activities"; //$NON-NLS-1$ //$NON-NLS-2$

		return executeGet(url);
	}

	/**
	 * Adds a comment to a pull request, optionally as a reply to an existing
	 * comment
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param pullRequestId
	 *            the pull request ID
	 * @param text
	 *            the comment text
	 * @param parentCommentId
	 *            the parent comment ID for replies, or -1 for top-level
	 *            comments
	 * @return the JSON response containing the created comment
	 * @throws IOException
	 *             if the request fails
	 */
	public String addPullRequestComment(@NonNull String projectKey,
			@NonNull String repositorySlug, long pullRequestId,
			@NonNull String text, long parentCommentId) throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/pull-requests/" + pullRequestId + "/comments"; //$NON-NLS-1$ //$NON-NLS-2$

		StringBuilder json = new StringBuilder();
		json.append("{\"text\": \"").append(escapeJson(text)).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (parentCommentId >= 0) {
			json.append(", \"parent\": {\"id\": ").append(parentCommentId) //$NON-NLS-1$
					.append("}"); //$NON-NLS-1$
		}
		json.append("}"); //$NON-NLS-1$

		return executePost(url, json.toString());
	}

	/**
	 * Updates the severity of a pull request comment (e.g. to create a task)
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param pullRequestId
	 *            the pull request ID
	 * @param commentId
	 *            the comment ID
	 * @param version
	 *            the current comment version (for optimistic locking)
	 * @param severity
	 *            the new severity ("NORMAL" or "BLOCKER")
	 * @return the JSON response containing the updated comment
	 * @throws IOException
	 *             if the request fails
	 */
	public String updateCommentSeverity(@NonNull String projectKey,
			@NonNull String repositorySlug, long pullRequestId,
			long commentId, int version, @NonNull String severity)
			throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/pull-requests/" + pullRequestId //$NON-NLS-1$
				+ "/comments/" + commentId; //$NON-NLS-1$

		String json = "{\"severity\": \"" + severity + "\", \"version\": " //$NON-NLS-1$ //$NON-NLS-2$
				+ version + "}"; //$NON-NLS-1$

		return executePut(url, json);
	}

	/**
	 * Updates the state of a pull request comment (e.g. to resolve a task)
	 *
	 * @param projectKey
	 *            the project key
	 * @param repositorySlug
	 *            the repository slug
	 * @param pullRequestId
	 *            the pull request ID
	 * @param commentId
	 *            the comment ID
	 * @param version
	 *            the current comment version (for optimistic locking)
	 * @param state
	 *            the new state ("OPEN" or "RESOLVED")
	 * @return the JSON response containing the updated comment
	 * @throws IOException
	 *             if the request fails
	 */
	public String updateCommentState(@NonNull String projectKey,
			@NonNull String repositorySlug, long pullRequestId,
			long commentId, int version, @NonNull String state)
			throws IOException {
		String url = serverUrl + API_BASE_PATH + "/projects/" + projectKey //$NON-NLS-1$
				+ "/repos/" + repositorySlug //$NON-NLS-1$
				+ "/pull-requests/" + pullRequestId //$NON-NLS-1$
				+ "/comments/" + commentId; //$NON-NLS-1$

		String json = "{\"state\": \"" + state + "\", \"version\": " //$NON-NLS-1$ //$NON-NLS-2$
				+ version + "}"; //$NON-NLS-1$

		return executePut(url, json);
	}

	private static String escapeJson(String text) {
		return text.replace("\\", "\\\\") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\"", "\\\"") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\n", "\\n") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\r", "\\r") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\t", "\\t"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String executeGet(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		try {
			// Set request method and headers
			connection.setRequestMethod("GET"); //$NON-NLS-1$
			connection.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$

			// Add Bearer token authentication
			String auth = "Bearer " + token; //$NON-NLS-1$
			connection.setRequestProperty("Authorization", auth); //$NON-NLS-1$

			// Set timeouts
			connection.setConnectTimeout(DEFAULT_TIMEOUT);
			connection.setReadTimeout(DEFAULT_TIMEOUT);

			// Check response code
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				return readResponse(connection.getInputStream());
			} else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new IOException(
						"Authentication failed. Check your access token."); //$NON-NLS-1$
			} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new IOException(
						"Resource not found. Check project key and repository slug."); //$NON-NLS-1$
			} else {
				String errorMessage = readResponse(connection.getErrorStream());
				throw new IOException("Request failed with status " //$NON-NLS-1$
						+ responseCode + ": " + errorMessage); //$NON-NLS-1$
			}
		} finally {
			connection.disconnect();
		}
	}

	private String executePost(String urlString, String jsonBody)
			throws IOException {
		return executeWriteRequest(urlString, jsonBody, "POST"); //$NON-NLS-1$
	}

	private String executePut(String urlString, String jsonBody)
			throws IOException {
		return executeWriteRequest(urlString, jsonBody, "PUT"); //$NON-NLS-1$
	}

	private String executeWriteRequest(String urlString, String jsonBody,
			String method) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		try {
			connection.setRequestMethod(method);
			connection.setRequestProperty("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
			connection.setRequestProperty("Content-Type", //$NON-NLS-1$
					"application/json"); //$NON-NLS-1$

			String auth = "Bearer " + token; //$NON-NLS-1$
			connection.setRequestProperty("Authorization", auth); //$NON-NLS-1$

			connection.setConnectTimeout(DEFAULT_TIMEOUT);
			connection.setReadTimeout(DEFAULT_TIMEOUT);
			connection.setDoOutput(true);

			byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
			try (OutputStream os = connection.getOutputStream()) {
				os.write(bodyBytes);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK
					|| responseCode == HttpURLConnection.HTTP_CREATED) {
				return readResponse(connection.getInputStream());
			} else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new IOException(
						"Authentication failed. Check your access token."); //$NON-NLS-1$
			} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new IOException(
						"Resource not found. Check project key and repository slug."); //$NON-NLS-1$
			} else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
				String errorMessage = readResponse(connection.getErrorStream());
				throw new IOException(
						"Conflict (version mismatch). " + errorMessage); //$NON-NLS-1$
			} else {
				String errorMessage = readResponse(connection.getErrorStream());
				throw new IOException("Request failed with status " //$NON-NLS-1$
						+ responseCode + ": " + errorMessage); //$NON-NLS-1$
			}
		} finally {
			connection.disconnect();
		}
	}

	private String readResponse(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return ""; //$NON-NLS-1$
		}

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			return response.toString();
		}
	}

	private byte[] executeGetBinary(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		try {
			// Set request method and headers
			connection.setRequestMethod("GET"); //$NON-NLS-1$
			connection.setRequestProperty("Accept", "*/*"); //$NON-NLS-1$ //$NON-NLS-2$

			// Add Bearer token authentication
			String auth = "Bearer " + token; //$NON-NLS-1$
			connection.setRequestProperty("Authorization", auth); //$NON-NLS-1$

			// Set timeouts
			connection.setConnectTimeout(DEFAULT_TIMEOUT);
			connection.setReadTimeout(DEFAULT_TIMEOUT);

			// Check response code
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				return readBinaryResponse(connection.getInputStream());
			} else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
				// Return empty byte array for non-existent files (added/deleted)
				return new byte[0];
			} else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new IOException(
						"Authentication failed. Check your access token."); //$NON-NLS-1$
			} else {
				String errorMessage = readResponse(connection.getErrorStream());
				throw new IOException("Request failed with status " //$NON-NLS-1$
						+ responseCode + ": " + errorMessage); //$NON-NLS-1$
			}
		} finally {
			connection.disconnect();
		}
	}

	private byte[] readBinaryResponse(InputStream inputStream)
			throws IOException {
		if (inputStream == null) {
			return new byte[0];
		}

		try (InputStream in = inputStream) {
			return in.readAllBytes();
		}
	}
}
