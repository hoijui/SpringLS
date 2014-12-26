/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

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


import java.awt.Color;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import net.iharder.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for spring lobby protocol related stuff, mostly conversion.
 * @author hoijui
 */
public final class ProtocolUtil {

	public static final String COUNTRY_UNKNOWN = "XX";

	private static final Logger LOG = LoggerFactory.getLogger(ProtocolUtil.class);

	private ProtocolUtil() {}

	/**
	 * Converts a boolean to a number, according to the lobby protocol standard.
	 * @param aBoolean to be converted into a numerical representation
	 * @return <tt>1</tt> if <tt>aBoolean</tt> is <tt>true</tt>,
	 *   <tt>false</tt> otherwise
	 * @see #numberToBool(byte)
	 */
	public static byte boolToNumber(final boolean aBoolean) {
		return (aBoolean ? ((byte) 1) : ((byte) 0));
	}

	/**
	 * Converts a number to a boolean, according to the lobby protocol standard.
	 * @param aNumber to be converted into a boolean
	 * @return <tt>true</tt> if <tt>aNumber == 1</tt>, <tt>false</tt> otherwise
	 * @see #boolToNumber(boolean)
	 */
	public static boolean numberToBool(final byte aNumber) {
		return (aNumber == 1);
	}

	// TODO why long (64bit) and not int (32bit)? (IP v6 woudl be 128bit)
	/**
	 * Converts an IP v4 number to a 64bit (long) number, according to the lobby
	 * protocol standard.
	 * @param ipAddress an IP v4 (<tt>Inet4Address</tt>)
	 * @return a 64 bit number representing the supplied IP
	 */
	public static long ip2Long(final InetAddress ipAddress) {

		long res;

		final byte[] addr = ipAddress.getAddress();
		final long field1 = (long) addr[0] << 24; // highest order byte
		final long field2 = (long) addr[1] << 16;
		final long field3 = (long) addr[2] << 8;
		final long field4 = (long) addr[3]; // lowest order byte
		res = field1 + field2 + field3 + field4;

		return res;
	}

	/**
	 * This method encodes plain-text passwords to MD5 hashed ones in base-64
	 * form.
	 */
	public static String encodePassword(final String plainPassword) {

		String encodedPassword;

		try {
			final byte[] md5Digest = Misc.getMD5(plainPassword);
			encodedPassword = Base64.encodeBytes(md5Digest);
		} catch (final NoSuchAlgorithmException ex) {
			LOG.error("Failed to encode password", ex);
			encodedPassword = null;
		}

		return encodedPassword;
	}

	/**
	 * @see #colorSpringToJava(int)
	 */
	public static Color colorSpringStringToJava(final String springColor) {

		Color color = null;

		try {
			color = colorSpringToJava(Integer.parseInt(springColor));
		} catch (final NumberFormatException ex) {
			LOG.debug("Invalid Spring color format number", ex);
		}

		return color;
	}

	/**
	 * This can be used for converting a lobby protocol color into a java color.
	 * See the myteamcolor argument of the MYBATTLESTATUS command for an
	 * example.
	 * Should be 32-bit signed integer in decimal form (e.g. 255 and not FF)
	 * where each color channel should occupy 1 byte (e.g. in hexadecimal:
	 * "00BBGGRR", B = blue, G = green, R = red).
	 * Example: 255 stands for "000000FF".
	 * @see #colorJavaToSpring(Color)
	 */
	public static Color colorSpringToJava(final int springColor) {

		final int red   = springColor       & 255;
		final int green = springColor >> 8  & 255;
		final int blue  = springColor >> 16 & 255;
//		int alpha = springColor >> 24 & 255;
		final Color color = new Color(red, green, blue/*, alpha*/);

		return color;
	}

	/**
	 * This can be used for converting a java color into a lobby protocol color.
	 * @see #colorSpringToJava(int)
	 */
	public static int colorJavaToSpring(final Color color) {

		int springColor = 0;

//		springColor += color.getAlpha() << 24;
		springColor += color.getBlue()  << 16;
		springColor += color.getGreen() << 8;
		springColor += color.getRed();

		return springColor;
	}

	public static Locale countryToLocale(final String country) {

		final String isoCountry = country.equals(COUNTRY_UNKNOWN) ? ""
				: country;
		return new Locale("", isoCountry);
	}
}
