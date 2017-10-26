package SignageUpdate;

import java.io.File;

public class PrefixedExtensionFileFilter extends ExtensionFileFilter {
	private String filterPrefix;

	public PrefixedExtensionFileFilter(String description, String prefix, String extension) {
		super(description, extension);
		filterPrefix = prefix;
	}

	public PrefixedExtensionFileFilter(String description, String prefix, String[] extensions) {
		super(description, extensions);
		filterPrefix = prefix;
	}

	/* (non-Javadoc)
	 * @see SignageUpdate.ExtensionFileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File file) {
		boolean result = super.accept(file);
		if (!result) {
			return result;
		}
		
		// add test for prefix
		try {
			String filename = file.getName();
			result &= filename.startsWith(filterPrefix);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}
}
