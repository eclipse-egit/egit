package org.eclipse.egit.ui.internal.clipboard;

import org.eclipse.egit.ui.internal.clone.GitUrlChecker;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

/**
 * Property Tester used to test the clipboard content. Does it contain an GIT
 * url ? Used for the paste active when condition.
 */
public class ClipboardPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {

		boolean value = internalTest(property);
		boolean trace = GitTraceLocation.CLIPBOARD.isActive();
		if (trace) {
			String clipboardText = getClipboardTextContent();
			GitTraceLocation
					.getTrace()
					.trace(GitTraceLocation.CLIPBOARD.getLocation(),
							"prop " + property + " The clipboard text value is " //$NON-NLS-1$ //$NON-NLS-2$
									+ (clipboardText == null
											? " No text value in clipboard " //$NON-NLS-1$
											: "'" + clipboardText + "'") //$NON-NLS-1$ //$NON-NLS-2$
									+ " = " + value //$NON-NLS-1$
									+ ", expected = " + expectedValue); //$NON-NLS-1$
		}
		return computeResult(expectedValue, value);
	}

	private boolean internalTest(String property) {
		if (property.equals("containsGitURL")) { //$NON-NLS-1$
			Object content = getClipboardTextContent();
			if (content != null) {

				String sanitized = GitUrlChecker.sanitizeAsGitUrl(content.toString());
				return (GitUrlChecker.isValidGitUrl(sanitized));
			}
		}

		return false;
	}

	/**
	 * Extract the text value in the clipboard. Return "null" if nothing
	 *
	 * @return "null" if nothing in clipboard or
	 */
	private String getClipboardTextContent() {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		Object content = clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();
		return content == null ? null : content.toString();
	}

}
