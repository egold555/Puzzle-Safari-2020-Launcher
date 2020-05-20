package org.to2mbn.jmccc.mcdownloader.provider;

import java.io.File;
import java.net.URI;
import org.to2mbn.jmccc.mcdownloader.download.tasks.DownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.version.Library;

class JarLibraryDownloadHandler implements LibraryDownloadHandler {

	@Override
	public DownloadTask<Void> createDownloadTask(File target, Library library, URI libraryUri) {
		return new FileDownloadTask(libraryUri, target);
	}

}
