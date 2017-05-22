package org.eclipse.egit.ui.test.commitmessageprovider;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.ICommitMessageProvider2;

public class CommitMessageProviderFactory {
	
	private static final String MESSAGE_PART_BEFORE_CARET = "Caret is supposed to be there: -->";
	private static final String MESSAGE_PART_AFTER_CARET = "<--\n\nThis is a commit message from " + TestCommitMessageProvider.class.getName();; 
	private static final String MESSAGE = MESSAGE_PART_BEFORE_CARET + MESSAGE_PART_AFTER_CARET;
	private static final int CARET_POSITION = MESSAGE_PART_BEFORE_CARET.length();
	
	private static boolean active = false;
	
	private static class CommitMessageProviderInstanceHolder {
		private static final ICommitMessageProvider2 INSTANCE = new TestCommitMessageProvider();
	}

	public static ICommitMessageProvider2 getCommitMessageProvider() {
		if (active) {
			return CommitMessageProviderInstanceHolder.INSTANCE;
		} else {
			return null;
		}
	}
	
	public static void activate() {
		active = true;
	}
	
	public static void deactivate() {
		active = false;
	}
	
	public static int getProvidedCaretPosition() {
		return CARET_POSITION;
	}
	
	public static class TestCommitMessageProvider implements ICommitMessageProvider2 {

		@Override
		public String getMessage(IResource[] resources) {
			return MESSAGE;
		}

		@Override
		public CommitMessageWithCaretPosition getCommitMessageWithPosition(IResource[] resources) {
			return new CommitMessageWithCaretPosition(MESSAGE, CARET_POSITION);
		}

	}

}
