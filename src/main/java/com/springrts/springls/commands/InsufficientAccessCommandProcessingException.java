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


import com.springrts.springls.Account;

/**
 * @see CommandProcessor#process(com.springrts.springls.Client, java.util.List)
 * @author hoijui
 */
public class InsufficientAccessCommandProcessingException
		extends CommandProcessingException
{
	public InsufficientAccessCommandProcessingException(
			final String commandName,
			final Account.Access required,
			final Account.Access present)
	{
		super(commandName, String.format(
				"Insufficient access rights; the minimum required is %s, but"
				+ " the account has %s.",
				required,
				present));
	}
}
