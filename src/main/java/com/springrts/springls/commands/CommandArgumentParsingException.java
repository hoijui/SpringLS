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


/**
 * TODO add class description here
 * @author hoijui
 */
public class CommandArgumentParsingException extends CommandParsingException {

	private final IndexedArgument argument;

	public CommandArgumentParsingException(
			final String commandName,
			final IndexedArgument argument,
			final String message)
	{
		super(commandName, message);

		this.argument = argument;
	}

	public CommandArgumentParsingException(
			final String commandName,
			final IndexedArgument argument,
			final String message,
			final Throwable cause)
	{
		super(commandName, message, cause);

		this.argument = argument;
	}

	public IndexedArgument getArgument() {
		return argument;
	}
}
