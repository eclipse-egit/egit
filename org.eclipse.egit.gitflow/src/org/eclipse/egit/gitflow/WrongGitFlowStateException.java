/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow;

/**
 * Signifies that a git flow operation was performed on a git repository state,
 * this operation is not intended for.
 */
public class WrongGitFlowStateException extends Exception {

	/**
	 * @generated
	 */
	private static final long serialVersionUID = 3091117695421525438L;

	/**
	 * @param e
	 */
	public WrongGitFlowStateException(Exception e) {
		super(e);
	}

	/**
	 * @param string
	 */
	public WrongGitFlowStateException(String string) {
		super(string);
	}
}
