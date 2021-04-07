fun String.splitIntoGroups(): List<String> {
    val matchResults = Regex(
        """element|\+|-|\*|<|>|=|&|\||\(|\)|\d+|%>%|}|filter\{|map\{"""
    ).findAll(this)
    return matchResults.map { it.value }
        .filter { it.isNotBlank() }
        .toList()
}

class Parser(private val groups: List<String>) {
    private var pos = 0
    fun parse(): CallChain {
        val result = parseCallChain()
        if (pos < groups.size) throw SyntaxErrorException()
        return result
    }

    private fun parseCallChain(): CallChain {
        var left: CallChain = parseCall()
        if (pos < groups.size) {
            if (groups[pos] == "%>%") {
                pos++
                left = CallChain.Chain(left as CallChain.Call, parseCallChain())
            } else return left
        }
        return left
    }

    private val callMap =
        mapOf(
            "map{" to { expr: Expression -> CallChain.Call.MapCall(expr) },
            "filter{" to { expr: Expression -> CallChain.Call.FilterCall(expr) })

    private val operationMap = mapOf(
        "+" to Expression.Op.NumType.PLUS, "-" to Expression.Op.NumType.MINUS,
        "*" to Expression.Op.NumType.TIMES, ">" to Expression.Op.LogicType.GT,
        "<" to Expression.Op.LogicType.LT, "&" to Expression.Op.LogicType.AND, "|" to Expression.Op.LogicType.OR,
        "=" to Expression.Op.LogicType.EQ
    )

    private fun checkExprType(expr: Expression, makeCall: (Expression) -> CallChain.Call): CallChain.Call {
        when (val call = makeCall(expr)) {
            is CallChain.Call.FilterCall -> {
                if (call.expr.isLogicType()) return call
                throw TypeErrorException()
            }
            is CallChain.Call.MapCall -> {
                if (call.expr.isNumType()) return call
                throw TypeErrorException()
            }
        }
    }

    private fun parseCall(): CallChain.Call {
        val makeCall = callMap[groups[pos++]]
        if (makeCall != null) {
            val expr = parseExpression()

            if (groups[pos++] != "}")
                throw SyntaxErrorException()
            return checkExprType(expr, makeCall)

        } else throw SyntaxErrorException()
    }

    private fun parseExpression(): Expression {
        val left = parseElement()
        if (pos < groups.size) {
            val op = operationMap[groups[pos]]
            return if (op != null) {
                pos++
                Expression.Binary(left, op, parseExpression())
            } else left
        }
        return left
    }

    private fun parseElement(): Expression {
        if (pos >= groups.size) throw SyntaxErrorException()
        else {
            return when (val group = groups[pos++]) {
                "element" -> Expression.Element
                "-" -> {
                    Expression.Constant(-groups[pos++].toInt())
                }
                "(" -> {
                    val arg = parseExpression()
                    val next = groups[pos++]
                    if (next == ")") arg
                    else throw SyntaxErrorException()
                }
                else -> Expression.Constant(group.toInt())
            }
        }

    }
}

fun parseAndTransform(input: String): String {
    val groups = input.splitIntoGroups()
    if (groups.isEmpty())
        return "SYNTAX ERROR"
    val p = Parser(groups)
    return try {
        val cc = p.parse()
        callChainTransform(cc).toCallChain().convertToString()
    } catch (e: Throwable) {
        when (e) {
            is TypeErrorException -> e.message.toString()
            else -> "SYNTAX ERROR"
        }
    }
}

class SyntaxErrorException(message: String = "SYNTAX ERROR") : Exception(message)
class TypeErrorException(message: String = "TYPE ERROR") : Exception(message)


