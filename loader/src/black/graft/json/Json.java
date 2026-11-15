package black.graft.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import black.graft.json.Json.JsonArray;
import black.graft.json.Json.JsonBoolean;
import black.graft.json.Json.JsonException;
import black.graft.json.Json.JsonNull;
import black.graft.json.Json.JsonNumber;
import black.graft.json.Json.JsonObject;
import black.graft.json.Json.JsonString;

/**
 * A strict JSON reader, for the few Mojang files Graft reads: {@code version.json}, version manifests and asset indexes.
 * It accepts exactly RFC 8259 and nothing else, so a truncated or tampered file fails here, with the offset, instead of
 * further on. Internal: not part of Graft's API.
 */
public sealed interface Json {
	/** A JSON object. Keys keep the order they were written in. */
	record JsonObject(Map<String, Json> members) implements Json {
		public JsonObject {
			members = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(members));
		}

		/** The member called {@code key}, if there is one. */
		public Optional<Json> member(String key) {
			return Optional.ofNullable(members.get(key));
		}

		/** The string member {@code key}; fails if it's missing or isn't a string. */
		public String string(String key) {
			return required(key, JsonString.class).value();
		}

		/** The object member {@code key}; fails if it's missing or isn't an object. */
		public JsonObject object(String key) {
			return required(key, JsonObject.class);
		}

		/** The array member {@code key}; fails if it's missing or isn't an array. */
		public JsonArray array(String key) {
			return required(key, JsonArray.class);
		}

		/** The number member {@code key}; fails if it's missing or isn't a number. */
		public BigDecimal number(String key) {
			return required(key, JsonNumber.class).value();
		}

		private <T extends Json> T required(String key, Class<T> type) {
			Json value = members.get(key);
			if (value == null) {
				throw new JsonException("missing member \"" + key + "\"; members are " + members.keySet());
			}
			if (!type.isInstance(value)) {
				throw new JsonException("member \"" + key + "\" is " + value.kind() + ", expected " + kindOf(type));
			}
			return type.cast(value);
		}
	}

	/** A JSON array. */
	record JsonArray(List<Json> elements) implements Json {
		public JsonArray {
			elements = List.copyOf(elements);
		}

		/** The elements, each required to be an object. */
		public List<JsonObject> objects() {
			List<JsonObject> objects = new ArrayList<>();
			for (int i = 0; i < elements.size(); i++) {
				if (!(elements.get(i) instanceof JsonObject object)) {
					throw new JsonException("element " + i + " is " + elements.get(i).kind() + ", expected an object");
				}
				objects.add(object);
			}
			return objects;
		}
	}

	/** A JSON string. */
	record JsonString(String value) implements Json {}

	/** A JSON number, kept exact. */
	record JsonNumber(BigDecimal value) implements Json {}

	/** {@code true} or {@code false}. */
	record JsonBoolean(boolean value) implements Json {}

	/** JSON's {@code null}, as a value rather than a Java null. */
	record JsonNull() implements Json {}

	/** A value that isn't valid JSON, or doesn't have the shape the reader expected. */
	final class JsonException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public JsonException(String message) {
			super(message);
		}
	}

	/** Reads one JSON value that makes up the whole of {@code text}, surrounding whitespace aside. */
	static Json parse(String text) {
		Reader reader = new Reader(text);
		Json value = reader.value();
		reader.skipWhitespace();
		if (!reader.atEnd()) {
			throw reader.error("unexpected text after the value");
		}
		return value;
	}

	/** Reads {@code text}, which must be a JSON object. */
	static JsonObject parseObject(String text) {
		if (!(parse(text) instanceof JsonObject object)) {
			throw new JsonException("expected a JSON object at the top level");
		}
		return object;
	}

	/** What kind of value this is, for error messages: "an object", "a string" and so on. */
	default String kind() {
		return kindOf(getClass());
	}

	private static String kindOf(Class<?> type) {
		if (type == JsonObject.class) { return "an object"; }
		if (type == JsonArray.class) { return "an array"; }
		if (type == JsonString.class) { return "a string"; }
		if (type == JsonNumber.class) { return "a number"; }
		if (type == JsonBoolean.class) { return "a boolean"; }
		return Reader.NULL;
	}

}

/** The parser's cursor over the text. */
final class Reader {
	static final String TRUE = "true";
	static final String FALSE = "false";
	static final String NULL = "null";
	private static final String HEX_ESCAPE = "a \\u escape needs four hex digits";
	private static final int MAX_DEPTH = 512;
	private final String text;
	private int at;
	private int depth;

	Reader(String text) {
		this.text = text;
	}

