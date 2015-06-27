/*******************************************************************************
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.externaltools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.internal.preferences.GitPreferenceRoot;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

/**
 * @author anb0s
 *
 */
public class ToolsUtils {

	/**
	 * @param filePath
	 *            path to the file
	 * @param inputStream
	 *            the used input stream
	 */
	public static void createAndFillFileForCompare(String filePath,
			InputStream inputStream) {
		FileOutputStream outputStream = createFileForCompare(filePath);
		try {
			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outputStream != null) {
				try {
					// outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	/**
	 * @return the created file
	 */
	public static File createDirectoryForTempFiles() {
		File tempDir = null;
		try {
			tempDir = ToolsUtils.createTempDirectory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempDir;
	}

	/**
	 * @return the created temp directory
	 * @throws IOException
	 */
	public static File createTempDirectory() throws IOException {
		File temp = File.createTempFile("temp", //$NON-NLS-1$
				Long.toString(System.nanoTime()));
		if (!(temp.delete())) {
			throw new IOException(
					"Could not delete temp file: " + temp.getAbsolutePath()); //$NON-NLS-1$
		}
		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " //$NON-NLS-1$
					+ temp.getAbsolutePath());
		}
		return (temp);
	}

	/**
	 * @param parent
	 * @param subDirName
	 * @return the created sub directory
	 * @throws IOException
	 */
	public static File createSubDirectory(File parent, String subDirName)
			throws IOException {
		File subDir = new File(parent, subDirName);
		if (!(subDir.mkdir())) {
			throw new IOException("Could not create directory: " //$NON-NLS-1$
					+ subDir.getAbsolutePath());
		}
		return (subDir);
	}

	/**
	 * @param temp
	 */
	public static void deleteDirectoryForTempFiles(File temp) {
		try {
			deleteTempDirectory(temp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param temp
	 * @throws IOException
	 */
	public static void deleteTempDirectory(File temp) throws IOException {
		File[] files = temp.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteTempDirectory(f);
				} else {
					f.delete();
				}
			}
		}
		if (!(temp.delete())) {
			throw new IOException(
					"Could not delete temp file: " + temp.getAbsolutePath()); //$NON-NLS-1$
		}
	}

	/**
	 * @param workingDirectory
	 * @param mergedCompareFilePath
	 * @param localCompareFilePath
	 * @param remoteCompareFilePath
	 * @param baseCompareFilePath
	 * @param toolCmd
	 * @param tempDir
	 * @return the exit code
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static int executeTool(File workingDirectory,
			String mergedCompareFilePath,
			String localCompareFilePath, String remoteCompareFilePath,
			String baseCompareFilePath, String toolCmd, File tempDir)
					throws IOException, InterruptedException {
		if (mergedCompareFilePath != null && localCompareFilePath != null
				&& remoteCompareFilePath != null && toolCmd != null) {
			if (baseCompareFilePath == null) {
				baseCompareFilePath = ""; //$NON-NLS-1$
			}
			String osname = System.getProperty("os.name", "") //$NON-NLS-1$ //$NON-NLS-2$
					.toLowerCase();
			int indexLast = toolCmd.indexOf(".sh"); //$NON-NLS-1$
			if (osname.indexOf("windows") != -1 //$NON-NLS-1$
					&& indexLast != -1) {
				return runExternalToolMsysBash(workingDirectory, toolCmd,
						mergedCompareFilePath, localCompareFilePath,
						remoteCompareFilePath, baseCompareFilePath, tempDir);
			} else {
				return runExternalToolNative(workingDirectory, toolCmd,
						mergedCompareFilePath, localCompareFilePath,
						remoteCompareFilePath, baseCompareFilePath);
			}
		}
		return 0;
	}

	/**
	 * @param workingDirectory
	 * @param command
	 * @param mergedCompareFilePath
	 * @param localCompareFilePath
	 * @param remoteCompareFilePath
	 * @param baseCompareFilePath
	 * @return the exit code
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static int runExternalToolNative(File workingDirectory,
			String command,
			String mergedCompareFilePath, String localCompareFilePath,
			String remoteCompareFilePath, String baseCompareFilePath)
					throws IOException, InterruptedException {
		command = command.replace("$MERGED", mergedCompareFilePath); //$NON-NLS-1$
		command = command.replace("$LOCAL", localCompareFilePath); //$NON-NLS-1$
		command = command.replace("$REMOTE", remoteCompareFilePath); //$NON-NLS-1$
		command = command.replace("$BASE", baseCompareFilePath); //$NON-NLS-1$
		String[] envp = null;
		return runCommand(command, envp, workingDirectory);
	}

	/**
	 * @param workingDirectory
	 * @param cmd
	 * @param mergedCompareFilePath
	 * @param localCompareFilePath
	 * @param remoteCompareFilePath
	 * @param baseCompareFilePath
	 * @param reusedTempDir
	 * @return the exit code
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static int runExternalToolMsysBash(File workingDirectory, String cmd,
			String mergedCompareFilePath, String localCompareFilePath,
			String remoteCompareFilePath, String baseCompareFilePath,
			File reusedTempDir) throws IOException, InterruptedException {
		String command = cmd;
		String cmdFilePath = null;
		File tempDir = null;
		if (reusedTempDir == null) {
			tempDir = createTempDirectory();
		} else {
			tempDir = reusedTempDir;
		}
		File cmdFile = new File(tempDir, "jgit-windows.sh"); //$NON-NLS-1$
		cmdFilePath = cmdFile.getAbsolutePath();
		cmdFilePath = cmdFilePath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		// command = command.replace("$LOCAL", leftCompareFilePath);
		// //$NON-NLS-1$
		// command = command.replace("$REMOTE", rightCompareFilePath);
		// //$NON-NLS-1$
		Files.write(cmdFile.toPath(), command.getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);
		command = "cmd.exe /C start /wait \"$bash-name\" \"$bash-cmd\" --login -c \"" //$NON-NLS-1$
				+ cmdFilePath + "\""; //$NON-NLS-1$
		command = command.replace("$bash-name", "bash"); //$NON-NLS-1$ //$NON-NLS-2$
		command = command.replace("$bash-cmd", GitPreferenceRoot.getBashPath());//$NON-NLS-1$
		// "C:\\Program Files\\Git\\bin\\bash.exe"); //$NON-NLS-1$
		String[] envp = { "MERGED=" + mergedCompareFilePath, //$NON-NLS-1$
				"LOCAL=" + localCompareFilePath, //$NON-NLS-1$
				"REMOTE=" + remoteCompareFilePath, //$NON-NLS-1$
				"BASE=" + baseCompareFilePath //$NON-NLS-1$
		}; // $NON-NLS-1$
		int exitCode = runCommand(command, envp, workingDirectory);
		deleteTempDirectory(tempDir);
		return exitCode;
	}

	private static FileOutputStream createFileForCompare(String filePath) {
		FileOutputStream fop = null;
		File file;
		file = new File(filePath);
		try {
			fop = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// if file doesnt exists, then create it
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return fop;
	}

	private static int runCommand(String command, String[] envp,
			File workingDirectory)
			throws IOException, InterruptedException {
		int exitCode = 0;
		System.out.println("command: " + command); //$NON-NLS-1$
		String[] cmdarray = command.split("\\s+"); //$NON-NLS-1$
		Process p = Runtime.getRuntime().exec(cmdarray, envp, workingDirectory);
		System.out.println("Waiting for command..."); //$NON-NLS-1$
		exitCode = p.waitFor();
		System.out.println("command done."); //$NON-NLS-1$
		return exitCode;
	}

	/**
	 * @param baseDirPath
	 * @param fileName
	 * @param subName
	 * @param revision
	 * @param writeToTemp
	 * @return the temp file name loaded
	 */
	public static String loadToTempFile(File baseDirPath, String fileName,
			String subName, FileRevisionTypedElement revision,
			boolean writeToTemp) {
		String tempFileName = null;
		if (revision != null) {
			System.out.println("revision.getName: " //$NON-NLS-1$
					+ revision.getName());
			String revisionFilePath = revision.getPath();
			if (baseDirPath != null && revisionFilePath != null) {
				System.out.println("revision.getPath: " //$NON-NLS-1$
						+ revisionFilePath);
				try {
					if (writeToTemp) {
						File subPath = createSubDirectory(baseDirPath, subName);
						tempFileName = subPath.getAbsolutePath()
								+ File.separator + fileName;
					} else {
						tempFileName = baseDirPath.getAbsolutePath()
								+ File.separator + fileName + "." //$NON-NLS-1$
								+ subName;
					}
					createAndFillFileForCompare(tempFileName,
							revision.getContents());
				} catch (CoreException | IOException e) {
					e.printStackTrace();
				}
				System.out.println("tempFileName: " //$NON-NLS-1$
						+ tempFileName);
			}
		}
		return tempFileName;
	}

	/**
	 * @param textHeader
	 * @param message
	 * @return yes or no
	 */
	public static int askUserAboutToolExecution(String textHeader,
			String message) {
		MessageBox mbox = new MessageBox(Display.getCurrent().getActiveShell(),
				SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
		mbox.setText(textHeader);
		mbox.setMessage(message);
		return mbox.open();
	}

	/**
	 * @param textHeader
	 * @param message
	 * @return yes or no
	 */
	public static int informUserAboutError(String textHeader, String message) {
		MessageBox mbox = new MessageBox(Display.getCurrent().getActiveShell(),
				SWT.ICON_ERROR | SWT.OK);
		mbox.setText(textHeader);
		mbox.setMessage(message);
		return mbox.open();
	}

	/*
	public static void openInCompare(RevCommit commit1, RevCommit commit2,
			String commit1Path, String commit2Path, Repository repository,
			IWorkbenchPage workBenchPage) {
		if (GitPreferenceRoot.useEclipseDiffTool()) {
			openInCompareInternal(commit1, commit2, commit1Path, commit2Path,
					repository, workBenchPage);
		} else {
			openInCompareExternal(commit1, commit2, commit1Path, commit2Path,
					repository, workBenchPage);
		}
	}

	private static void openInCompareInternal(RevCommit commit1,
			RevCommit commit2, String commit1Path, String commit2Path,
			Repository repository, IWorkbenchPage workBenchPage) {
		final ITypedElement base = CompareUtils
				.getFileRevisionTypedElement(commit1Path, commit1, repository);
		final ITypedElement next = CompareUtils
				.getFileRevisionTypedElement(commit2Path, commit2, repository);
		CompareEditorInput in = new GitCompareFileRevisionEditorInput(base,
				next, null);
		CompareUtils.openInCompare(workBenchPage, repository, in);
	}

	private static void openInCompareExternal(RevCommit commit1,
			RevCommit commit2,
			String commit1Path, String commit2Path, Repository repository,
			IWorkbenchPage workBenchPage) {
		// TODO
		String diffCmd = GitPreferenceRoot.getExternalDiffToolCommand();

		createFileFromCommit(commit1, commit1Path, ".LOCAL", repository); //$NON-NLS-1$
		createFileFromCommit(commit2, commit2Path, ".REMOTE", repository); //$NON-NLS-1$

		MessageBox mbox = new MessageBox(Display.getCurrent().getActiveShell(),
                SWT.ICON_INFORMATION | SWT.OK);
		mbox.setText("getExternalDiffToolCommand"); //$NON-NLS-1$
		mbox.setMessage(diffCmd + "\n" + commit1Path + "\n" + commit2Path); //$NON-NLS-1$ //$NON-NLS-2$
		mbox.open();
	}

	@SuppressWarnings({ "resource", "null" })
	private static void createFileFromCommit(RevCommit commit,
			String commitPath, String fileNamePostfix, Repository repository) {

	     // source:
	     // https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/
    	 // org/dstadler/jgit/api/ReadFileFromCommit.java
		System.out.println("*** createFileFromCommit: " + commitPath); //$NON-NLS-1$
		RevTree tree = commit.getTree();
		System.out.println("Having tree: " + tree); //$NON-NLS-1$
		// now try to find a specific file
		TreeWalk treeWalk = new TreeWalk(repository);
		try {
			treeWalk.addTree(tree);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		treeWalk.setRecursive(true);
		treeWalk.setFilter(PathFilter.create(commitPath)); // $NON-NLS-1$
		try {
			if (!treeWalk.next()) {
				throw new IllegalStateException(
						"Did not find expected file '" + commitPath + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ObjectId objectId = treeWalk.getObjectId(0);
		ObjectLoader loader = null;
		try {
			loader = repository.open(objectId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// and then one can the loader to read the file
		try {
			loader.copyTo(System.out);
			FileOutputStream fop = createFileForCompare(
					ResourceUtil.getFileForLocation(repository, commitPath)
							.getRawLocation().toOSString(),
					fileNamePostfix);
			loader.copyTo(fop);
		} catch (MissingObjectException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		treeWalk.close();
	}
*/

}
