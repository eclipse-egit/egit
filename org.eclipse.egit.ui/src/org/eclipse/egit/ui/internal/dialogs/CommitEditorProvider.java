/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *    Robert Pofuk <rpofuk@gmail.com> Bug 486268
 *
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.ICommitEditorProvider;
import org.eclipse.egit.ui.ICommitMessageEditor;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitProposalProcessor;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.swt.widgets.Composite;

/**
 * Provider for commit editor. Check extension points and returns
 * implementation.
 */
public class CommitEditorProvider {

	private static final String COMMIT_MESSAGE_PROVIDER_ID = "org.eclipse.egit.ui.commitMessageEditor"; //$NON-NLS-1$

	/**
	 * @param parent
	 * @param initialText
	 * @param styles
	 * @param commitProposalProcessor
	 * @return Commit message editor. If no extension specified, returns
	 *         CommitMessageArea
	 */
	public static ICommitMessageEditor getCommitMessageProvider(
			Composite parent, @NonNull String initialText, int styles,
			CommitProposalProcessor commitProposalProcessor) {

		try {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] config = registry
					.getConfigurationElementsFor(COMMIT_MESSAGE_PROVIDER_ID);
			if (config.length > 0) {
				Object provider;
				provider = config[0].createExecutableExtension("class");//$NON-NLS-1$
				if (provider instanceof ICommitEditorProvider) {
					ICommitEditorProvider editorProvider = (ICommitEditorProvider) provider;
					if (editorProvider.isEnabled()) {
						return editorProvider.getEditor(parent, initialText,
								styles, commitProposalProcessor);
					}

				} else {
					Activator.logError(
							UIText.CommitDialog_WrongTypeOfCommitMessageProvider,
							null);
				}
			}
		} catch (CoreException e) {
			Activator.logError(
					UIText.CommitDialog_WrongTypeOfCommitMessageProvider, e);

		}

		return new DefaultCommitEditorProvider().getEditor(parent, initialText,
				styles, commitProposalProcessor);
	}
}
