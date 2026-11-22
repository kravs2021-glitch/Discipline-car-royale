package com.example.royale.data

/**
 * Lightweight pure-Kotlin JSON reader/writer so save serialization works identically
 * on Android devices and local JVM unit tests without android.jar stub dependencies.
 */
internal object RoyaleJson {

    fun escape(s: String): String = buildString(s.length + 4) {
        append('"')
        for (ch in s) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    fun encodeValue(v: Any?): String = when (v) {
        null -> "null"
        is Boolean, is Int, is Long, is Float, is Double -> v.toString()
        is String -> escape(v)
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { (k, valItem) ->
            "${escape(k.toString())}:${encodeValue(valItem)}"
        }
        is Iterable<*> -> v.joinToString(",", "[", "]") { encodeValue(it) }
        else -> escape(v.toString())
    }

    fun parseObject(json: String): Map<String, Any?> {
        val parsed = Parser(json).parseValue()
        @Suppress("UNCHECKED_CAST")
        return parsed as? Map<String, Any?> ?: emptyMap()
    }

    private class Parser(private val src: String) {
        private var pos = 0

        private fun skipWs() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        fun parseValue(): Any? {
            skipWs()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> parseObj()
                '[' -> parseArr()
                '"' -> parseStr()
                't', 'f' -> parseBool()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObj(): Map<String, Any?> {
            pos++ // '{'
            val map = LinkedHashMap<String, Any?>()
            skipWs()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                val key = parseStr()
                skipWs()
                if (pos < src.length && src[pos] == ':') pos++
                val value = parseValue()
                map[key] = value
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    if (pos < src.length && src[pos] == '}') pos++
                    break
                }
            }
            return map
        }

        private fun parseArr(): List<Any?> {
            pos++ // '['
            val list = ArrayList<Any?>()
            skipWs()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(parseValue())
                skipWs()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else {
                    if (pos < src.length && src[pos] == ']') pos++
                    break
                }
            }
            return list
        }

        private fun parseStr(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') break
                if (c == '\\' && pos < src.length) {
                    val esc = src[pos++]
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> if (pos + 4 <= src.length) {
                            sb.append(src.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseBool(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4
                true
            } else {
                pos += 5
                false
            }
        }

        private fun parseNull(): Any? {
            pos += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = pos
            while (pos < src.length && (src[pos].isDigit() || src[pos] in "-+.eE")) pos++
            val raw = src.substring(start, pos)
            return if (raw.contains('.') || raw.contains('e', ignoreCase = true)) {
                raw.toDouble()
            } else {
                raw.toLong()
            }
        }
    }
}
