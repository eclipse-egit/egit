/*******************************************************************************
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.decorators;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

/**
 * Interface which decoratable element classes must implement to be decorated by
 * the {@link ProblemLabelDecorator}.
 */
public interface IProblemDecoratable {

	/**
	 * Indication for no problem.
	 */
	public static int SEVERITY_NONE = -1;

	/**
	 * Should return the problem severity of the decoratable element, e.g.
	 * {@link IMarker#SEVERITY_ERROR}. Return
	 * {@link IProblemDecoratable#SEVERITY_NONE} for no problem.
	 * <p>
	 * Implementation can use
	 * {@link IResource#findMaxProblemSeverity(String, boolean, int)} for
	 * resources.
	 *
	 * @return problem severity
	 */
	int getProblemSeverity();
}
