/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

/**
 * Drop Adapter Assistant for the Git Change Set model
 */
public class GitChangeSetDropAdapterAssistant extends CommonDropAdapterAssistant {

	/**
	 *
	 */
	public GitChangeSetDropAdapterAssistant() {
		// TODO Auto-generated constructor stub
		System.out.println("asdf"); //$NON-NLS-1$
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
			TransferData transferType) {
		if (target instanceof GitModelCache)
			return Status.OK_STATUS;

		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter aDropAdapter,
			DropTargetEvent aDropTargetEvent, Object aTarget) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return true;
	}

}
