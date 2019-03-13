package org.eclipse.egit.ui.internal.repository.tree.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryGroups;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryNode;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.util.StringUtils;

/**
 * Adds repository group, if repositories are selected, they are added to the
 * group
 */
public class AddRepositoryGroupCommand
		extends RepositoriesViewCommandHandler<RepositoryTreeNode> {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		RepositoryGroups groups = new RepositoryGroups();

		Optional<String> optionalGroupName = getNewGroupName(event, groups);
		if (optionalGroupName.isPresent()) {
			String group = optionalGroupName.get();
			groups.addGroup(group);
			List<String> repoDirs = getSelectedRepositories(event);
			if (!repoDirs.isEmpty()) {
				groups.addRepositoriesToGroup(group, repoDirs);
			}
			getView(event).refresh();
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

	private Optional<String> getNewGroupName(ExecutionEvent event,
			RepositoryGroups groups) throws ExecutionException {
		InputDialog inputDialog = new InputDialog(getActiveShell(event),
				"Create Repository Group", //$NON-NLS-1$
				"Input the name of the new group", //$NON-NLS-1$
				"", //$NON-NLS-1$
				new IInputValidator() {

					@Override
					public String isValid(String name) {
						if (StringUtils.isEmptyOrNull(name)) {
							return "name must not be null";//$NON-NLS-1$
						} else if (groups.getGroupNames()
								.contains(name.trim())) {
							return "group already exists";//$NON-NLS-1$
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
