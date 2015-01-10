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
import com.springrts.springls.ServerConfiguration;
import com.springrts.springls.ServerNotification;
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;
import java.util.Collections;

/**
 * Allows a user to change his username.
 * @author hoijui
 */
@SupportedCommand("RENAMEACCOUNT")
public class RenameAccountCommandProcessor extends AbstractCommandProcessor {

	public RenameAccountCommandProcessor() {
		super(
				new CommandArguments(
						Collections.EMPTY_LIST,
						new Argument("newUsername")),
				Account.Access.NORMAL);
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		final String newUsername = (String)args.getSentences().get(0);

		if (getConfiguration().getBoolean(ServerConfiguration.LAN_MODE)) {
			processingError(client, String.format(
					"%s failed: You cannot rename your account while"
					+ " the server is running in LAN mode, since you have no"
					+ " persistent account!", getCommandName()));
			return;
		}

		// validate new user name
		final String valid = Account.isOldUsernameValid(newUsername);
		if (valid != null) {
			processingError(client, String.format(
					"%s failed: Invalid username (reason: %s)",
					getCommandName(), valid));
			return;
		}

		final Account account = getContext().getAccountsService().findAccountNoCase(newUsername);
		if ((account != null) && (account != client.getAccount())) {
			processingError(client, String.format(
					"%s failed: Account with same username already exists!",
					getCommandName()));
			return;
		}

		final String oldName = client.getAccount().getName();
		final Account accountNew = client.getAccount().clone();
		accountNew.setName(newUsername);
		accountNew.setLastLogin(System.currentTimeMillis());
		accountNew.setLastIp(client.getIp());
		final boolean mergeOk = getContext().getAccountsService()
				.mergeAccountChanges(accountNew, client.getAccount().getName());
		if (mergeOk) {
			client.setAccount(accountNew);
		} else {
			processingError(client, "Your account renaming failed.");
			return;
		}

		// make sure all mutes are accordingly adjusted to the new userName:
		for (int i = 0; i < getContext().getChannels().getChannelsSize(); i++) {
			getContext().getChannels().getChannel(i).getMuteList().rename(
					client.getAccount().getName(), newUsername);
		}

		client.sendLine(String.format(
				"SERVERMSG Your account has been renamed to <%s>."
				+ " Reconnect with the new account."
				+ " You will now be automatically disconnected!",
				accountNew.getName()));
		getContext().getClients().killClient(client, "Quit: renaming account");
		// let's save new accounts info to disc
		getContext().getAccountsService().saveAccounts(false);
		getContext().getClients().sendToAllAdministrators(String.format(
				"SERVERMSG [broadcast to all admins]:"
				+ " User <%s> has just renamed his account to <%s>",
				oldName, client.getAccount().getName()));

		// add server notification:
		final ServerNotification srvNotif
				= new ServerNotification("Account renamed");
		srvNotif.addLine(String.format(
				"User <%s> has renamed his account to <%s>",
				oldName, client.getAccount().getName()));
		getContext().getServerNotifications().addNotification(srvNotif);
	}
}
