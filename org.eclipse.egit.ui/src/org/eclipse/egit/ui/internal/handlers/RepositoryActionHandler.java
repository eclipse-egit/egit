package org.eclipse.egit.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.actions.RepositoryAction;

/**
 * A handler class for Team Actions on Git controlled projects
 */

public class RepositoryActionHandler extends AbstractHandler {

	/**
	 * a simple wrapper for actions. Uses a command parameter and reflection to
	 * fire off the passed-in action class.
	 */

	public Object execute(ExecutionEvent event) throws ExecutionException {
		String actionClass = event
				.getParameter("org.eclipse.egit.ui.command.action.class");
		try {
			java.lang.reflect.Constructor co;
			co = Class.forName(actionClass).getConstructor();
			RepositoryAction action = (RepositoryAction) co.newInstance();
			action.execute(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}