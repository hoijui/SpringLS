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

/**
 * A Command processor is responsible for handling a single type of command,
 * specified by {@link SupportedCommand}.
 * @author hoijui
 */
public interface CommandProcessor extends ContextReceiver {

	/**
	 * A property for the name of the shape.
	 */
	String NAME_PROPERTY = "Command-Name";

	/**
	 * Returns a description of the arguments supported by this command.
	 * @return supported arguments constellation
	 */
	CommandArguments getArguments();

//	/**
//	 * Returns a handler for parsing exceptions.
//	 * It will be used for handling command arguments parsing
//	 * and command handling exceptions.
//	 * @return a handler, or <code>null</code>, if the default handler
//	 *   should be used
//	 */
//	CommandParsingExceptionListener getExceptionHandler();
//
//	/**
//	 * Parses the commands arguments from a command string.
//	 * @return the commands parsed arguments,
//	 *   or <code>null</code> in case of an error
//	 */
//	ParsedCommandArguments parseArguments(
//			Client client,
//			String command,
//			int argsStartIndex);

	/**
	 * Process one call of the command.
	 * This is invoked whenever a command with the name specified
	 * in <code>SupportedCommand</code> is received from a client.
//	 * @param args arguments to the command, this does not include the command
//	 *   name its self
	 * @param commandClean the plain text command to be executed for the client,
	 *   already trimmed
	 * @param argsStartIndex the starting index of arguments in commandClean,
	 *   or -1, if there are none
	 * @return true, if the command was valid and successfully executed
	 */
	void process(Client client, String commandClean, int argsStartIndex)
			throws CommandProcessingException;
}
