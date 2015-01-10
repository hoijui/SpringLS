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
import com.springrts.springls.commands.AbstractCommandProcessor;
import com.springrts.springls.commands.Argument;
import com.springrts.springls.commands.CommandArguments;
import com.springrts.springls.commands.CommandProcessingException;
import com.springrts.springls.commands.ParsedCommandArguments;
import com.springrts.springls.commands.SupportedCommand;

/**
 * @author hoijui
 */
@SupportedCommand("REGISTER")
public class RegisterCommandProcessor extends AbstractCommandProcessor {

	public RegisterCommandProcessor() {
		super(
				new CommandArguments(
						new Argument("username"),
						new Argument("password")),
				Account.Access.NONE);
		setToClientErrorCommandName("REGISTRATIONDENIED");
	}

	@Override
	public void process(
			final Client client,
			final ParsedCommandArguments args)
			throws CommandProcessingException
	{
		if (!getContext().getAccountsService().isRegistrationEnabled()) {
			processingError(client, "Sorry, account registration is currently disabled");
			return;
		}

		if (client.getAccount().getAccess() != Account.Access.NONE) {
			// only clients which are not logged-in can register
			processingError(client, "You are already logged-in,"
					+ " no need to register a new account");
			return;
		}

		if (getConfiguration().getBoolean(ServerConfiguration.LAN_MODE)) {
			// no need to register an account in LAN mode, since it accepts any
			// userName
			processingError(client, "Can not register in LAN-mode."
					+ " Login with any username and password to proceed");
			return;
		}

		final String username = (String)args.getWords().get(0);
		final String password = (String)args.getWords().get(1);

		// validate userName:
		String valid = Account.isOldUsernameValid(username);
		if (valid != null) {
			processingError(client, String.format("Invalid username (reason: %s)", valid));
			return;
		}

		// validate password:
		valid = Account.isPasswordValid(password);
		if (valid != null) {
			processingError(client, String.format("Invalid password (reason: %s)", valid));
			return;
		}
		Account account = getContext().getAccountsService()
				.findAccountNoCase(username);
		if (account != null) {
			processingError(client, "Account already exists");
			return;
		}

		// check for reserved names:
		if (Account.RESERVED_NAMES.contains(username)) {
			processingError(client, "Invalid account name - you are"
					+ " trying to register a reserved account name");
			return;
		}
		/*if (!getContext().whiteList.contains(client.getIp())) {
			if (registrationTimes.containsKey(client.ip)
			&& (int)(registrationTimes.get(client.ip)) + 3600 > (System.currentTimeMillis()/1000)) {
			client.sendLine("REGISTRATIONDENIED This ip has already registered an account recently");
			context.getClients().sendToAllAdministrators("SERVERMSG Client at " + client.ip + "'s registration of " + username + " was blocked due to register spam");
			return false;
			}
			registrationTimes.put(client.ip, (int)(System.currentTimeMillis()/1000));*/
			/*String proxyDNS = "dnsbl.dronebl.org"; //Bot checks this with the broadcast, no waiting for a response
			String[] ipChunks = client.ip.split("\\.");
			for (int i = 0; i < 4; i++) {
			proxyDNS = ipChunks[i] + "." + proxyDNS;
			}
			try {
			InetAddress.getByName(proxyDNS);
			client.sendLine("REGISTRATIONDENIED Using a known proxy ip");
			context.getClients().sendToAllAdministrators("SERVERMSG Client at " + client.ip + "'s registration of " + username + " was blocked as it is a proxy ip");
			return false;
			} catch (final UnknownHostException ex) {
			}
		}*/
		getContext().getClients().sendToAllAdministrators(String.format(
				"SERVERMSG New registration of <%s> at %s", username,
				client.getIp().getHostAddress()));
		account = new Account(username, password, client.getIp(),
				client.getCountry());
		getContext().getAccountsService().addAccount(account);

		// let's save new accounts info to disk
		getContext().getAccountsService().saveAccounts(false);

		client.sendLine("REGISTRATIONACCEPTED");
	}
}
