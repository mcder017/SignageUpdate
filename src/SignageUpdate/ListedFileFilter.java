package SignageUpdate;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ListedFileFilter extends FileFilter {

	boolean allowDirectorySearch = true;
	String[] acceptedFilenames;
	String filterDescription = "Accepts only filenames (in any directory) from a given list";
	
	public ListedFileFilter(String description, String[] filenames, boolean allowDirSearch) {
		acceptedFilenames = filenames;
		filterDescription = description;
		allowDirectorySearch = allowDirSearch;
	}

	public ListedFileFilter(String description, String[] filenames) {
		acceptedFilenames = filenames;
		filterDescription = description;
	}

	public ListedFileFilter(String[] filenames) {
		acceptedFilenames = filenames;
	}

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return allowDirectorySearch;
		}
		
		for (String okName : acceptedFilenames) {
			if (okName.equalsIgnoreCase(f.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		return filterDescription;
	}

}
