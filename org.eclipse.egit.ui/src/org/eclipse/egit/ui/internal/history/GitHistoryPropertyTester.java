package org.eclipse.egit.ui.internal.history;

import java.io.File;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;

/**
 * A {@link PropertyTester} specific to the git history page.
 */
public class GitHistoryPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if ("isSingleFileHistory".equals(property)) { //$NON-NLS-1$
			GitHistoryPage page = getGitHistoryPage(receiver);
			if (page == null) {
				return false;
			}
			Object single = page.getInputInternal().getSingleFile();
			if (expectedValue instanceof String) {
				if (expectedValue.equals("resource")) { //$NON-NLS-1$
					return single instanceof IResource;
				} else if (expectedValue.equals("file")) { //$NON-NLS-1$
					return single instanceof File;
				}
			} else {
				return computeResult(expectedValue, single != null);
			}
		}
		return false;
	}

	private GitHistoryPage getGitHistoryPage(Object receiver) {
		if (!(receiver instanceof IHistoryView)) {
			return null;
		}
		IHistoryPage page = ((IHistoryView) receiver).getHistoryPage();
		if (page instanceof GitHistoryPage) {
			return (GitHistoryPage) page;
		}
		return null;
	}

}
