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


import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import java.lang.reflect.Constructor;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all the command processors for a server instance.
 * @see CommandProcessor
 * @author hoijui
 */
public class CommandProcessors implements ContextReceiver {

	private final Logger log  = LoggerFactory.getLogger(CommandProcessors.class);
	private final Map<String, CommandProcessor> cmdNameToProcessor;
	private Context context;
	private CommandProcessorTracker commandProcessorTracker;

	public CommandProcessors() {

		cmdNameToProcessor = new HashMap<String, CommandProcessor>();
		context = null;
		commandProcessorTracker = null;
	}

	/**
	 * Extracts the name of the command supported by a command processor
	 * from its {@link SupportedCommand} annotation.
	 */
	public static String extractCommandName(final Class<? extends CommandProcessor> cmdProcCls) throws IllegalArgumentException {

		final SupportedCommand supCmd = cmdProcCls.getAnnotation(SupportedCommand.class);
		if (supCmd == null) {
			throw new IllegalArgumentException(cmdProcCls.getCanonicalName()
					+ " is not a valid "
					+ CommandProcessor.class.getCanonicalName()
					+ "; @" + SupportedCommand.class.getCanonicalName()
					+ " annotation is missing.");
		}
		if (!supCmd.value().equals(supCmd.value().toUpperCase())) {
			throw new IllegalArgumentException(cmdProcCls.getCanonicalName()
					+ " is not a valid "
					+ CommandProcessor.class.getCanonicalName()
					+ "; The command name has to be upper-case only.");
		}
		final String name = supCmd.value();

		return name;
	}

	@Override
	public void setContext(final Context context) {
		this.context = context;
	}

	/**
	 * Loads the default command processors.
	 */
	public void init() {

		// TODO move this into a BundleActivator.start() method
		commandProcessorTracker = new CommandProcessorTracker(context.getFramework().getBundleContext());
		commandProcessorTracker.open();
		// TODO move this into a BundleActivator.stop() method
//		commandProcessorTracker.close();

		new com.springrts.springls.commands.impl.Activator().start(context.getFramework().getBundleContext());
	}

	/**
	 * Adds a command processor, responsible for handling the given
	 * command-name.
	 * @param commandName the name of the command to handle with the given
	 *   processor
	 * @param commandProcessor the processor to handle the given command-name
	 */
	public void add(final String commandName, final CommandProcessor commandProcessor) {
		cmdNameToProcessor.put(commandName, commandProcessor);
	}

	/**
	 * Removes a command processor, responsible for handling the given
	 * command-name.
	 * @param commandName the name for which to remove the command processor
	 */
	public void remove(final String commandName) {
		cmdNameToProcessor.remove(commandName);
	}

	/**
	 * Returns the command processor responsible for handling the given command.
	 * @param commandName the name of the command to search a processor for
	 * @return the command processor responsible for handling the given command,
	 *   or <code>null</code>, if no suitable one was found
	 */
	public CommandProcessor get(final String commandName) {
		return cmdNameToProcessor.get(commandName);
	}

	/**
	 * Instantiates a single CommandProcessor.
	 * @param cmdProcessorClass the class to instantiate
	 * @throws Exception if loading failed, for whatever reason
	 */
	public static CommandProcessor load(
			final Class<? extends CommandProcessor> cmdProcessorClass)
			throws IllegalArgumentException
	{
		CommandProcessor cmdProcessor = null;

		Constructor<? extends CommandProcessor> noArgsCtor = null;
		try {
			noArgsCtor = cmdProcessorClass.getConstructor();
		} catch (final NoSuchMethodException ex) {
			throw new IllegalArgumentException(
					cmdProcessorClass.getCanonicalName()
					+ " is not a valid CommandProcessor; "
					+ "No-args constructor is missing.",
					ex);
		}
		try {
			cmdProcessor = noArgsCtor.newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new IllegalArgumentException("Failed to instantiate "
					+ cmdProcessorClass.getCanonicalName(), ex);
		}

		return cmdProcessor;
	}

	/**
	 * Adds a command processor.
	 * @param cmdProcessor to be added
	 * @throws Exception if name extraction fails
	 */
	public static void add(
			final BundleContext bundleContext,
			final CommandProcessor cmdProcessor)
	{
		final String cmdName = CommandProcessors.extractCommandName(
				cmdProcessor.getClass());

		cmdProcessor.setContext(Context.getService(bundleContext,
				Context.class));

		final Dictionary dict = new Hashtable();
		dict.put(CommandProcessor.NAME_PROPERTY, cmdName);
		bundleContext.registerService(CommandProcessor.class.getName(),
				cmdProcessor, dict);
	}
}
