/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.BranchConfigurationDialog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * Properties for repository and user configuration (read-only).
 * <p>
 * Depending on which mode is selected, either the user configuration, the
 * repository configuration, or the effective configuration is shown.
 */
public class BranchPropertySource implements IPropertySource {
	static final String EDITACTIONID = "EditBranch"; //$NON-NLS-1$

	private static class EditAction extends Action {

		private BranchPropertySource source;

		public EditAction(String text, ImageDescriptor image,
				BranchPropertySource source) {
			super(text, image);
			this.source = source;
		}

		public EditAction setSource(BranchPropertySource source) {
			this.source = source;
			return this;
		}

		@Override
		public String getId() {
			return EDITACTIONID;
		}

		@Override
		public void run() {
			new BranchConfigurationDialog(source.myPage.getSite().getShell(),
					source.myBranchName, source.myRepository).open();
			source.myPage.refresh();
		}

		@Override
		public int getStyle() {
			return IAction.AS_PUSH_BUTTON;
		}
	}

	private final PropertySheetPage myPage;

	private final Repository myRepository;

	private final String myBranchName;

	private ActionContributionItem editAction;

	/**
	 * @param repository
	 *            the repository
	 * @param fullBranchName
	 *            the full name of the branch to show
	 * @param page
	 *            the page showing the properties
	 */
	public BranchPropertySource(Repository repository, String fullBranchName,
			PropertySheetPage page) {
		myPage = page;
		myBranchName = Repository.shortenRefName(fullBranchName);
		myRepository = repository;

		synchronized (myPage) {
			// check if the actions are already there, if not, create them
			IActionBars bars = myPage.getSite().getActionBars();
			IToolBarManager mgr = bars.getToolBarManager();

			editAction = ((ActionContributionItem) mgr.find(EDITACTIONID));
			if (editAction != null)
				((EditAction) editAction.getAction()).setSource(this);
			else {
				editAction = new ActionContributionItem(new EditAction(
						UIText.RepositoryPropertySource_EditConfigButton,
						UIIcons.EDITCONFIG, this));

				mgr.add(new Separator());
				mgr.add(editAction);
			}

			mgr.update(false);
		}
	}

	@Override
	public Object getEditableValue() {
		return null;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> resultList = new ArrayList<>();

		PropertyDescriptor desc = new PropertyDescriptor(
				ConfigConstants.CONFIG_KEY_MERGE, UIText.BranchPropertySource_UpstreamBranchDescriptor);
		desc.setCategory(UIText.BranchPropertySource_UpstreamConfigurationCategory);
		resultList.add(desc);
		desc = new PropertyDescriptor(ConfigConstants.CONFIG_KEY_REMOTE,
				UIText.BranchPropertySource_RemoteDescriptor);
		desc.setCategory(UIText.BranchPropertySource_UpstreamConfigurationCategory);
		resultList.add(desc);
		desc = new PropertyDescriptor(ConfigConstants.CONFIG_KEY_REBASE,
				UIText.BranchPropertySource_RebaseDescriptor);
		desc.setCategory(UIText.BranchPropertySource_UpstreamConfigurationCategory);
		resultList.add(desc);

		return resultList.toArray(new IPropertyDescriptor[0]);
	}

	@Override
	public Object getPropertyValue(Object id) {
		String actId = ((String) id);
		String value = myRepository.getConfig().getString(
				ConfigConstants.CONFIG_BRANCH_SECTION, myBranchName, actId);
		if (value == null || value.length() == 0)
			return UIText.BranchPropertySource_ValueNotSet;

		return value;
	}

	@Override
	public boolean isPropertySet(Object id) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object id) {
		// no editing here
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		// no editing here
	}
}
