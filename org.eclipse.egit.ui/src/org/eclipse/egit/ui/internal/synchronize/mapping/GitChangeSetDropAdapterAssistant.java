/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.mapping;

import static org.eclipse.egit.ui.internal.CommonUtils.runCommand;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.ADD_TO_INDEX;
import static org.eclipse.egit.ui.internal.actions.ActionCommands.REMOVE_FROM_INDEX;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCache;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCacheFile;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCacheTree;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingFile;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelWorkingTree;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

/**
 * Drop Adapter Assistant for the Git Change Set model
 */
public class GitChangeSetDropAdapterAssistant extends
		CommonDropAdapterAssistant {

	private static final URLTransfer SELECTION_TRANSFER = URLTransfer
			.getInstance();

	/**
	 * Stage operation type
	 */
	private static final String STAGE_OP = "STAGE"; //$NON-NLS-1$

	/**
	 * Unstage operation type
	 */
	private static final String UNSTAGE_OP = "UNSTAGE"; //$NON-NLS-1$

	/**
	 * Unsupported operation type
	 */
	private static final String UNSUPPORTED_OP = "UNSUPPORTED"; //$NON-NLS-1$

	@Override
	public IStatus validateDrop(Object target, int operationCode,
			TransferData transferType) {
		TreeSelection selection = (TreeSelection) LocalSelectionTransfer
				.getTransfer().getSelection();

		String operation = getOperationType(selection);

		if (!UNSUPPORTED_OP.equals(operation)) {
			if (target instanceof GitModelWorkingTree) {
				if (UNSTAGE_OP.equals(operation))
					return Status.OK_STATUS;
			} else if (STAGE_OP.equals(operation)
					&& target instanceof GitModelCache)
				return Status.OK_STATUS;
		}

		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter aDropAdapter,
			DropTargetEvent aDropTargetEvent, Object aTarget) {
		TreeSelection selection = (TreeSelection) LocalSelectionTransfer
				.getTransfer().getSelection();
		String operation = getOperationType(selection);

		if (STAGE_OP.equals(operation))
			runCommand(ADD_TO_INDEX, selection);
		else if (UNSTAGE_OP.equals(operation))
			runCommand(REMOVE_FROM_INDEX, selection);

		return Status.OK_STATUS;
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return SELECTION_TRANSFER.isSupportedType(aTransferType);
	}

	private String getOperationType(TreeSelection selection) {
		String operation = null;

		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			String tmpOperation = null;
			Object next = i.next();
			if (next instanceof GitModelWorkingFile)
				tmpOperation = STAGE_OP;
			else if (next instanceof GitModelCacheFile)
				tmpOperation = UNSTAGE_OP;
			else if (next instanceof GitModelCacheTree) {
				if (((GitModelCacheTree) next).isWorkingTree())
					tmpOperation = STAGE_OP;
				else
					tmpOperation = UNSTAGE_OP;
			} else {
				operation = UNSUPPORTED_OP;
				break;
			}

			if (operation == null)
				operation = tmpOperation;
			else if (!operation.equals(tmpOperation)) {
				operation = UNSUPPORTED_OP;
				break;
			}
		}

		return operation;
	}

}
