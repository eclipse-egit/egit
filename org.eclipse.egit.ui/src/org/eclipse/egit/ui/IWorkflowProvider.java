/*******************************************************************************
 * Copyright (C) 2010, Thorsten Kamann <thorsten@kamann.info>
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;

/**
 * This interface must be implemented to be a workflow provider. A workflow
 * provider provides informations related to a user workflow.
 * This message will be added to the text field in the {@link CommitDialog}. <br/>
 *
 */
public interface IWorkflowProvider {

	/**
	 * ID of the extension point for the WorkflowProvider
	 */
	public static final String WORKFLOW_PROVIDER_ID = "org.eclipse.egit.ui.workflowProvider"; //$NON-NLS-1$

	/**
	 * @param resources
	 * @return the message the {@link CommitDialog} should use as default message or
	 * <code>null</code> if this provider cannot provide a commit message
	 */
	public String getCommitMessage(IResource[] resources);

	/**
	 * @return a branch name suggestion for the Creation wizard
	 */
	public String getBranchNameSuggestion();

	/**
	 * @return default source reference from the Preferences
	 */
	public String getDefaultSourceReference();
}
