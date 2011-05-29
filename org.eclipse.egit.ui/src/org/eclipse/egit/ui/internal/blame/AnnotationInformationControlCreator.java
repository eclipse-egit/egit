/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.swt.widgets.Shell;

/**
 * Annotation information control creator
 */
public class AnnotationInformationControlCreator extends
		AbstractReusableInformationControlCreator {

	private boolean resizable;

	/**
	 * Create annotation information control creator
	 *
	 * @param resizable
	 */
	public AnnotationInformationControlCreator(boolean resizable) {
		this.resizable = resizable;
	}

	protected IInformationControl doCreateInformationControl(Shell parent) {
		return new AnnotationInformationControl(parent, resizable,
				new AnnotationInformationControlCreator(true));
	}

}
