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
package org.eclipse.egit.ui.internal.pullrequest;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.internal.bitbucket.ChangedFile;
import org.eclipse.egit.core.internal.bitbucket.PullRequest;
import org.eclipse.egit.core.internal.bitbucket.PullRequestComment;

/**
 * Utility class for parsing JSON responses from Bitbucket Data Center REST API
 */
public class PullRequestJsonParser {

	private PullRequestJsonParser() {
		// Utility class, no instances
	}

	/**
	 * Parse pull requests from JSON response
	 *
	 * @param json
	 *            the JSON response from /pull-requests endpoint
	 * @return list of pull requests
	 */
	public static List<PullRequest> parsePullRequests(String json) {
		List<PullRequest> result = new ArrayList<>();

		// Find the "values" array in the response
		int valuesStart = json.indexOf("\"values\":"); //$NON-NLS-1$
		if (valuesStart == -1) {
			return result;
		}

		// Find the opening bracket of the values array
		int arrayStart = json.indexOf('[', valuesStart);
		if (arrayStart == -1) {
			return result;
		}

		// Parse each pull request object in the array
		int pos = arrayStart + 1;
		while (pos < json.length()) {
			// Skip whitespace
			while (pos < json.length()
					&& Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}

			if (pos >= json.length() || json.charAt(pos) == ']') {
				break;
			}

			// Find the opening brace of the PR object
			if (json.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(json, pos);
				if (objEnd == -1) {
					break;
				}

				String prJson = json.substring(pos, objEnd + 1);
				PullRequest pr = parseSinglePullRequest(prJson);
				if (pr != null) {
					result.add(pr);
				}

				pos = objEnd + 1;
				// Skip comma if present
				while (pos < json.length() && (Character.isWhitespace(
						json.charAt(pos)) || json.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	/**
	 * Parse a single pull request from JSON
	 *
	 * @param json
	 *            the JSON object
	 * @return pull request or null
	 */
	public static PullRequest parseSinglePullRequest(String json) {
		PullRequest pr = new PullRequest();

		// Parse id
		Long id = extractLongValue(json, "\"id\":"); //$NON-NLS-1$
		if (id != null) {
			pr.setId(id.longValue());
		}

		// Parse version
		Integer version = extractIntValue(json, "\"version\":"); //$NON-NLS-1$
		if (version != null) {
			pr.setVersion(version.intValue());
		}

		// Parse title
		String title = extractStringValue(json, "\"title\":"); //$NON-NLS-1$
		if (title != null) {
			pr.setTitle(title);
		}

		// Parse description
		String description = extractStringValue(json, "\"description\":"); //$NON-NLS-1$
		if (description != null) {
			pr.setDescription(description);
		}

		// Parse state
		String state = extractStringValue(json, "\"state\":"); //$NON-NLS-1$
		if (state != null) {
			pr.setState(state);
		}

		// Parse open
		Boolean open = extractBooleanValue(json, "\"open\":"); //$NON-NLS-1$
		if (open != null) {
			pr.setOpen(open.booleanValue());
		}

		// Parse closed
		Boolean closed = extractBooleanValue(json, "\"closed\":"); //$NON-NLS-1$
		if (closed != null) {
			pr.setClosed(closed.booleanValue());
		}

		// Parse createdDate
		Long createdDate = extractLongValue(json, "\"createdDate\":"); //$NON-NLS-1$
		if (createdDate != null) {
			pr.setCreatedDate(new java.util.Date(createdDate.longValue()));
		}

		// Parse updatedDate
		Long updatedDate = extractLongValue(json, "\"updatedDate\":"); //$NON-NLS-1$
		if (updatedDate != null) {
			pr.setUpdatedDate(new java.util.Date(updatedDate.longValue()));
		}

		// Parse fromRef
		String fromRefJson = extractObjectValue(json, "\"fromRef\":"); //$NON-NLS-1$
		if (fromRefJson != null) {
			pr.setFromRef(parseRef(fromRefJson));
		}

		// Parse toRef
		String toRefJson = extractObjectValue(json, "\"toRef\":"); //$NON-NLS-1$
		if (toRefJson != null) {
			pr.setToRef(parseRef(toRefJson));
		}

		// Parse author
		String authorJson = extractObjectValue(json, "\"author\":"); //$NON-NLS-1$
		if (authorJson != null) {
			pr.setAuthor(parseParticipant(authorJson));
		}

		// Parse comment count from properties
		String propertiesJson = extractObjectValue(json, "\"properties\":"); //$NON-NLS-1$
		if (propertiesJson != null) {
			Integer commentCount = extractIntValue(propertiesJson,
					"\"commentCount\":"); //$NON-NLS-1$
			if (commentCount != null) {
				pr.setCommentCount(commentCount.intValue());
			}
		}

		return pr;
	}

	/**
	 * Parse changed files from JSON response
	 *
	 * @param json
	 *            the JSON response from /changes endpoint
	 * @return list of changed files
	 */
	public static List<ChangedFile> parseChangedFiles(String json) {
		List<ChangedFile> result = new ArrayList<>();

		// Find the "values" array in the response
		int valuesStart = json.indexOf("\"values\":"); //$NON-NLS-1$
		if (valuesStart == -1) {
			return result;
		}

		// Find the opening bracket of the values array
		int arrayStart = json.indexOf('[', valuesStart);
		if (arrayStart == -1) {
			return result;
		}

		// Parse each changed file object in the array
		int pos = arrayStart + 1;
		while (pos < json.length()) {
			// Skip whitespace
			while (pos < json.length()
					&& Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}

			if (pos >= json.length() || json.charAt(pos) == ']') {
				break;
			}

			// Find the opening brace of the changed file object
			if (json.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(json, pos);
				if (objEnd == -1) {
					break;
				}

				String changedFileJson = json.substring(pos, objEnd + 1);
				ChangedFile cf = parseSingleChangedFile(changedFileJson);
				if (cf != null) {
					result.add(cf);
				}

				pos = objEnd + 1;
				// Skip comma if present
				while (pos < json.length() && (Character.isWhitespace(
						json.charAt(pos)) || json.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	/**
	 * Parse a single changed file from JSON
	 *
	 * @param json
	 *            the JSON object
	 * @return changed file or null
	 */
	public static ChangedFile parseSingleChangedFile(String json) {
		ChangedFile cf = new ChangedFile();

		// Parse type (ADD, MODIFY, DELETE, MOVE, COPY)
		String type = extractStringValue(json, "\"type\":"); //$NON-NLS-1$
		if (type != null) {
			cf.setType(type);
		}

		// Parse path
		String pathJson = extractObjectValue(json, "\"path\":"); //$NON-NLS-1$
		if (pathJson != null) {
			cf.setPath(parsePath(pathJson));
		}

		// Parse srcPath (for MOVE/COPY operations)
		String srcPathJson = extractObjectValue(json, "\"srcPath\":"); //$NON-NLS-1$
		if (srcPathJson != null) {
			cf.setSrcPath(parsePath(srcPathJson));
		}

		return cf;
	}

	/**
	 * Parse pull request activities from JSON response and extract comments
	 *
	 * @param json
	 *            the JSON response from /activities endpoint
	 * @return list of PullRequestComment objects
	 */
	public static List<PullRequestComment> parseActivities(String json) {
		List<PullRequestComment> result = new ArrayList<>();

		// Find the "values" array
		int valuesStart = json.indexOf("\"values\":"); //$NON-NLS-1$
		if (valuesStart == -1) {
			return result;
		}

		int arrayStart = json.indexOf('[', valuesStart);
		if (arrayStart == -1) {
			return result;
		}

		// Parse each activity object
		int pos = arrayStart + 1;
		while (pos < json.length()) {
			while (pos < json.length()
					&& Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}

			if (pos >= json.length() || json.charAt(pos) == ']') {
				break;
			}

			if (json.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(json, pos);
				if (objEnd == -1) {
					break;
				}

				String activityJson = json.substring(pos, objEnd + 1);

				// Check if this is a COMMENTED action
				String action = extractStringValue(activityJson, "\"action\":"); //$NON-NLS-1$
				if ("COMMENTED".equals(action)) { //$NON-NLS-1$
					PullRequestComment comment = parseCommentActivity(
							activityJson);
					if (comment != null) {
						result.add(comment);
					}
				}

				pos = objEnd + 1;
				while (pos < json.length() && (Character.isWhitespace(
						json.charAt(pos)) || json.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	/**
	 * Parse a single comment activity
	 *
	 * @param activityJson
	 *            the activity JSON object
	 * @return PullRequestComment or null
	 */
	private static PullRequestComment parseCommentActivity(
			String activityJson) {
		// Extract the "comment" object
		String commentJson = extractObjectValue(activityJson, "\"comment\":"); //$NON-NLS-1$
		if (commentJson == null) {
			return null;
		}

		PullRequestComment comment = parseComment(commentJson);
		if (comment == null) {
			return null;
		}

		// Extract the "commentAnchor" object if present
		String anchorJson = extractObjectValue(activityJson,
				"\"commentAnchor\":"); //$NON-NLS-1$
		if (anchorJson != null) {
			parseCommentAnchor(comment, anchorJson);
		}

		return comment;
	}

	/**
	 * Parse a comment object
	 *
	 * @param commentJson
	 *            the comment JSON object
	 * @return PullRequestComment or null
	 */
	private static PullRequestComment parseComment(String commentJson) {
		PullRequestComment comment = new PullRequestComment();

		// Parse id
		Long id = extractLongValue(commentJson, "\"id\":"); //$NON-NLS-1$
		if (id != null) {
			comment.setId(id.longValue());
		}

		// Parse version
		Integer version = extractIntValue(commentJson, "\"version\":"); //$NON-NLS-1$
		if (version != null) {
			comment.setVersion(version.intValue());
		}

		// Parse text
		String text = extractStringValue(commentJson, "\"text\":"); //$NON-NLS-1$
		if (text != null) {
			comment.setText(text);
		}

		// Parse author
		String authorJson = extractObjectValue(commentJson, "\"author\":"); //$NON-NLS-1$
		if (authorJson != null) {
			String name = extractStringValue(authorJson, "\"name\":"); //$NON-NLS-1$
			String displayName = extractStringValue(authorJson,
					"\"displayName\":"); //$NON-NLS-1$
			String email = extractStringValue(authorJson,
					"\"emailAddress\":"); //$NON-NLS-1$
			comment.setAuthorName(name);
			comment.setAuthorDisplayName(displayName);
			comment.setAuthorEmail(email);
		}

		// Parse createdDate
		Long createdDate = extractLongValue(commentJson, "\"createdDate\":"); //$NON-NLS-1$
		if (createdDate != null) {
			comment.setCreatedDate(new java.util.Date(createdDate.longValue()));
		}

		// Parse updatedDate
		Long updatedDate = extractLongValue(commentJson, "\"updatedDate\":"); //$NON-NLS-1$
		if (updatedDate != null) {
			comment.setUpdatedDate(new java.util.Date(updatedDate.longValue()));
		}

		// Parse state
		String state = extractStringValue(commentJson, "\"state\":"); //$NON-NLS-1$
		if (state != null) {
			comment.setState(state);
		}

		// Parse severity
		String severity = extractStringValue(commentJson, "\"severity\":"); //$NON-NLS-1$
		if (severity != null) {
			comment.setSeverity(severity);
		}

		// Parse replies (nested comments)
		String repliesJson = extractObjectValue(commentJson, "\"comments\":"); //$NON-NLS-1$
		if (repliesJson != null && repliesJson.startsWith("[")) { //$NON-NLS-1$
			List<PullRequestComment> replies = parseCommentArray(repliesJson);
			comment.setReplies(replies);
		}

		return comment;
	}

	/**
	 * Parse a comment anchor into the comment object
	 *
	 * @param comment
	 *            the comment to update
	 * @param anchorJson
	 *            the anchor JSON object
	 */
	private static void parseCommentAnchor(PullRequestComment comment,
			String anchorJson) {
		// Parse line
		Integer line = extractIntValue(anchorJson, "\"line\":"); //$NON-NLS-1$
		comment.setLine(line);

		// Parse lineType
		String lineType = extractStringValue(anchorJson, "\"lineType\":"); //$NON-NLS-1$
		comment.setLineType(lineType);

		// Parse fileType
		String fileType = extractStringValue(anchorJson, "\"fileType\":"); //$NON-NLS-1$
		comment.setFileType(fileType);

		// Parse path
		String path = extractStringValue(anchorJson, "\"path\":"); //$NON-NLS-1$
		comment.setPath(path);

		// Parse srcPath
		String srcPath = extractStringValue(anchorJson, "\"srcPath\":"); //$NON-NLS-1$
		comment.setSrcPath(srcPath);
	}

	/**
	 * Parse an array of comments
	 *
	 * @param arrayJson
	 *            the JSON array string
	 * @return list of comments
	 */
	private static List<PullRequestComment> parseCommentArray(
			String arrayJson) {
		List<PullRequestComment> result = new ArrayList<>();

		if (!arrayJson.startsWith("[")) { //$NON-NLS-1$
			return result;
		}

		int pos = 1; // Skip opening bracket
		while (pos < arrayJson.length()) {
			while (pos < arrayJson.length()
					&& Character.isWhitespace(arrayJson.charAt(pos))) {
				pos++;
			}

			if (pos >= arrayJson.length() || arrayJson.charAt(pos) == ']') {
				break;
			}

			if (arrayJson.charAt(pos) == '{') {
				int objEnd = findMatchingBrace(arrayJson, pos);
				if (objEnd == -1) {
					break;
				}

				String commentJson = arrayJson.substring(pos, objEnd + 1);
				PullRequestComment comment = parseComment(commentJson);
				if (comment != null) {
					result.add(comment);
				}

				pos = objEnd + 1;
				while (pos < arrayJson.length() && (Character.isWhitespace(
						arrayJson.charAt(pos)) || arrayJson.charAt(pos) == ',')) {
					pos++;
				}
			} else {
				pos++;
			}
		}

		return result;
	}

	// Helper parsing methods

	private static PullRequest.PullRequestRef parseRef(String json) {
		PullRequest.PullRequestRef ref = new PullRequest.PullRequestRef();

		String id = extractStringValue(json, "\"id\":"); //$NON-NLS-1$
		if (id != null) {
			ref.setId(id);
		}

		String displayId = extractStringValue(json, "\"displayId\":"); //$NON-NLS-1$
		if (displayId != null) {
			ref.setDisplayId(displayId);
		}

		String repoJson = extractObjectValue(json, "\"repository\":"); //$NON-NLS-1$
		if (repoJson != null) {
			ref.setRepository(parseRepository(repoJson));
		}

		return ref;
	}

	private static PullRequest.Repository parseRepository(String json) {
		PullRequest.Repository repo = new PullRequest.Repository();

		String slug = extractStringValue(json, "\"slug\":"); //$NON-NLS-1$
		if (slug != null) {
			repo.setSlug(slug);
		}

		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			repo.setName(name);
		}

		String projectJson = extractObjectValue(json, "\"project\":"); //$NON-NLS-1$
		if (projectJson != null) {
			repo.setProject(parseProject(projectJson));
		}

		return repo;
	}

	private static PullRequest.Project parseProject(String json) {
		PullRequest.Project project = new PullRequest.Project();

		String key = extractStringValue(json, "\"key\":"); //$NON-NLS-1$
		if (key != null) {
			project.setKey(key);
		}

		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			project.setName(name);
		}

		return project;
	}

	private static PullRequest.PullRequestParticipant parseParticipant(
			String json) {
		PullRequest.PullRequestParticipant participant = new PullRequest.PullRequestParticipant();

		String userJson = extractObjectValue(json, "\"user\":"); //$NON-NLS-1$
		if (userJson != null) {
			participant.setUser(parseUser(userJson));
		}

		String role = extractStringValue(json, "\"role\":"); //$NON-NLS-1$
		if (role != null) {
			participant.setRole(role);
		}

		Boolean approved = extractBooleanValue(json, "\"approved\":"); //$NON-NLS-1$
		if (approved != null) {
			participant.setApproved(approved.booleanValue());
		}

		return participant;
	}

	private static PullRequest.User parseUser(String json) {
		PullRequest.User user = new PullRequest.User();

		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			user.setName(name);
		}

		String emailAddress = extractStringValue(json, "\"emailAddress\":"); //$NON-NLS-1$
		if (emailAddress != null) {
			user.setEmailAddress(emailAddress);
		}

		String displayName = extractStringValue(json, "\"displayName\":"); //$NON-NLS-1$
		if (displayName != null) {
			user.setDisplayName(displayName);
		}

		return user;
	}

	private static ChangedFile.Path parsePath(String json) {
		ChangedFile.Path path = new ChangedFile.Path();

		// Parse toString
		String toStringValue = extractStringValue(json, "\"toString\":"); //$NON-NLS-1$
		if (toStringValue != null) {
			path.setToString(toStringValue);
		}

		// Parse name
		String name = extractStringValue(json, "\"name\":"); //$NON-NLS-1$
		if (name != null) {
			path.setName(name);
		}

		// Parse extension
		String extension = extractStringValue(json, "\"extension\":"); //$NON-NLS-1$
		if (extension != null) {
			path.setExtension(extension);
		}

		// Parse components array
		List<String> components = new ArrayList<>();
		String componentsJson = json.substring(json.indexOf("\"components\":")); //$NON-NLS-1$
		int arrayStart = componentsJson.indexOf('[');
		if (arrayStart != -1) {
			int arrayEnd = componentsJson.indexOf(']', arrayStart);
			if (arrayEnd != -1) {
				String arrayContent = componentsJson.substring(arrayStart + 1,
						arrayEnd);
				// Simple string array parser
				int compPos = 0;
				while (compPos < arrayContent.length()) {
					int quoteStart = arrayContent.indexOf('"', compPos);
					if (quoteStart == -1) {
						break;
					}
					int quoteEnd = arrayContent.indexOf('"', quoteStart + 1);
					if (quoteEnd == -1) {
						break;
					}
					components.add(
							arrayContent.substring(quoteStart + 1, quoteEnd));
					compPos = quoteEnd + 1;
				}
			}
		}
		path.setComponents(components);

		return path;
	}

	// Low-level extraction utilities

	private static String extractStringValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int valueStart = json.indexOf('"', keyPos + key.length());
		if (valueStart == -1) {
			return null;
		}

		int valueEnd = valueStart + 1;
		boolean escape = false;
		while (valueEnd < json.length()) {
			char c = json.charAt(valueEnd);
			if (escape) {
				escape = false;
				valueEnd++;
				continue;
			}
			if (c == '\\') {
				escape = true;
				valueEnd++;
				continue;
			}
			if (c == '"') {
				return json.substring(valueStart + 1, valueEnd);
			}
			valueEnd++;
		}

		return null;
	}

	private static Long extractLongValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int valueStart = keyPos + key.length();
		while (valueStart < json.length()
				&& Character.isWhitespace(json.charAt(valueStart))) {
			valueStart++;
		}

		int valueEnd = valueStart;
		while (valueEnd < json.length() && (Character.isDigit(
				json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) {
			valueEnd++;
		}

		if (valueEnd > valueStart) {
			try {
				return Long.valueOf(json.substring(valueStart, valueEnd));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		return null;
	}

	private static Integer extractIntValue(String json, String key) {
		Long value = extractLongValue(json, key);
		return value != null ? Integer.valueOf(value.intValue()) : null;
	}

	private static Boolean extractBooleanValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int valueStart = keyPos + key.length();
		while (valueStart < json.length()
				&& Character.isWhitespace(json.charAt(valueStart))) {
			valueStart++;
		}

		if (json.startsWith("true", valueStart)) { //$NON-NLS-1$
			return Boolean.TRUE;
		} else if (json.startsWith("false", valueStart)) { //$NON-NLS-1$
			return Boolean.FALSE;
		}

		return null;
	}

	private static String extractObjectValue(String json, String key) {
		int keyPos = json.indexOf(key);
		if (keyPos == -1) {
			return null;
		}

		int objectStart = json.indexOf('{', keyPos + key.length());
		if (objectStart == -1) {
			return null;
		}

		int objectEnd = findMatchingBrace(json, objectStart);
		if (objectEnd == -1) {
			return null;
		}

		return json.substring(objectStart, objectEnd + 1);
	}

	private static int findMatchingBrace(String json, int startPos) {
		int depth = 0;
		boolean inString = false;
		boolean escape = false;

		for (int i = startPos; i < json.length(); i++) {
			char c = json.charAt(i);

			if (escape) {
				escape = false;
				continue;
			}

			if (c == '\\') {
				escape = true;
				continue;
			}

			if (c == '"') {
				inString = !inString;
				continue;
			}

			if (!inString) {
				if (c == '{') {
					depth++;
				} else if (c == '}') {
					depth--;
					if (depth == 0) {
						return i;
					}
				}
			}
		}

		return -1;
	}
}
