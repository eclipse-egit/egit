package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.widgets.Shell;

/**
 * Adds repository group, if repositories are selected, they are added to the
 * group
 */
public class AddRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroups groups = new RepositoryGroups();

		Optional<String> optionalGroupName = getNewGroupName(
				getActiveShell(event),
				UIText.RepositoriesView_AddRepoGroup_title, groups, ""); //$NON-NLS-1$
		if (optionalGroupName.isPresent()) {
			String group = optionalGroupName.get();
			Optional<UUID> groupUUID = groups.addGroup(group);
			if (groupUUID.isPresent()) {
				List<String> repoDirs = getSelectedRepositories(event);
				if (!repoDirs.isEmpty()) {
					groups.addRepositoriesToGroup(groupUUID.get(), repoDirs);
				}
				getView(event).refresh();
			}
		}
		return null;
	}

	private List<String> getSelectedRepositories(ExecutionEvent event)
			throws ExecutionException {
		List<String> repoDirs = new ArrayList<>();
		List<RepositoryTreeNode> elements = getSelectedNodes(event);
		for (Object element : elements) {
			if (element instanceof RepositoryNode) {
				File dir = ((RepositoryNode) element).getRepository()
						.getDirectory();
				repoDirs.add(dir.toString());
			}
		}
		return repoDirs;
	}

	static Optional<String> getNewGroupName(Shell shell, String title,
			RepositoryGroups groups, String initialName) {
		InputDialog inputDialog = new InputDialog(shell, title,
				UIText.RepositoriesView_RepoGroup_enterName,
				initialName,
				new IInputValidator() {

					@Override
					public String isValid(String name) {
						if (name==null || StringUtils.isEmptyOrNull(name.trim())) {
							return UIText.RepositoriesView_RepoGroup_validateEmptyName;
						} else {
							if (groups.groupExists(name)) {
								return UIText.RepositoriesView_RepoGroup_validateGroupExists;
							}
						}
						return null;
					}

				});

		if (inputDialog.open() == Window.OK) {
			return Optional.of(inputDialog.getValue());
		} else {
			return Optional.empty();
		}
	}
}
