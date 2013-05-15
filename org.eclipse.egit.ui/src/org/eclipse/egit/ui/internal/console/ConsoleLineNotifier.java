package org.eclipse.egit.ui.internal.console;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.debug.ui.console.IConsoleLineTrackerExtension;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * Tracks text appended to the console and notifies listeners in terms of whole
 * lines.
 * <p>
 * A copy of org.eclipse.debug.internal.ui.views.console.ConsoleLineNotifier
 */
public class ConsoleLineNotifier implements IPatternMatchListener, IPropertyChangeListener {
	/**
	 * Console listeners
	 */
	private List fListeners = new ArrayList(2);

	/**
	 * The console this notifier is tracking
	 */
	private GitBashConsole fConsole = null;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse.ui.console.TextConsole)
	 */
	public void connect(TextConsole console) {
		Assert.isLegal(console instanceof GitBashConsole);
		fConsole = (GitBashConsole) console;
		GitBashLineTracker lineTracker = new GitBashLineTracker();
		lineTracker.init(fConsole);
		addConsoleListener(lineTracker);
		fConsole.addPropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IPatternMatchListener#disconnect()
	 */
	public synchronized void disconnect() {
        try {
            IDocument document = fConsole.getDocument();
            if (document != null) {
                int lastLine = document.getNumberOfLines() - 1;
                if (document.getLineDelimiter(lastLine) == null) {
                    IRegion lineInformation = document.getLineInformation(lastLine);
                    lineAppended(lineInformation);
                }
            }
        } catch (BadLocationException e) {
			// ignore
        }
    }

    /**
     * Notification the console's streams have been closed
     */
    public synchronized void consoleClosed() {
        int size = fListeners.size();
        for (int i = 0; i < size; i++) {
            IConsoleLineTracker tracker = (IConsoleLineTracker) fListeners.get(i);
            if (tracker instanceof IConsoleLineTrackerExtension) {
                ((IConsoleLineTrackerExtension) tracker).consoleClosed();
            }
            tracker.dispose();
        }

        fConsole = null;
        fListeners = null;
    }

	/**
     * Adds the given listener to the list of listeners notified when a line of
     * text is appended to the console.
     *
     * @param listener
     */
	public void addConsoleListener(IConsoleLineTracker listener) {
        if (!fListeners.contains(listener))
            fListeners.add(listener);
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IPatternMatchListener#matchFound(org.eclipse.ui.console.PatternMatchEvent)
     */
    public void matchFound(PatternMatchEvent event) {
        try  {
            IDocument document = fConsole.getDocument();
            int lineOfOffset = document.getLineOfOffset(event.getOffset());
            String delimiter = document.getLineDelimiter(lineOfOffset);
            int strip = delimiter==null ? 0 : delimiter.length();
            Region region = new Region(event.getOffset(), event.getLength()-strip);
            lineAppended(region);
        } catch (BadLocationException e) {}
    }

	/**
	 * @param region
	 */
    public void lineAppended(IRegion region) {
        int size = fListeners.size();
        for (int i=0; i<size; i++) {
            IConsoleLineTracker tracker = (IConsoleLineTracker) fListeners.get(i);
            tracker.lineAppended(region);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        if(event.getProperty().equals(IConsoleConstants.P_CONSOLE_OUTPUT_COMPLETE)) {
            fConsole.removePropertyChangeListener(this);
            consoleClosed();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IPatternMatchListener#getPattern()
     */
    public String getPattern() {
		return "\\$>.*\\r(\\n?)|\\$>.*\\n"; //$NON-NLS-1$
//    	return ".*\\r(\\n?)|.*\\n"; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IPatternMatchListener#getCompilerFlags()
     */
    public int getCompilerFlags() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.console.IPatternMatchListener#getLineQualifier()
     */
    public String getLineQualifier() {
        return "\\n|\\r"; //$NON-NLS-1$
    }

}
