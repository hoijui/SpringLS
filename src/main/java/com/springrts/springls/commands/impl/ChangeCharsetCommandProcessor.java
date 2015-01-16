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

package com.springrts.springls.commands.impl;


import com.springrts.springls.Account;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Lets an administrator set the server-wide char-set.
 * @author hoijui
 */
@SupportedCommand("CHANGECHARSET")
public class ChangeCharsetCommandProcessor extends AbstractCommandProcessor {

	public ChangeCharsetCommandProcessor() {
		super(
				new CommandArguments(new Argument("charset")),
				Account.Access.ADMIN);
	}

	@Override
	public boolean process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		final String charset = (String)args.getWords().get(0);

		try {
			getContext().getServer().setCharset(charset);
		} catch (final IllegalCharsetNameException ex) {
			client.sendLine("SERVERMSG Error: Illegal charset name: " + charset);
			return false;
		} catch (final UnsupportedCharsetException ex) {
			client.sendLine("SERVERMSG Error: Unsupported charset: " + charset);
			return false;
		}

		client.sendLine("SERVERMSG Charset set to " + charset);

		return true;
	}
}
