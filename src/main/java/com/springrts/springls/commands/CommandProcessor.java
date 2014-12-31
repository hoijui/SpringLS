/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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


import com.springrts.springls.Client;
import com.springrts.springls.ContextReceiver;
import java.util.List;

/**
 * A Command processor is responsible to handle a single type of command,
 * specified by {@link SupportedCommand}.
 * @author hoijui
 */
public interface CommandProcessor extends ContextReceiver {

	/**
	 * A property for the name of the shape.
	 */
	String NAME_PROPERTY = "Command-Name";

	/**
	 * Indicates whether this command uses named or indexed arguments.
	 * Only some of the newer commands use named arguments,
	 * and usually only the ones not used very frequently.
	 * Named arguments cost a bit of extra bandwidth,
	 * but make it easier for protocol changes to be backwards compatible.
	 * For more details about named arguments,
	 * please see the protocol description.
	 * @return true if the command uses named arguments,
	 *   false if it uses indexed arguments
	 */
	boolean isUsingNamedArguments();

	/**
	 * Process one call of the command.
	 * This is invoked whenever a command with the name specified
	 * in <code>SupportedCommand</code> is received from a client.
	 * @param args arguments to the command, this does not include the command
	 *   name its self
	 * @return true, if the command was valid and successfully executed
	 */
	boolean process(Client client, List<String> args)
			throws CommandProcessingException;
}
