/*******************************************************************************
 * Copyright (C) 2010, 2020 Dariusz Luksza <dariusz@luksza.org> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.settings.GitSettings;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.GpgConfig;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.team.core.TeamException;

/**
 * Creates a tag. Semantics are subtly different from JGit's {@link TagCommand}:
 * <ul>
 * <li>The {@code annotated} flag does not affect signing in any way. (In git,
 * -a suppresses tags.forceSignAnnotated.)</li>
 * <li>Creating a lightweight tag with the same name and pointing at the same
 * commit is allowed without the {@code force} flag.</li>
 * <li>For historical reasons, creating the every same annotated tag pointing at
 * the same commit is also allowed without the {@code force} flag, although this
 * looks more like it was allowed by accident, and it won't work for signed
 * tags.</li>
 * <li>If a lightweight tag is requested and no signing will take place, the
 * message and tagger are silently ignored. JGit would throw an exception if a
 * message or tagger are set for a lightweight tag.</li>
 * </ul>
 */
public class TagOperation implements IEGitOperation {

	private final @NonNull Repository repository;

	private boolean force;

	private boolean annotated = true;

	private Boolean sign;

	private @NonNull String name = ""; //$NON-NLS-1$

	private String message;

	private RevCommit target;

	private PersonIdent tagger;

	private CredentialsProvider credentialsProvider;

	/**
	 * Creates a new {@link TagOperation} to create a tag in a given
	 * {@link Repository}.
	 *
	 * @param repository
	 *            the {@link Repository} to create the tag in
	 */
	public TagOperation(@NonNull Repository repository) {
		this.repository = repository;
	}

	/**
	 * Sets whether an already existing tag may be updated by this
	 * {@link TagOperation}.
	 *
	 * @param force
	 *            whether to allow updating an existing tag
	 * @return {@code this}
	 */
	public TagOperation setForce(boolean force) {
		this.force = force;
		return this;
	}

	/**
	 * Retrieves whether updating an existing is allowed in this
	 * {@link TagOperation}.
	 *
	 * @return {@code true} if allowed; {@code false} otherwise
	 */
	public boolean isForce() {
		return force;
	}

	/**
	 * Sets whether this {@link TagOperation} shall create an annotated tag.
	 * <p>
	 * If set to {@code false}, a potentially set {@link #setMessage(String)
	 * message} or {@link #setTagger(PersonIdent) tagger} is ignored.
	 * </p>
	 *
	 * @param annotated
	 *            {@code false} if a lightweight tag is to be created,
	 *            {@code true} otherwise (default)
	 * @return {@code this}
	 * @see #setMessage(String)
	 * @see #setTagger(PersonIdent)
	 */
	public TagOperation setAnnotated(boolean annotated) {
		this.annotated = annotated;
		return this;
	}

	/**
	 * Retrieves whether this {@link TagOperation} shall create an annotated
	 * tag.
	 *
	 * @return {@code true} if allowed; {@code false} otherwise
	 */
	public boolean isAnnotated() {
		return annotated;
	}

	/**
	 * Sets whether this {@link TagOperation} shall create a signed tag.
	 *
	 * @param sign
	 *            {@code true} to create a signed tag, {@code false} if not, or
	 *            {@code null} to let the git config decide (default)
	 * @return {@code this}
	 */
	public TagOperation setSign(Boolean sign) {
		this.sign = sign;
		return this;
	}

	/**
	 * Retrieves whether this {@link TagOperation} shall create a signed tag.
	 *
	 * @return whether the tag will be signed; {@code null} means the git config
	 *         decides
	 */
	@Nullable
	public Boolean getSign() {
		return sign;
	}

	/**
	 * Sets the name of the tag to be created by this {@link TagOperation}.
	 *
	 * @param name
	 *            of the tag to be created
	 * @return {@code this}
	 */
	public TagOperation setName(@NonNull String name) {
		this.name = name;
		return this;
	}

	/**
	 * Retrieves the name of the tag to be created by this {@link TagOperation}.
	 *
	 * @return the tag name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the message of the tag to be created by this {@link TagOperation}.
	 * <p>
	 * Any message set will be ignored if the tag is to be a lightweight tag.
	 * </p>
	 *
	 * @param message
	 *            of the tag to be created
	 * @return {@code this}
	 */
	public TagOperation setMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Retrieves the message of the tag to be created by this
	 * {@link TagOperation}.
	 *
	 * @return the tag name
	 * @see #setAnnotated(boolean)
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the target {@link RevCommit} for the tag to be created by this
	 * {@link TagOperation}.
	 *
	 * @param target
	 *            {@link RevCommit} the tag shall point to
	 * @return {@code this}
	 */
	public TagOperation setTarget(@NonNull RevCommit target) {
		this.target = target;
		return this;
	}

