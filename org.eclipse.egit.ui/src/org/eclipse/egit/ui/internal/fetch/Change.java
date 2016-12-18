/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Jaxsun McCarthy Huggan <jaxsun.mccarthy@tasktop.com> - Bug 509181
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.osgi.util.NLS;

class Change implements Comparable<Change> {
	private final String refName;

	private final Integer changeNumber;

	private final Integer patchSetNumber;

	static Change fromRef(String refName) {
		try {
			if (!refName.startsWith("refs/changes/")) //$NON-NLS-1$
				return null;
			String[] tokens = refName.substring(13).split("/"); //$NON-NLS-1$
			if (tokens.length != 3)
				return null;
			Integer changeNumber = Integer.valueOf(tokens[1]);
			Integer patchSetNumber = Integer.valueOf(tokens[2]);
			return new Change(refName, changeNumber, patchSetNumber);
		} catch (NumberFormatException e) {
			// if we can't parse this, just return null
			return null;
		} catch (IndexOutOfBoundsException e) {
			// if we can't parse this, just return null
			return null;
		}
	}

	private Change(String refName, Integer changeNumber,
			Integer patchSetNumber) {
		this.refName = refName;
		this.changeNumber = changeNumber;
		this.patchSetNumber = patchSetNumber;
	}

	public String getRefName() {
		return refName;
	}

	public Integer getChangeNumber() {
		return changeNumber;
	}

	public Integer getPatchSetNumber() {
		return patchSetNumber;
	}

	public String suggestBranchName() {
		return NLS.bind(UIText.Change_SuggestedBranchNamePattern, changeNumber,
				patchSetNumber);
	}

	public String computeFullRefName() {
		return NLS.bind(UIText.Change_FullRefNamePattern, changeNumber,
				patchSetNumber);
	}

	@Override
	public String toString() {
		return refName;
	}

	@Override
	public int compareTo(Change other) {
		int changeDiff = this.changeNumber.compareTo(other.changeNumber);
		if (changeDiff == 0) {
			changeDiff = this.patchSetNumber.compareTo(other.patchSetNumber);
		}
		return changeDiff;
	}
}
