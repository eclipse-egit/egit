/******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Shell;

/**
 * Annotation information control creator
 */
public class BlameInformationControlCreator extends
		AbstractReusableInformationControlCreator {

	private IVerticalRulerInfo rulerInfo;

	/**
	 * Create annotation information control creator
	 *
	 * @param rulerInfo
	 */
	public BlameInformationControlCreator(IVerticalRulerInfo rulerInfo) {
		this.rulerInfo = rulerInfo;
	}

	@Override
	protected IInformationControl doCreateInformationControl(Shell parent) {
		EnrichedCreator enrichedCreator = new EnrichedCreator();
		BlameInformationControl control = new BlameInformationControl(parent,
				enrichedCreator, rulerInfo);
		enrichedCreator.hoverInformationControl = control;
		return control;
	}

	// The enriched control needs access to the original hover control. We can
	// do that by using this separate creator.
	private static class EnrichedCreator extends
			AbstractReusableInformationControlCreator {

		private BlameInformationControl hoverInformationControl;

		@Override
		protected IInformationControl doCreateInformationControl(Shell parent) {
			return new BlameInformationControl(parent, hoverInformationControl);
		}
	}
}
