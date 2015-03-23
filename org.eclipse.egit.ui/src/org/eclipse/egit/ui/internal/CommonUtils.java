/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2013, Michael Keppler <michael.keppler@gmx.de>
 * Copyright (C) 2014, IBM Corporation (Markus Keller <markus_keller@ch.ibm.com>)
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Class containing all common utils
 */
public class CommonUtils {

	private CommonUtils() {
		// non-instantiable utility class
	}

	/**
	 * Instance of comparator that sorts strings in ascending alphabetical and
	 * numerous order (also known as natural order), case insensitive.
	 *
	 * The comparator is guaranteed to return a non-zero value if
	 * string1.equals(String2) returns false
	 */
	public static final Comparator<String> STRING_ASCENDING_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if (o1.length() == 0 || o2.length() == 0)
				return o1.length() - o2.length();

			LinkedList<String> o1Parts = splitIntoDigitAndNonDigitParts(o1);
			LinkedList<String> o2Parts = splitIntoDigitAndNonDigitParts(o2);

			Iterator<String> o2PartsIterator = o2Parts.iterator();

			for (String o1Part : o1Parts) {
				if (!o2PartsIterator.hasNext())
					return 1;

				String o2Part = o2PartsIterator.next();

				int result;

				if (Character.isDigit(o1Part.charAt(0)) && Character.isDigit(o2Part.charAt(0))) {
					o1Part = stripLeadingZeros(o1Part);
					o2Part = stripLeadingZeros(o2Part);
					result = o1Part.length() - o2Part.length();
					if (result == 0)
						result = o1Part.compareToIgnoreCase(o2Part);
				} else {
					result = o1Part.compareToIgnoreCase(o2Part);
				}

				if (result != 0)
					return result;
			}

			if (o2PartsIterator.hasNext())
				return -1;
			else {
				// strings are equal (in the Object.equals() sense)
				// or only differ in case and/or leading zeros
				return o1.compareTo(o2);
			}
		}
	};

	/**
	 * Instance of comparator which sorts {@link Ref} names using
	 * {@link CommonUtils#STRING_ASCENDING_COMPARATOR}.
	 */
	public static final Comparator<Ref> REF_ASCENDING_COMPARATOR = new Comparator<Ref>() {
		@Override
		public int compare(Ref o1, Ref o2) {
			return STRING_ASCENDING_COMPARATOR.compare(o1.getName(), o2.getName());
		}
	};

	/**
	 * Comparator for comparing {@link IResource} by the result of
	 * {@link IResource#getName()}.
	 */
	public static final Comparator<IResource> RESOURCE_NAME_COMPARATOR = new Comparator<IResource>() {
		@Override
		public int compare(IResource r1, IResource r2) {
			return Policy.getComparator().compare(r1.getName(), r2.getName());
		}
	};

	/**
	 * Programatically run command based on it id and given selection
	 *
	 * @param commandId
	 *            id of command that should be run
	 * @param selection
	 *            given selection
	 * @return {@code true} when command was successfully executed,
	 *         {@code false} otherwise
	 */
	public static boolean runCommand(String commandId,
			IStructuredSelection selection) {
		ICommandService commandService = CommonUtils.getService(PlatformUI
				.getWorkbench(), ICommandService.class);
		Command cmd = commandService.getCommand(commandId);
		if (!cmd.isDefined())
			return false;

		IHandlerService handlerService = CommonUtils.getService(PlatformUI
				.getWorkbench(), IHandlerService.class);
		EvaluationContext c = null;
		if (selection != null) {
			c = new EvaluationContext(
					handlerService.createContextSnapshot(false),
					selection.toList());
			c.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);
			c.removeVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
		}
		try {
			if (c != null)
				handlerService.executeCommandInContext(
						new ParameterizedCommand(cmd, null), null, c);
			else
				handlerService.executeCommand(commandId, null);

			return true;
		} catch (CommandException ignored) {
			// Ignored
		}
		return false;
	}

	/**
	 * Retrieves the service corresponding to the given API.
	 * <p>
	 * Workaround for "Unnecessary cast" errors, see bug 441615. Can be removed
	 * when EGit depends on Eclipse 4.5 or higher.
	 *
	 * @param locator
	 *            the service locator, must not be null
	 * @param api
	 *            the interface the service implements, must not be null
	 * @return the service, or null if no such service could be found
	 * @see IServiceLocator#getService(Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getService(IServiceLocator locator, Class<T> api) {
		Object service = locator.getService(api);
		return (T) service;
	}

	/**
	 * @param element
	 * @param adapterType
	 * @return the adapted element, or null
	 */
	public static <T> T getAdapterForObject(Object element, Class<T> adapterType) {
		if (adapterType.isInstance(element)) {
			return adapterType.cast(element);
		}
		if (element instanceof IAdaptable) {
			Object adapted = ((IAdaptable) element).getAdapter(adapterType);
			if (adapterType.isInstance(adapted)) {
				return adapterType.cast(adapted);
			}
		}
		Object adapted = Platform.getAdapterManager().getAdapter(element,
				adapterType);
		if (adapterType.isInstance(adapted)) {
			return adapterType.cast(adapted);
		}
		return null;
	}
	
	/**
	 * Returns the adapter corresponding to the given adapter class.
	 * <p>
	 * Workaround for "Unnecessary cast" errors, see bug 460685. Can be removed
	 * when EGit depends on Eclipse 4.5 or higher.
	 *
	 * @param adaptable
	 *            the adaptable
	 * @param adapterClass
	 *            the adapter class to look up
	 * @return a object of the given class, or <code>null</code> if this object
	 *         does not have an adapter for the given class
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAdapter(IAdaptable adaptable,
			Class<T> adapterClass) {
		Object adapter = adaptable.getAdapter(adapterClass);
		return (T) adapter;
	}
	

	private static LinkedList<String> splitIntoDigitAndNonDigitParts(
			String input) {
		LinkedList<String> parts = new LinkedList<String>();
		int partStart = 0;
		boolean previousWasDigit = Character.isDigit(input.charAt(0));
		for (int i = 1; i < input.length(); i++) {
			boolean isDigit = Character.isDigit(input.charAt(i));
			if (isDigit != previousWasDigit) {
				parts.add(input.substring(partStart, i));
				partStart = i;
				previousWasDigit = isDigit;
			}
		}
		parts.add(input.substring(partStart));
		return parts;
	}

	private static String stripLeadingZeros(String input) {
		for (int i = 0; i < input.length(); i++)
			if (input.charAt(i) != '0')
				return input.substring(i);
		return ""; //$NON-NLS-1$
	}
}
