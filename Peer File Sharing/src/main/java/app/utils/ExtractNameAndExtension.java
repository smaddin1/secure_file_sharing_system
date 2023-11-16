package app.utils;

/**
 * The ExtractNameAndExtension class is used to extract the name and extension from a given file name.
 * It assumes that the file name is in the format of 'name.extension'. If no extension is present,
 * the extension is set to an empty string.
 */
public class ExtractNameAndExtension {
    /**
     * The full file name, including its extension.
     */
    private String absoluteFileName;

    /**
     * The name of the file without the extension.
     */
    private String fileName;

    /**
     * The extension of the file.
     */
    private String extension;

    /**
     * Constructor that takes the full file name as input.
     *
     * @param fileName The full name of the file, including the extension.
     */
    public ExtractNameAndExtension(String fileName) {
        this.absoluteFileName = fileName;
    }

    /**
     * Retrieves the file name without the extension.
     *
     * @return The file name without the extension.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Retrieves the file extension.
     *
     * @return The file extension, or an empty string if no extension is present.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Processes the absolute file name to extract the name and extension.
     * If there is no extension, the extension is set to an empty string.
     */
    public void run() {
        int lastDotIndex = this.absoluteFileName.lastIndexOf(".");

        if (lastDotIndex != -1) {
            this.fileName = absoluteFileName.substring(0, lastDotIndex);
            this.extension = absoluteFileName.substring(lastDotIndex + 1);
        } else {
            this.fileName = absoluteFileName;
            this.extension = "";
        }
    }
}
