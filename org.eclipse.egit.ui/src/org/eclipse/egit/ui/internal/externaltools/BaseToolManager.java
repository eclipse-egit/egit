/*******************************************************************************
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.externaltools;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author anb0s
 *
 */
public class BaseToolManager {

	private Set<PreDefinedTool> listPreDefined = new HashSet<>();
	private Set<UserOverloadedTool> listUserOverloaded = new HashSet<>();
	private Set<UserDefinedTool> listUserDefined = new HashSet<>();
	private AttributeSet attributes = new AttributeSet();

	/**
	 *
	 */
	public BaseToolManager() {

	}

	/**
	 * @return the default tool name
	 */
	public String getDefaultToolName() {
		Attribute attr = getAttribute(null, "tool"); //$NON-NLS-1$
		String name = "none"; //$NON-NLS-1$
		if (attr != null) {
			String attrValue = attr.getValue();
			if (attrValue != null && !attrValue.equals("")) { //$NON-NLS-1$
				name = attrValue;
			}
		}
		return name;
	}

	/**
	 * @param name
	 *            tool name
	 * @return tool command or null
	 */
	public String getToolCommand(String name) {
		ITool tool = getTool(name);
		if (tool != null) {
			return tool.getCommand();
		}
		return null;
	}

	/**
	 * remove all user defined tools (user overloaded and user defined)
	 */
	public void removeAllUserDefinitions() {
		listUserOverloaded.clear();
		listUserDefined.clear();
	}

	/**
	 * @param name
	 *            tool name
	 * @param path
	 *            tool path
	 * @param options
	 *            tool options (can be multiple strings)
	 * @return true if successful
	 */
	public boolean addPreDefinedTool(String name, String path,
			String... options) {
		listPreDefined.add(new PreDefinedTool(name, path, options));
		return false;
	}

	/**
	 * @param name
	 *            tool name
	 * @param path
	 *            tool path
	 * @return true if successful
	 */
	public boolean addUserOverloadedTool(String name, String path) {
		removeUserOverloadedTool(name);
		ITool tool = getPreDefinedTool(name);
		if (tool != null) {
			UserOverloadedTool userTool = new UserOverloadedTool(tool, path);
			listUserOverloaded.add(userTool);
			return true;
		}
		return false;
	}

	/**
	 * @param name
	 *            tool name
	 * @param cmd
	 *            tool command
	 * @return true if successful
	 */
	public boolean addUserDefinedTool(String name, String cmd) {
		removeUserDefinedTool(name);
		UserDefinedTool userTool = new UserDefinedTool(name, cmd);
		listUserDefined.add(userTool);
		return true;
	}

	/**
	 * @param name
	 * @return the tool
	 */
	public ITool getTool(String name) {
		// check the user defined first
		ITool tool = getUserDefinedTool(name);
		// if not found the internal
		if (tool == null) {
			tool = getUserOverloadedTool(name);
			if (tool == null) {
				tool = getPreDefinedTool(name);
			}
		}
		return tool;
	}

	private void removeUserOverloadedTool(String name) {
		ITool tool = getUserOverloadedTool(name);
		if (tool != null) {
			listUserOverloaded.remove(tool);
		}
	}

	private void removeUserDefinedTool(String name) {
		ITool tool = getUserDefinedTool(name);
		if (tool != null) {
			listUserDefined.remove(tool);
		}
	}

	private ITool getPreDefinedTool(String name) {
		if (name != null) {
			for (Iterator<PreDefinedTool> iterator = listPreDefined
					.iterator(); iterator.hasNext();) {
				ITool tool = iterator.next();
				if (tool.getName().equals(name)) {
					return tool;
				}
			}
		}
		return null;
	}

	private ITool getUserOverloadedTool(String name) {
		if (name != null) {
			for (Iterator<UserOverloadedTool> iterator = listUserOverloaded
					.iterator(); iterator.hasNext();) {
				ITool tool = iterator.next();
				if (tool.getName().equals(name)) {
					return tool;
				}
			}
		}
		return null;
	}

	private ITool getUserDefinedTool(String name) {
		if (name != null) {
			for (Iterator<UserDefinedTool> iterator = listUserDefined
					.iterator(); iterator.hasNext();) {
				ITool tool = iterator.next();
				if (tool.getName().equals(name)) {
					return tool;
				}
			}
		}
		return null;
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @param attrValue
	 */
	public void addAttribute(String toolName, String attrName,
			String attrValue) {
		AttributeSet attrSet = getAttributeSet(toolName);
		if (attrSet != null) {
			attrSet.addAttribute(attrName, attrValue);
		}
	}

	/**
	 * @param toolName
	 * @param attrName
	 */
	public void removeAttribute(String toolName, String attrName) {
		AttributeSet attrSet = getAttributeSet(toolName);
		if (attrSet != null) {
			attrSet.removeAttribute(attrName);
		}
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @param fallback
	 * @return the value
	 */
	public String getAttributeValue(String toolName, String attrName,
			boolean fallback) {
		Attribute attr = getAttribute(toolName, attrName);
		if (attr == null && fallback) {
			attr = getAttribute(null, attrName);
		}
		if (attr != null) {
			return attr.getValue();
		}
		return null;
	}

	/**
	 * @param toolName
	 * @param attrName
	 * @param fallback
	 * @return the value
	 */
	public boolean getAttributeValueBoolean(String toolName, String attrName,
			boolean fallback) {
		Attribute attr = getAttribute(toolName, attrName);
		if (attr == null && fallback) {
			attr = getAttribute(null, attrName);
		}
		if (attr != null) {
			return attr.getValueBoolean();
		}
		return false;
	}

	private Attribute getAttribute(String toolName, String attrName) {
		AttributeSet attrSet = getAttributeSet(toolName);
		if (attrSet != null) {
			return attrSet.getAttribute(attrName);
		}
		return null;
	}

	private AttributeSet getAttributeSet(String toolName) {
		if (toolName != null) {
			ITool tool = getTool(toolName);
			if (tool != null) {
				return tool.getAttributeSet();
			}
		} else {
			return attributes;
		}
		return null;
	}

}
