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


import com.springrts.springls.commands.SupportedCommand;


/**
 * Sent by any client participating in a battle when he wants to say something
 * in "/me" IRC style.
 * Server can forge this command too (for example when founder of the battle
 * kicks a user, server uses SAYBATTLEEX saying founder kicked a user).
 * @author hoijui
 */
@SupportedCommand("SAYBATTLEEX")
public class SayBattleExCommandProcessor extends SayBattleCommandProcessor {

	public SayBattleExCommandProcessor() {
		super("SAIDBATTLEEX");
	}
}
