package SignageUpdate;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ListedFileFilter extends FileFilter {

	String[] acceptedFilenames;
	String filterDescription = "Accepts only filenames (in any directory) from a given list";
	
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
			return true;
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
