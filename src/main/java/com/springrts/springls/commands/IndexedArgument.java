/*
	Copyright (c) 2015 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.commands;

import com.springrts.springls.util.Parser;


/**
 * Describes a single indexed (word or sentence) argument of a command.
 * This is pretty much a reflection of the lobby protocol definition.
 * @author hoijui
 */
public interface IndexedArgument {

	/**
	 * Returns the name of the argument.
	 * This should be a valid variable name matching "^[_a-zA-Z0-9]+$" (regex).
	 * @return the case sensitive name
	 */
	String getName();

	/**
	 * Returns the type of the value of the argument.
	 * Examples: String.class, Float.class, Integer.class
	 * @return the value types class
	 */
	Class getValueType();

	/**
	 * Indicates whether this is an optional or a required argument.
	 * @return true, if this is an optional argument, false if it is required
	 */
	boolean isOptional();

	/**
	 * Process one call of the command.
	 * This is invoked whenever a command with the name specified
	 * in <code>SupportedCommand</code> is received from a client.
	 * @param args arguments to the command, this does not include the command
	 *   name its self
	 * @return true, if the command was valid and successfully executed
	 */
	<T> Parser<String, T> getValueParser();
}
