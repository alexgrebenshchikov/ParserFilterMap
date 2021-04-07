fun updateState(call: CallChain.Call, state: FilterMapCall): FilterMapCall {
    return when (call) {
        is CallChain.Call.MapCall -> FilterMapCall(
            state.filterCall,
            call.subst(state.mapCall.expr) as CallChain.Call.MapCall
        )
        is CallChain.Call.FilterCall -> when (state.filterCall.expr) {
            Expression.Element -> FilterMapCall(
                CallChain.Call.FilterCall(call.expr.subst(state.mapCall.expr)),
                state.mapCall
            )
            else -> FilterMapCall(
                CallChain.Call.FilterCall(
                    Expression.Binary(
                        state.filterCall.expr,
                        Expression.Op.LogicType.AND,
                        call.expr.subst(state.mapCall.expr)
                    )
                ), state.mapCall
            )
        }
    }
}

fun callChainTransform(
    chain: CallChain, state: FilterMapCall =
        FilterMapCall(
            CallChain.Call.FilterCall(Expression.Element),
            CallChain.Call.MapCall(Expression.Element)
        )
): FilterMapCall {
    return when (chain) {
        is CallChain.Call -> updateState(chain, state).removeTempFilter().calcSubExpr()
            .swapVarAndNum().simplifyLtGtEq().simplifyFalse()
        is CallChain.Chain -> callChainTransform(chain.tail, updateState(chain.head, state))
    }
}

fun FilterMapCall.removeTempFilter(): FilterMapCall {
    return when (filterCall.expr) {
        Expression.Element -> FilterMapCall(
            CallChain.Call.FilterCall(
                Expression.Binary(Expression.Element, Expression.Op.LogicType.EQ, Expression.Element)
            ), mapCall
        )
        else -> this
    }
}

fun FilterMapCall.calcSubExpr(): FilterMapCall {
    return FilterMapCall(
        CallChain.Call.FilterCall(filterCall.expr.calc()),
        CallChain.Call.MapCall(mapCall.expr.calc())
    )
}

fun FilterMapCall.swapVarAndNum(): FilterMapCall {
    return FilterMapCall(
        CallChain.Call.FilterCall(filterCall.expr.swapVarAndNum()),
        CallChain.Call.MapCall(mapCall.expr)
    )
}

fun FilterMapCall.simplifyLtGtEq(): FilterMapCall {
    return FilterMapCall(
        CallChain.Call.FilterCall(filterCall.expr.simplifyLtGtEq()),
        CallChain.Call.MapCall(mapCall.expr)
    )
}

fun FilterMapCall.simplifyFalse(): FilterMapCall {
    val simpFilter = filterCall.expr.simplifyFalse()
    val simpMap = if (simpFilter.checkIsAlwaysFalse()) Expression.Element else mapCall.expr
    return FilterMapCall(
        CallChain.Call.FilterCall(simpFilter),
        CallChain.Call.MapCall(simpMap)
    )
}