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

import com.springrts.springls.util.Misc;
import com.springrts.springls.util.Parser;
import com.springrts.springls.util.ProtocolUtil;
import java.awt.Color;
import java.net.InetAddress;


/**
 * Describes a single indexed (word or sentence) argument of a command.
 * This is pretty much a reflection of the lobby protocol definition.
 * @author hoijui
 */
public class Argument implements NamedArgument {

	private static final Parser<String, String> PARSER_NONE
			= new Parser<String, String>() {
		@Override
		public String parse(final String input) {
			return input;
		}
	};

	public static final Parser<String, String> PARSER_TO_UPPER_CASE
			= new Parser<String, String>() {
		@Override
		public String parse(final String input) {
			return input.toUpperCase();
		}
	};

	public static final Parser<String, Integer> PARSER_TO_INTEGER
			= new Parser<String, Integer>() {
		@Override
		public Integer parse(final String input) {
			return Integer.parseInt(input);
		}
	};

	public static final Parser<String, Long> PARSER_TO_LONG
			= new Parser<String, Long>() {
		@Override
		public Long parse(final String input) {
			return Long.parseLong(input);
		}
	};

	public static final Parser<String, Boolean> PARSER_TO_BOOLEAN
			= new Parser<String, Boolean>() {
		@Override
		public Boolean parse(final String input) {
			return ProtocolUtil.numberToBool(Byte.parseByte(input));
		}
	};

	public static final Parser<String, Color> PARSER_TO_COLOR
			= new Parser<String, Color>() {
		@Override
		public Color parse(final String input) {
			return ProtocolUtil.colorSpringStringToJava(input);
		}
	};

	public static final Parser<String, InetAddress> PARSER_TO_INET_ADDRESS
			= new Parser<String, InetAddress>() {
		@Override
		public InetAddress parse(final String input) {
			return Misc.parseIp(input);
		}
	};

	private final String name;
	private final Class valueType;
	private final Parser<String, ?> valueParser;
	private final boolean optional;

	public Argument(
			final String name,
			final Class valueType,
			final Parser<String, ?> valueParser,
			final boolean optional)
	{
		this.name = name;
		this.valueType = valueType;
		this.valueParser = valueParser;
		this.optional = optional;
	}

	public Argument(
			final String name,
			final Class valueType,
			final Parser<String, ?> valueParser)
	{
		this(name, valueType, valueParser, false);
	}

	public Argument(final String name, final boolean optional) {
		this(name, String.class, PARSER_NONE, optional);
	}

	public Argument(final String name) {
		this(name, String.class, PARSER_NONE, false);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class getValueType() {
		return valueType;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	@Override
	public <T> Parser<String, T> getValueParser() {
		return (Parser<String, T>) valueParser;
	}
}
