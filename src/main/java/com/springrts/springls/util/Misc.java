/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls.util;


import java.net.UnknownHostException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Miscellaneous utility functions.
 * @author Betalord
 * @author hoijui
 */
public final class Misc {

	private static final Logger LOG = LoggerFactory.getLogger(Misc.class);

	public static final String EOL = "\n";

	public static final String MAVEN_GROUP_ID = "com.springrts.springls";
	public static final String MAVEN_ARTIFACT_ID = "springls";

	public static final String UNKNOWN_VERSION = "<unknown-version>";

	private static final Pattern PATTERN_IP_V4 = Pattern.compile("^[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}$");
	private static final Pattern PATTERN_IP_V6 = Pattern.compile("^[0-9a-fA-F.:]+$");

	private static Properties mavenProperties = null;
	private static final Semaphore MAVEN_PROPERTIES_INIT = new Semaphore(1);

	/** Make clear that this is an utility class */
	private Misc() {}

	/**
	 * Concatenates a list of strings together, starting at a certain index,
	 * using a single space character as separator.
	 * See
	 * <a href="http://leepoint.net/notes-java/data/strings/96string_examples/example_arrayToString.html">
	 * this document</a> on why <code>StringBuilder</code> is faster.
	 */
	public static String makeSentence(final String[] words, final int startIndex) {

		final StringBuilder result = new StringBuilder();

		if (startIndex < words.length) {
			result.append(words[startIndex]);
			for (int i = startIndex + 1; i < words.length; i++) {
				result.append(' ').append(words[i]);
			}
		}

		return result.toString();
	}
	/**
	 * Concatenates a list of strings together, using a single space character
	 * as separator.
	 * This has the same effect like <code>makeSentence(args, 0)</code>.
	 */
	public static String makeSentence(final String[] words) {
		return makeSentence(words, 0);
	}
	/**
	 * Concatenates a list of strings together, using a single space character
	 * as separator.
	 * @see #makeSentence(String[], int)
	 */
	public static String makeSentence(final List<String> words, final int startIndex) {

		final StringBuilder res = new StringBuilder();

		if (startIndex < words.size()) {
			res.append(words.get(startIndex));
			for (int i = startIndex + 1; i < words.size(); i++) {
				res.append(' ');
				res.append(words.get(i));
			}
		}

		return res.toString();
	}

	/**
	 * This method will return the local IP address, such as "192.168.1.100"
	 * instead of "127.0.0.1".
	 * See also
	 * <a href="http://forum.java.sun.com/thread.jspa?threadID=619056&messageID=3477258">
	 * this related post</a>.
	 */
	public static InetAddress getLocalIpAddress() {

		InetAddress localIpAddress = null;

		try {
			final Enumeration<NetworkInterface> netfaces = NetworkInterface.getNetworkInterfaces();

			while (netfaces.hasMoreElements()) {
				final NetworkInterface netface = netfaces.nextElement();
				final Enumeration<InetAddress> addresses = netface.getInetAddresses();

				while (addresses.hasMoreElements()) {
					final InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') == -1) {
						localIpAddress = address;
					}
				}
			}
		} catch (final SocketException sex) {
			LOG.trace("Failed evaluating the local IP address", sex);
			localIpAddress = null;
		}

