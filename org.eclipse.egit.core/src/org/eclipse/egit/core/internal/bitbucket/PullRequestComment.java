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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Model class for a pull request comment from Bitbucket Data Center
 */
public class PullRequestComment {

	private long id;

	private int version;

	private String text;

	private String authorName;

	private String authorDisplayName;

	private String authorEmail;

	private Date createdDate;

	private Date updatedDate;

	private String state;

	private String severity;

	// Comment anchor information (null for general PR comments)
	private String path;

	private String srcPath;

	private Integer line; // null for file-level comments

	private String lineType; // ADDED, REMOVED, CONTEXT

	private String fileType; // FROM, TO

	// Thread structure
	private List<PullRequestComment> replies = new ArrayList<>();

	/**
	 * @return the comment ID
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the comment ID
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the comment version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            the comment version
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the comment text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text
	 *            the comment text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the author username
	 */
	public String getAuthorName() {
		return authorName;
	}

	/**
	 * @param authorName
	 *            the author username
	 */
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	/**
	 * @return the author display name
	 */
	public String getAuthorDisplayName() {
		return authorDisplayName;
	}

	/**
	 * @param authorDisplayName
	 *            the author display name
	 */
	public void setAuthorDisplayName(String authorDisplayName) {
		this.authorDisplayName = authorDisplayName;
	}

	/**
	 * @return the author email
	 */
	public String getAuthorEmail() {
		return authorEmail;
	}

	/**
	 * @param authorEmail
	 *            the author email
	 */
	public void setAuthorEmail(String authorEmail) {
		this.authorEmail = authorEmail;
	}

	/**
	 * @return the creation date
	 */
	public Date getCreatedDate() {
		return createdDate;
	}

	/**
	 * @param createdDate
	 *            the creation date
	 */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * @return the last update date
	 */
	public Date getUpdatedDate() {
		return updatedDate;
	}

	/**
	 * @param updatedDate
	 *            the last update date
	 */
	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

	/**
	 * @return the comment state (OPEN, RESOLVED)
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state
	 *            the comment state
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the comment severity (NORMAL, BLOCKER)
	 */
	public String getSeverity() {
		return severity;
	}

	/**
	 * @param severity
	 *            the comment severity
	 */
	public void setSeverity(String severity) {
		this.severity = severity;
	}

	/**
	 * @return the file path (null for general PR comments)
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 *            the file path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the source path for renamed/moved files
	 */
	public String getSrcPath() {
		return srcPath;
	}

	/**
	 * @param srcPath
	 *            the source path
	 */
	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}

	/**
	 * @return the line number (null for file-level or general comments)
	 */
	public Integer getLine() {
		return line;
	}

	/**
	 * @param line
	 *            the line number
	 */
	public void setLine(Integer line) {
		this.line = line;
	}

	/**
	 * @return the line type (ADDED, REMOVED, CONTEXT)
	 */
	public String getLineType() {
		return lineType;
	}

	/**
	 * @param lineType
	 *            the line type
	 */
	public void setLineType(String lineType) {
		this.lineType = lineType;
	}

	/**
	 * @return the file type/side (FROM for left/old, TO for right/new)
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * @param fileType
	 *            the file type/side
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * @return the list of replies to this comment
	 */
	public List<PullRequestComment> getReplies() {
		return replies;
	}

	/**
	 * @param replies
	 *            the list of replies
	 */
	public void setReplies(List<PullRequestComment> replies) {
		this.replies = replies;
	}

	/**
	 * @return true if this is a general PR comment (no file/line anchor)
	 */
	public boolean isGeneralComment() {
		return path == null;
	}

	/**
	 * @return true if this is a file-level comment (has path but no line)
	 */
	public boolean isFileLevelComment() {
		return path != null && line == null;
	}

	/**
	 * @return true if this is an inline comment (has path and line)
	 */
	public boolean isInlineComment() {
		return path != null && line != null;
	}
}
