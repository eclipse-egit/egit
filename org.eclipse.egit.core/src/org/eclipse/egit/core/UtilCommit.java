package org.eclipse.egit.core;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A RevCommit with an extra field 'util' that can store extra information about this commit
 */
public class UtilCommit extends RevCommit {
	private Object util;

	/**
	 * Default constructor
	 * @param id
	 *            object name for the commit.
	 */
	public UtilCommit(AnyObjectId id) {
		super(id);
	}

	/**
	 * Getter for 'util' field.
	 * @return util field
	 */
	public Object getUtil() {
		return util;
	}

	/**
	 * Setter for 'util' field
	 * @param util The new contents for the util field
	 */
	public void setUtil(Object util) {
		this.util = util;
	}
}
