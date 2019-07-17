/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;

/**
 * Results for the find toolbar. This object stores the rows in the history
 * table that contain a match to a given pattern.
 *
 * @see FindToolbar
 * @see FindToolbarJob
 */
public class FindResults {
	private Map<Integer, Integer> matchesMap = new LinkedHashMap<>();

	private List<RevObject> revObjList = new ArrayList<>();

	Integer[] keysArray;

	private int matchesCount;

	private RevFlag highlight;

	private boolean overflow;

	private final CopyOnWriteArrayList<IFindListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Adds the given listener to be notified when search results are added or
	 * the results are cleared. Has no effect if the listener is already
	 * registered.
	 *
	 * @param listener
	 *            to add
	 */
	public void addFindListener(IFindListener listener) {
		listeners.addIfAbsent(listener);
	}

	/**
	 * Removes the given listener if it was registered.
	 *
	 * @param listener
	 *            to remove
	 */
	public void removeFindListener(IFindListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Returns if the index in the history table matches the find pattern.
	 *
	 * @param index
	 *            history table item index.
	 * @return boolean <code>true</code> if the history table <code>index</code>
	 *         contains a match to the find pattern, <code>false</code>
	 *         otherwise
	 */
	public synchronized boolean isFoundAt(int index) {
		return matchesMap.containsKey(Integer.valueOf(index));
	}

	/**
	 * Returns the first table item index after <code>index</code> that
	 * contains a match to the find pattern.
	 *
	 * @param index
	 *            the history table item index
	 * @return the index after <code>index</code> that contains a match.
	 *         Returns -1 if there isn't a match after <code>index</code>
	 */
	public synchronized int getIndexAfter(int index) {
		Integer[] matches = getkeysArray();
		int sres = Arrays.binarySearch(matches, Integer.valueOf(index));
		if (sres >= 0 && sres != matches.length - 1) {
			return matches[sres + 1].intValue();
		} else if (sres < 0) {
			sres = -sres - 1;
			if (sres < matches.length) {
				return matches[sres].intValue();
			}
		}

		return -1;
	}

	/**
	 * Returns the first table item index before <code>index</code> that
	 * contains a match to the find pattern.
	 *
	 * @param index
	 *            the history table item index
	 * @return the index before <code>index</code> that contains a match.
	 *         Returns -1 if there isn't a match before <code>index</code>
	 */
	public synchronized int getIndexBefore(int index) {
		Integer[] matches = getkeysArray();
		int sres = Arrays.binarySearch(matches, Integer.valueOf(index));
		if (sres >= 0 && sres != 0) {
			return matches[sres - 1].intValue();
		} else if (sres < -1) {
			sres = -sres;
			return matches[sres - 2].intValue();
		}

		return -1;
	}

	/**
	 * Returns the first table item index that contains a match to the find
	 * pattern.
	 *
	 * @return the first index that contains a match. Returns -1 if there isn't
	 *         any match
	 */
	public synchronized int getFirstIndex() {
		Iterator iter = matchesMap.keySet().iterator();
		if (iter.hasNext()) {
			return ((Integer) iter.next()).intValue();
		}

		return -1;
	}

	/**
	 * Returns the last table item index that contains a match to the find
	 * pattern.
	 *
	 * @return the last index that contains a match. Returns -1 if there isn't
	 *         any match
	 */
	public synchronized int getLastIndex() {
		Integer[] matches = getkeysArray();
		if (matches.length > 0) {
			return matches[matches.length - 1].intValue();
		}

		return -1;
	}

	/**
	 * Returns the index in the matches list for the history table item
	 * <code>index</code>.
	 *
	 * @param index
	 *            the history table item index
	 * @return the position of the <code>index</code> in the total matches
	 *         list. Returns -1 if <code>index</code> doesn't contain a match
	 */
	public synchronized int getMatchNumberFor(int index) {
		Integer ix = matchesMap.get(Integer.valueOf(index));
		if (ix != null) {
			return ix.intValue();
		}

		return -1;
	}

	/**
	 * @return int
	 */
	public int size() {
		return matchesCount;
	}

	/**
	 * Cleans the find results. All match item indexes are removed.
	 */
	public synchronized void clear() {
		if (highlight != null) {
			for (RevObject o : revObjList) {
				o.remove(highlight);
			}
		}
		matchesMap.clear();
		revObjList.clear();
		keysArray = null;
		boolean hadItems = matchesCount > 0;
		matchesCount = 0;
		if (hadItems) {
			for (IFindListener listener : listeners) {
				listener.cleared();
			}
		}
	}

	/**
	 * Adds a history table item index (<code>matchIx</code>) to the find
	 * results matches list.
	 *
	 * @param matchIx
	 *            the history table item index that matches a find pattern.
	 * @param revObj
	 *            The RevObject that will have the highlight tag set.
	 */
	public synchronized void add(int matchIx, RevObject revObj) {
		matchesMap.put(Integer.valueOf(matchIx), Integer
				.valueOf(++matchesCount));
		revObjList.add(revObj);
		revObj.add(highlight);
		keysArray = null;
		for (IFindListener listener : listeners) {
			listener.itemAdded(matchIx, revObj);
		}
	}

	private Integer[] getkeysArray() {
		if (keysArray == null) {
			keysArray = matchesMap.keySet().toArray(
					new Integer[0]);
		}

		return keysArray;
	}

	synchronized void setHighlightFlag(RevFlag hFlag) {
		if (highlight != null) {
			clear();
		}
		this.highlight = hFlag;
	}

	synchronized void setOverflow() {
		overflow = true;
	}

	synchronized boolean isOverflow() {
		return overflow;
	}
}
