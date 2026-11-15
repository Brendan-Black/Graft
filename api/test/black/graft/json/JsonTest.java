package black.graft.json;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;

import java.math.BigDecimal;
import java.util.List;

import black.graft.json.Json.JsonArray;
import black.graft.json.Json.JsonException;
import black.graft.json.Json.JsonObject;

public final class JsonTest {
	private JsonTest() {}

	public static void run() {
		JsonObject v = Json.parseObject("""
			{"id": "26.3", "n": -1.5e2, "ok": true, "none": null,
			 "list": [1, "two", {"three": 3}, []], "esc": "a\\"b\\\\c\\/d\\n\\u00e9"}""");
		eq("26.3", v.string("id"), "a string member");
		eq(new BigDecimal("-1.5e2"), v.number("n"), "a number, exactly");
		eq(new Json.JsonBoolean(true), v.member("ok").orElseThrow(), "a boolean");
		eq(new Json.JsonNull(), v.member("none").orElseThrow(), "null is a value, not a Java null");
		ok(v.member("missing").isEmpty(), "a missing member is empty");
		JsonArray list = v.array("list");
		eq(4, list.elements().size(), "array elements");
		eq("a\"b\\c/d\né", v.string("esc"), "escapes, including \\u");
		eq(List.of("id", "n", "ok", "none", "list", "esc"), List.copyOf(v.members().keySet()), "members keep their order");

		ok(fails(JsonException.class, () -> v.string("missing"), "missing").getMessage().contains("missing member \"missing\""),
			"a required member that's missing is named");
		ok(fails(JsonException.class, () -> v.string("n"), "wrong type").getMessage().contains("is a number, expected a string"),
			"a member of the wrong kind says both kinds");
		fails(JsonException.class, () -> list.objects(), "objects() refuses an array with non-objects");

		for (String bad : List.of("", "{", "{\"a\":1,}", "[1,]", "{\"a\" 1}", "{\"a\":1,\"a\":2}", "\"unterminated", "\"tab\there\"",
			"01", "1.", "-", "1e", "tru", "nul", "[1] x", "{'a':1}", "\"\\x\"", "\"\\u12\"", "[".repeat(600) + "]".repeat(600))) {
			fails(JsonException.class, () -> Json.parse(bad), "invalid JSON is refused: " + (bad.length() > 20 ? bad.substring(0, 20) + "..." : bad));
		}
		ok(fails(JsonException.class, () -> Json.parse("[1, 2,, 3]"), "offset").getMessage().contains("offset 6"),
			"the message gives the offset");
		fails(JsonException.class, () -> Json.parseObject("[1]"), "parseObject wants an object");
		eq(new Json.JsonString("x"), Json.parse("  \"x\"\n"), "whitespace around the value is fine");
	}
}
