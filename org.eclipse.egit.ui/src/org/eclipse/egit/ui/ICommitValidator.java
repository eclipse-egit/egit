package org.eclipse.egit.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Allows implementation of a commit validator.
 * <p>
 * A commit validator has the chance to analyze and validate each new commit
 * object that is created. The validator does <b>not</b> have the ability to
 * reject the commit directly. Feedback to the user is collected from all
 * available validators and presented in a way to make the user aware of issues.
 * The validator can influence the presentation by setting the issue severity
 * accordingly.
 * <p>
 * This validation is <b>only</b> available in the UI, thus the location.
 */
public interface ICommitValidator {

	/**
	 * Validate the given commit, and provide a validation result. Results with
	 * severity {@link IStatus#INFO}, {@link IStatus#WARNING} and
	 * {@link IStatus#ERROR} are presented to the user in a dialog after
	 * committing with according icons along with the status message.
	 *
	 * @param commit
	 *            the commit to validate
	 * @return the result of the validation
	 */
	public IStatus validate(RevCommit commit);

}
