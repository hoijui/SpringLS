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
import com.springrts.springls.Battle;
import com.springrts.springls.Client;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.List;

/**
 * Sent by founder of the battle to server telling him he enabled one or more
 * previous disabled units.
 * At least one unit name must be passed as an argument.
 * @author hoijui
 */
@SupportedCommand("ENABLEUNITS")
public class EnableUnitsCommandProcessor extends AbstractCommandProcessor {

	public EnableUnitsCommandProcessor() {
		super(
				new CommandArguments(
						true,
						new Argument("unitName")),
				Account.Access.NORMAL,
				true,
				true); // only the founder can
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final Battle battle = getBattle(client);

		final List<String> unitNames = (List<String>)args.getWords();
		for (final String unit : unitNames) {
			// will ignore it if string is not found in the list
			battle.getDisabledUnits().remove(unit);
		}

		battle.sendToAllExceptFounder(args.getFullCommand());
	}
}
