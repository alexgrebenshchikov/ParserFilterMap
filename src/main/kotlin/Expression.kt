sealed class Expression {
    object Element : Expression()
    class Constant(val value: Int) : Expression()

    sealed class Op {
        sealed class LogicType : Op() {
            object GT : LogicType()
            object LT : LogicType()
            object AND : LogicType()
            object OR : LogicType()
            object EQ : LogicType()
        }

        sealed class NumType : Op() {
            object PLUS : NumType()
            object MINUS : NumType()
            object TIMES : NumType()
        }

    }


    class Binary(
        val left: Expression,
        val op: Op,
        val right: Expression
    ) : Expression()

    class Negate(val arg: Expression) : Expression()

    fun subst(from: Expression): Expression {
        return when (this) {
            Element -> from
            is Binary ->
                Binary(left.subst(from), op, right.subst(from))
            else -> this
        }
    }

    private val operationMap = mapOf(
        Op.NumType.PLUS to "+", Op.NumType.MINUS to "-",
        Op.NumType.TIMES to "*", Op.LogicType.GT to ">",
        Op.LogicType.LT to "<", Op.LogicType.AND to "&",
        Op.LogicType.OR to "|", Op.LogicType.EQ to "="
    )

    override fun toString(): String {
        return when (this) {
            Element -> "element"
            is Constant -> value.toString()
            is Negate -> "-$arg"
            is Binary -> "(" + left.toString() + operationMap[op] + right.toString() + ")"
        }
    }


}

