/*******************************************************************************
 * Copyright (C) 2011, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test;


public class CommitMessageUtil {

	/**
	 * @param commitMessage
	 *            commit message with platform independent line endings (\n)
	 * @return change id string
	 */
	public static String extractChangeId(String commitMessage) {
		int changeIdOffset = findOffsetOfChangeIdLine(commitMessage);
		if (changeIdOffset <= 0)
			return null;
		int endOfChangeId = findNextEOL(changeIdOffset, commitMessage);
		return commitMessage.substring(changeIdOffset, endOfChangeId);
	}

	private static int findNextEOL(int oldPos, String message) {
		return message.indexOf("\n", oldPos + 1);
	}

	private static int findOffsetOfChangeIdLine(String message) {
		return message.indexOf("\nChange-Id: I");
	}

}