		return localIpAddress;
	}

	/**
	 * Converts time (in milliseconds) to a string like this:
	 * "{dd} days, {hh} hours and {mm} minutes"
	 */
	public static String timeToDHM(final long duration) {

		long remainingTime = duration;

		final long days = remainingTime / (1000 * 60 * 60 * 24);
		remainingTime -= days * (1000 * 60 * 60 * 24);

		final long hours = remainingTime / (1000 * 60 * 60);
		remainingTime -= hours * (1000 * 60 * 60);

		final long minutes = remainingTime / (1000 * 60);

		return String.format("%d days, %d hours and %d minutes",
				days, hours, minutes);
	}

	/**
	 * Reallocates an array with a new size, and copies the contents
	 * of the old array to the new array.
	 * See also <a href="http://www.source-code.biz/snippets/java/3.htm">
	 * this related code snipped</a>.
	 * @param oldArray  the old array, to be reallocated.
	 * @param newSize   the new array size.
	 * @return A new array with the same contents.
	 */
	public static Object[] resizeArray(final Object[] oldArray, final int newSize) {

		final int oldSize = java.lang.reflect.Array.getLength(oldArray);
		final Class<?> elementType = oldArray.getClass().getComponentType();
		final Object[] newArray = (Object[]) java.lang.reflect.Array.newInstance(
				elementType, newSize);
		final int preserveLength = Math.min(oldSize, newSize);
		if (preserveLength > 0) {
			System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
		}
		return newArray;
	}

	public static byte[] getMD5(final String plainText) throws NoSuchAlgorithmException {

		final MessageDigest mdAlgorithm = MessageDigest.getInstance("md5");
		mdAlgorithm.update(plainText.getBytes());
		final byte[] md5Digest = mdAlgorithm.digest();

		return md5Digest;
	}

	/**
	 * Downloads a file from the given URL and saves it to disk to specified
	 * file.
	 * @param downloadLimit is specified in bytes per second
	 *   (use 0 for unlimited). This is the maximum rate at which this
	 *   method will attempt to download the file.
	 * Returns number of bytes written if it succeeds.
	 * Original code copied from
	 * <a href="http://schmidt.devlib.org/java/file-download.html#source">here
	 * </a>
	 */
	public static long download(final String address, final String localFileName, final int downloadLimit) throws IOException {

		long numWritten = 0;
		OutputStream outF = null;
		OutputStream out = null;
		URLConnection conn;
		InputStream in = null;

		// we will regulate download speed within 1 second time frames and always
		// wait after the 1st read of the receive buffer in a new time frame
		// for exactly 1 millisecond. That is neeeded because read() method
		// may return very quickly (in 0 ms, and even multiple times in 0 ms),
		// if we would calculate download speed for such very short time periods
		// we could get enormous data rate caused by dividing something very large
		// with something very small, which we don't want (that's an error) - we
		// also want to avoid division by zero at the same time.

		long lastTimeStamp = System.nanoTime() / 1000000L;
		int bytesSinceLastTimeStamp = 0;

		try {
			final URL url = new URL(address);
			outF = new FileOutputStream(localFileName);
			out = new BufferedOutputStream(outF);
			conn = url.openConnection();
			in = conn.getInputStream();
			final byte[] buffer = new byte[1024];
			int numRead;

			while ((numRead = in.read(buffer)) != -1) {
				// limit download speed:
				if (downloadLimit > 0) {
					bytesSinceLastTimeStamp += numRead;
					final long timeDiff = System.nanoTime() / 1000000 - lastTimeStamp;
					if (timeDiff > 0) { // to avoid division by zero
						final double rate = (double) bytesSinceLastTimeStamp / (double) timeDiff * 1000.0;
						if (rate > downloadLimit) {
							final long sleepTime = Math.round(((double) bytesSinceLastTimeStamp / downloadLimit * 1000.0) - timeDiff);
							try {
								// sleep a bit:
								Thread.sleep(sleepTime);
							} catch (final InterruptedException ie) {
							}
						}

						// check if we must start a new time frame:
						if (timeDiff > 1000) {
							// start new time frame
							lastTimeStamp = System.nanoTime() / 1000000;
							bytesSinceLastTimeStamp = 0;
						}
					} else {
						try {
							// We need this because we do not check time
							// between 0 and 1st millisecond in a time frame,
							// but in the first millisecond, a lot of data
							// may be read from the socket buffer,
							// which we do not want, because we can not regulate
							// download speed accurately in that case.
							Thread.sleep(1);
						} catch (final InterruptedException ie) {
						}
					}
				}

				// write received data to file:
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			} else if (outF != null) {
				outF.close();
			}
		}

		return numWritten;
	}

	/**
	 * @see #download(String, String, int)
	 */
	public static long download(final String address, final String localFileName) throws IOException {
		return download(address, localFileName, 0);
	}

	public static InetAddress parseIp(final String netAddress, final boolean acceptV6, final boolean acceptHostname) {

		InetAddress inetAddress = null;

		try {
			if (!acceptHostname) {
				if (!PATTERN_IP_V6.matcher(netAddress).matches()) {
					throw new IllegalArgumentException("Is neither a v4 nor a v6 IP address");
				} else if (!acceptV6 && !PATTERN_IP_V4.matcher(netAddress).matches()) {
					throw new IllegalArgumentException("Only IP v4 addresses are supported");
				}
			}

			try {
				inetAddress = InetAddress.getByName(netAddress);
			} catch (final UnknownHostException ex) {
				throw new IllegalArgumentException("Failed to resolve hostname to IP", ex);
			}
		} catch (final IllegalArgumentException ex) {
			LOG.trace("Failed parsing IP: {} - {}", netAddress, ex.getMessage());
		}

		return inetAddress;
	}
	public static InetAddress parseIp(final String netAddress) {
		return parseIp(netAddress, false, false);
	}

	private static Properties initMavenProperties() {

		Properties mavenProps = null;
		InputStream propFileIn = null;

		try {
			final String pomPropsLoc = "/META-INF/maven/" + MAVEN_GROUP_ID + "/" + MAVEN_ARTIFACT_ID + "/pom.properties";
			propFileIn = Misc.class.getResourceAsStream(pomPropsLoc);
			if (propFileIn == null) {
				throw new IOException("Failed locating resource in the classpath: " + pomPropsLoc);
			}
			final Properties tmpProps = new Properties();
			tmpProps.load(propFileIn);
			mavenProps = tmpProps;
		} catch (final IOException ex) {
			LOG.warn("Failed reading the Maven properties file", ex);
		} finally {
			if (propFileIn != null) {
				try {
					propFileIn.close();
				} catch (IOException ioex) {
					LOG.warn("Failed closing stream to Maven properties file", ioex);
				}
			}
		}

		return mavenProps;
	}

	/**
	 * Reads this applications Maven properties file in the
	 * META-INF directory of the class-path.
	 */
	public static Properties getMavenProperties() {

		if (mavenProperties == null) {
			try {
				MAVEN_PROPERTIES_INIT.acquire();
				if (mavenProperties == null) {
					mavenProperties = initMavenProperties();
				}
			} catch (InterruptedException ex) {
				// do nothing
			} finally {
				MAVEN_PROPERTIES_INIT.release();
			}
		}

		return mavenProperties;
	}

	/**
	 * Reads this applications version from the Maven properties file in the
	 * META-INF directory.
	 */
	public static String getAppVersion() {

		String appVersion = null;

		final Properties myProperties = getMavenProperties();
		if (myProperties != null) {
			appVersion = myProperties.getProperty("version", null);
		}

		if (appVersion == null) {
			LOG.warn("Failed getting the Applications version from the Maven properties file");
		}

		return appVersion;
	}

	/**
	 * Returns this applications version, or {@link #UNKNOWN_VERSION}, if the
	 * Maven properties can not be read.
	 * @return this applications version, or {@link #UNKNOWN_VERSION}, if the
	 *   Maven properties can not be read.
	 */
	public static String getAppVersionNonNull() {

		String appVersion = getAppVersion();

		if (appVersion == null) {
			appVersion = UNKNOWN_VERSION;
		}

		return appVersion;
	}

	/**
	 * Reads a plain-text file from disc, line by line.
	 * Replaces all line endings with '\n'.
	 * @param file the file to read
	 * @return the content of the file with '\n' line-endings
	 */
	public static String readTextFile(final File file) throws IOException {

		final StringBuilder content = new StringBuilder();
		Reader inF = null;
		BufferedReader in = null;
		try {
			inF = new FileReader(file);
			in = new BufferedReader(inF);
			String line;
			while ((line = in.readLine()) != null) {
				content.append(line).append(EOL);
			}
		} finally {
			try {
				if (in != null) {
					in.close();
				} else if (inF != null) {
					inF.close();
				}
			} catch (final IOException ex) {
				LOG.trace("Failed to close file reader: "
						+ file.getAbsolutePath(), ex);
			}
		}

		return content.toString();
	}

	/**
	 * Utility method used to delete a file or a directory recursively.
	 * @param fileOrDir the file or directory to recursively delete.
	 */
	public static void deleteFileOrDir(final File fileOrDir) throws IOException {

		if (fileOrDir.isDirectory()) {
			final File[] childs = fileOrDir.listFiles();
			for(final File child : childs){
				deleteFileOrDir(child);
			}
		}
		if (!fileOrDir.delete()) {
			throw new IOException("Failed deleting a file/directory: "
					+ fileOrDir.getCanonicalPath());
		}
	}

	/**
	 * Create a temporary cache directory file descriptor
	 * and makes sure to clean it up on application exit.
	 * This makes sure no file or directory with that name exists,
	 * but the caller still has to create the directory himself.
	 * @param dirName base name of the directory descriptor to create
	 * @return a file descriptor to a non existing file/directory.
	 */
	public static File createTempDir(final String dirName) throws IOException {

		final File cacheDir = File.createTempFile(dirName, null);
		// we just created a file, but we actually need a directory
		if (!cacheDir.delete()) {
			throw new IOException("Failed deleting a temporary file: "
					+ cacheDir.getCanonicalPath());
		}
		// delete at application shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(new FileRemover(cacheDir)));

		return cacheDir;
	}

	/**
	 * Removes a file or a directory recursively when run.
	 */
	public static class FileRemover implements Runnable {

		private final File fileOrDir;

		public FileRemover(final File fileOrDir) {
			this.fileOrDir = fileOrDir;
		}

		@Override
		public void run() {

			try {
				Misc.deleteFileOrDir(fileOrDir);
			} catch (final IOException ex) {
				LOG.warn("Failed to delete the temporary directory "
						+ fileOrDir.getAbsolutePath(), ex);
			}
		}
	}
}