	/**
	 * Retrieves the target of the tag to be created by this
	 * {@link TagOperation}.
	 *
	 * @return the target {@link RevCommit}
	 */
	public RevCommit getTarget() {
		return target;
	}

	/**
	 * Sets the {@link PersonIdent} to use as the author of the tag to be
	 * created by this {@link TagOperation}.
	 * <p>
	 * A possibly set tagger will be ignored if the tag is to be a lightweight
	 * tag.
	 * </p>
	 *
	 * @param tagger
	 *            the {@link PersonIdent} to use, or {@code null} to use a
	 *            default author based on the repository configuration (default)
	 * @return {@code this}
	 * @see #setAnnotated(boolean)
	 */
	public TagOperation setTagger(PersonIdent tagger) {
		this.tagger = tagger;
		return this;
	}

	/**
	 * Retrieves the author of the tag to be created by this
	 * {@link TagOperation}.
	 *
	 * @return the {link PersonIdent} used as author, or {@code null} if none
	 *         set
	 */
	public PersonIdent getTagger() {
		return tagger;
	}

	/**
	 * Sets a {@link CredentialsProvider} that may be used during signing.
	 *
	 * @param credentialsProvider
	 * @return {@code this}
	 */
	public TagOperation setCredentialsProvider(
			CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
		return this;
	}

	/**
	 * Retrieves the {@link CredentialsProvider} set on this
	 * {@link TagOperation}.
	 *
	 * @return the {@link CredentialsProvider} or {@code null} if none is set
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.setTaskName(MessageFormat
				.format(CoreText.TagOperation_performingTagging, getName()));
		try (Git git = new Git(repository)) {
			TagCommand command = git.tag()
					.setName(getName())
					.setForceUpdate(isForce())
					.setObjectId(getTarget());
			// Because of tag.forceSignAnnotated we have to be careful
			// not to call setAnnotated(true) explicitly.
			// TagCommand.setAnnotated() corresponds to the "-a"
			// command-line option being given explicitly, which would
			// force an unsigned tag unless tag.gpgSign was also true or
			// the user also gave the -s option. "-s" (or "--no-sign")
			// is sign != null here.
			//
			// So we must not setAnnotated(true) if sign == null,
			// otherwise tag.forceSignAnnotated = true would be ignored.
			if (!isAnnotated()) {
				// We want a lightweight tag, unless it's signed
				if (Boolean.FALSE.equals(sign)) {
					setMessage(null);
					setTagger(null);
					command.setAnnotated(false);
				} else if (sign == null) {
					// User did not decide explicitly.
					GpgConfig config = new GpgConfig(repository.getConfig());
					if (!config.isSignAllTags()) {
						setMessage(null);
						setTagger(null);
						command.setAnnotated(false);
					}
				}
			}
			command.setMessage(getMessage()).setTagger(getTagger());
			if (sign != null) {
				command.setSigned(sign.booleanValue());
			}
			// A CredentialsProvider might be needed when signing to
			// get the passphrase for an encrypted key
			CredentialsProvider provider = getCredentialsProvider();
			if (provider != null) {
				// If none is set explicitly, the command will fall back to
				// CredentialsProvider.getDefault()
				command.setCredentialsProvider(provider);
			}
			// Ensure the Eclipse preference, if set, overrides the git config
			File gpg = GitSettings.getGpgExecutable();
			if (gpg != null) {
				GpgConfig cfg = new GpgConfig(repository.getConfig()) {

					@Override
					public String getProgram() {
						return gpg.getAbsolutePath();
					}
				};
				command.setGpgConfig(cfg);
			}
			command.call();
			progress.worked(1);
		} catch (RefAlreadyExistsException e) {
			// NO_CHANGE: update of existing lightweight tag to the same commit.
			if (!RefUpdate.Result.NO_CHANGE.equals(e.getUpdateResult())) {
				throw new TeamException(MessageFormat.format(
						CoreText.TagOperation_taggingFailure, getName(),
						e.getMessage()), e);
			}
		} catch (GitAPIException e) {
			throw new TeamException(
					MessageFormat.format(CoreText.TagOperation_taggingFailure,
							getName(), e.getMessage()),
					e);
		}
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

}
