import java.lang.Integer.max
import java.lang.Integer.min

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
            is Constant -> {
                if (value < 0) "($value)" else "$value"
            }
            is Binary -> "(" + left.toString() + operationMap[op] + right.toString() + ")"
        }
    }

    fun calc(): Expression {
        return when (this) {
            Element -> this
            is Constant -> this
            is Binary -> {
                val l = left.calc()
                val r = right.calc()
                return if (l is Constant && r is Constant && op is Op.NumType) {
                    when (op) {
                        Op.NumType.PLUS -> Constant(l.value + r.value)
                        Op.NumType.MINUS -> Constant(l.value - r.value)
                        Op.NumType.TIMES -> Constant(l.value * r.value)
                    }
                } else {
                    Binary(l, op, r)
                }
            }
        }
    }

    private val invOp = mapOf(
        Op.NumType.PLUS to Op.NumType.PLUS, Op.NumType.MINUS to Op.NumType.MINUS,
        Op.NumType.TIMES to Op.NumType.TIMES, Op.LogicType.AND to Op.LogicType.AND,
        Op.LogicType.OR to Op.LogicType.OR, Op.LogicType.EQ to Op.LogicType.EQ,
        Op.LogicType.GT to Op.LogicType.LT, Op.LogicType.LT to Op.LogicType.GT
    )

    fun swapVarAndNum(): Expression {
        return when (this) {
            Element -> this
            is Constant -> this
            is Binary -> if (left is Constant && right !is Constant) Binary(right, invOp[op] ?: error(""), left)
            else Binary(left.swapVarAndNum(), op, right.swapVarAndNum())
        }
    }

    private fun calcRight(left: Constant, right: Constant, op: Op): Constant? {
        return when (op) {
            Op.NumType.PLUS -> Constant(right.value - left.value)
            Op.NumType.MINUS -> Constant(right.value + left.value)
            else -> null
        }
    }

    private val correctOp = mapOf(Op.LogicType.GT to true, Op.LogicType.LT to true, Op.LogicType.EQ to true)

    fun simplifyLtGtEq(): Expression {
        return when (this) {
            Element -> this
            is Constant -> this
            is Binary -> if (left is Binary && left.right is Constant && right is Constant) {
                val r = calcRight(left.right, right, left.op)
                if (correctOp[op] != null && r != null) Binary(left.left, op, r) else this
            } else Binary(left.simplifyLtGtEq(), op, right.simplifyLtGtEq())
        }
    }


    fun checkIsAlwaysFalse(): Boolean {
        return this is Binary && left is Constant && right is Constant && op is Op.LogicType.EQ && left.value != right.value
    }

    private fun simpAnd(leftN: Constant, leftOp: Op, rightN: Constant, rightOp: Op, default: Expression): Expression {
        if (leftOp is Op.LogicType.EQ && rightOp is Op.LogicType.EQ) {
            return if (leftN.value == rightN.value) Binary(Element, Op.LogicType.EQ, leftN)
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.EQ && rightOp is Op.LogicType.GT) {
            return if (leftN.value > rightN.value) Binary(Element, Op.LogicType.EQ, leftN)
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.EQ && rightOp is Op.LogicType.LT) {
            return if (leftN.value < rightN.value) Binary(Element, Op.LogicType.EQ, leftN)
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.GT && rightOp is Op.LogicType.EQ) {
            return if (leftN.value < rightN.value) Binary(Element, Op.LogicType.EQ, rightN)
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.GT && rightOp is Op.LogicType.GT) {
            return Binary(Element, Op.LogicType.GT, Constant(max(leftN.value, rightN.value)))
        }
        if (leftOp is Op.LogicType.GT && rightOp is Op.LogicType.LT) {
            return if (leftN.value + 1 < rightN.value) default
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.LT && rightOp is Op.LogicType.EQ) {
            return if (leftN.value > rightN.value) Binary(Element, Op.LogicType.EQ, rightN)
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.LT && rightOp is Op.LogicType.GT) {
            return if (leftN.value - 1 > rightN.value) default
            else Binary(Constant(1), Op.LogicType.EQ, Constant(0))
        }
        if (leftOp is Op.LogicType.LT && rightOp is Op.LogicType.LT) {
            return Binary(Element, Op.LogicType.LT, Constant(min(leftN.value, rightN.value)))
        }
        return default
    }

    fun simplifyFalse(): Expression {
        return when (this) {
            Element -> this
            is Constant -> this
            is Binary -> {
                val l = left.simplifyFalse()
                val r = right.simplifyFalse()
                when (op) {
                    Op.LogicType.AND -> {
                        if (l.checkIsAlwaysFalse() || r.checkIsAlwaysFalse()) {
                            Binary(Constant(1), Op.LogicType.EQ, Constant(0))
                        } else {
                            if (l is Binary && r is Binary && l.left is Element && r.left is Element) {
                                simpAnd(l.right as Constant, l.op, r.right as Constant, r.op, Binary(l, op, r))
                            } else Binary(l, op, r)
                        }
                    }
                    else -> Binary(l, op, r)
                }
            }
        }
    }

    fun isNumType(): Boolean {
        return when (this) {
            Element -> true
            is Constant -> true
            is Binary -> when (op) {
                is Op.NumType -> left.isNumType() && right.isNumType()
                is Op.LogicType -> false
            }
        }
    }

    fun isLogicType(): Boolean {
        return when (this) {
            Element -> false
            is Constant -> false
            is Binary -> when (op) {
                is Op.LogicType -> (left.isNumType() && right.isNumType()) || (left.isLogicType() && right.isLogicType())
                is Op.NumType -> false
            }
        }
    }

}



