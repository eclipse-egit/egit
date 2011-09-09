package org.eclipse.egit.ui.internal.clone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.transport.URIish;

/**
 * @author Qiangsheng Wang(wangqs_eclipse@yahoo.com)
 */
public class SimpleRpositoryURLFileParser {
	/**
	 * @param path
	 * @return urls
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public List<String> parse(String path) throws IOException, URISyntaxException {
		ArrayList<String> urls = new ArrayList<String>();
		File file = new File(path);
		if (file.exists() && file.isFile()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));
			String line;
			while ((line = reader.readLine()) != null) {
				// Try to new an URL
				URIish uri = new URIish(line);
				if (uri.isRemote()) {
					urls.add(line);
				}
			}
		}
		return urls;
	}

}
