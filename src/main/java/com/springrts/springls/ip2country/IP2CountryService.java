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

package com.springrts.springls.ip2country;


import java.net.InetAddress;
import java.util.Locale;

/**
 * @author Betalord
 * @author hoijui
 */
public interface IP2CountryService {

	/**
	 * Converts an IP address into the corresponding country code in the
	 * lobby protocol standard.
	 * @return 2-chars wide country code, as defined in ISO 3166-1 alpha-2,
	 *   or "XX" if the country is unknown.
	 */
	String getCountryCode(InetAddress ip);

	/**
	 * Converts an IP address into a Locale, with the language unspecified.
	 * @see #getCountryCode(InetAddress)
	 */
	Locale getLocale(InetAddress ip);
}