	Json value() {
		skipWhitespace();
		if (atEnd()) {
			throw error("expected a value, found the end of the text");
		}
		char c = text.charAt(at);
		return switch (c) {
			case '{' -> object();
			case '[' -> array();
			case '"' -> new JsonString(string());
			case 't' -> literal(TRUE, new JsonBoolean(true));
			case 'f' -> literal(FALSE, new JsonBoolean(false));
			case 'n' -> literal(NULL, new JsonNull());
			default -> {
				if (c == '-' || (c >= '0' && c <= '9')) {
					yield number();
				}
				throw error("unexpected character '" + c + "'");
			}
		};
	}

	private JsonObject object() {
		enter();
		at++; // {
		Map<String, Json> members = new LinkedHashMap<>();
		skipWhitespace();
		if (peek('}')) {
			at++;
			depth--;
			return new JsonObject(members);
		}
		while (true) {
			skipWhitespace();
			if (!peek('"')) {
				throw error("expected a member name in quotes");
			}
			String key = string();
			if (members.containsKey(key)) {
				throw error("member \"" + key + "\" appears twice");
			}
			skipWhitespace();
			expect(':');
			members.put(key, value());
			skipWhitespace();
			if (peek(',')) {
				at++;
			} else if (peek('}')) {
				at++;
				depth--;
				return new JsonObject(members);
			} else {
				throw error("expected ',' or '}' in an object");
			}
		}
	}

	private JsonArray array() {
		enter();
		at++; // [
		List<Json> elements = new ArrayList<>();
		skipWhitespace();
		if (peek(']')) {
			at++;
			depth--;
			return new JsonArray(elements);
		}
		while (true) {
			elements.add(value());
			skipWhitespace();
			if (peek(',')) {
				at++;
			} else if (peek(']')) {
				at++;
				depth--;
				return new JsonArray(elements);
			} else {
				throw error("expected ',' or ']' in an array");
			}
		}
	}

	private String string() {
		at++; // opening quote
		StringBuilder out = new StringBuilder();
		while (true) {
			if (atEnd()) {
				throw error("a string isn't closed");
			}
			char c = text.charAt(at++);
			if (c == '"') {
				return out.toString();
			}
			if (c < 0x20) {
				throw error("a control character inside a string must be escaped");
			}
			if (c != '\\') {
				out.append(c);
				continue;
			}
			if (atEnd()) {
				throw error("a string ends inside an escape");
			}
			char e = text.charAt(at++);
			switch (e) {
				case '"' -> out.append('"');
				case '\\' -> out.append('\\');
				case '/' -> out.append('/');
				case 'b' -> out.append('\b');
				case 'f' -> out.append('\f');
				case 'n' -> out.append('\n');
				case 'r' -> out.append('\r');
				case 't' -> out.append('\t');
				case 'u' -> out.append(unicodeEscape());
				default -> throw error("unknown escape '\\" + e + "'");
			}
		}
	}

	private char unicodeEscape() {
		if (at + 4 > text.length()) {
			throw error(HEX_ESCAPE);
		}
		int code = 0;
		for (int i = 0; i < 4; i++) {
			int digit = Character.digit(text.charAt(at++), 16);
			if (digit < 0) {
				throw error(HEX_ESCAPE);
			}
			code = code * 16 + digit;
		}
		return (char) code;
	}

	private JsonNumber number() {
		int start = at;
		if (peek('-')) {
			at++;
		}
		if (peek('0')) {
			at++;
		} else if (!digits()) {
			throw error("a number needs digits");
		}
		if (peek('.')) {
			at++;
			if (!digits()) {
				throw error("a number needs digits after '.'");
			}
		}
		if (peek('e') || peek('E')) {
			at++;
			if (peek('+') || peek('-')) {
				at++;
			}
			if (!digits()) {
				throw error("a number needs digits in its exponent");
			}
		}
		return new JsonNumber(new BigDecimal(text.substring(start, at)));
	}

	private boolean digits() {
		int start = at;
		while (!atEnd() && text.charAt(at) >= '0' && text.charAt(at) <= '9') {
			at++;
		}
		return at > start;
	}

	private Json literal(String word, Json value) {
		if (!text.startsWith(word, at)) {
			throw error("expected " + word);
		}
		at += word.length();
		return value;
	}

	private void enter() {
		if (++depth > MAX_DEPTH) {
			throw error("nested deeper than " + MAX_DEPTH);
		}
	}

	private void expect(char c) {
		if (!peek(c)) {
			throw error("expected '" + c + "'");
		}
		at++;
	}

	private boolean peek(char c) {
		return !atEnd() && text.charAt(at) == c;
	}

	void skipWhitespace() {
		while (!atEnd()) {
			char c = text.charAt(at);
			if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
				return;
			}
			at++;
		}
	}

	boolean atEnd() {
		return at >= text.length();
	}

	JsonException error(String what) {
		return new JsonException("invalid JSON at offset " + at + ": " + what);
	}
}
