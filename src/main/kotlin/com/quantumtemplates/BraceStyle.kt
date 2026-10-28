package com.quantumtemplates

// The templates are written once, K&R, and converted when the Allman style is chosen. The conversion only has to
// handle template text, so it works line by line rather than parsing C#.
object BraceStyle {

    // An opening brace at the end of a line that has code before it: `    public class Foo : Bar {`
    private val OPENING_BRACE = Regex("""^(\s*)(\S.*?)\s*\{\s*$""")

    fun apply(text: String, style: TemplateBraceStyle): String = when (style) {
        TemplateBraceStyle.K_AND_R -> text
        TemplateBraceStyle.ALLMAN -> toAllman(text)
    }

    private fun toAllman(text: String): String {
        val lines = text.lines()
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i++]
            val match = OPENING_BRACE.matchEntire(line)
            if (match == null || line.trimStart().startsWith("//")) {
                out += line
                continue
            }
            val (indent, head) = match.destructured
            out += indent + head
            out += "$indent{"
            // K&R templates leave a blank line after `class Foo {`; under Allman the brace line already separates
            // the body, so drop it. Keep it in an empty body (`{`, blank, `}`) where it marks where code goes.
            if (i + 1 < lines.size && lines[i].isBlank() && lines[i + 1].trim() != "}") i++
        }
        return out.joinToString("\n")
    }
}
