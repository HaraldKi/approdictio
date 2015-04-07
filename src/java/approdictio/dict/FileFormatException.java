package approdictio.dict;

/**
 * <p>
 * is the exception thrown if a file read does contain format errors.
 * </p>
 */
public class FileFormatException extends Exception {
  private final int lineNo;
  private final String msg;
  private String filename = "stream without name";
  public FileFormatException(String msg, int lineNo, Throwable e) {
    this.msg = msg;
    this.lineNo = lineNo;
    if (e!=null) {
      initCause(e);
    }
  }
  public FileFormatException(String msg, int lineNo) {
    this(msg, lineNo, null);
  }
  public void setFilename(String filename) {
    this.filename = filename;
  }
  /**
   * <p>the number of the line with the error</p>
   * @return the error line number
   */
  public int getLineNo() { return lineNo; }
  public String getFilename() { return filename; }
  @Override
  public String getMessage() {
    return String.format("%s(%s): %s", filename, lineNo, msg);
  }
}
