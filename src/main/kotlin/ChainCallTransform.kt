fun updateState(call: CallChain.Call, state: FilterMapCall): FilterMapCall {
    return when (call) {
        is CallChain.Call.MapCall -> FilterMapCall(state.filterCall, call.subst(state.mapCall.expr) as CallChain.Call.MapCall)
        is CallChain.Call.FilterCall -> when (state.filterCall.expr) {
            Expression.Element -> FilterMapCall(CallChain.Call.FilterCall(call.expr.subst(state.mapCall.expr)), state.mapCall)
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
        is CallChain.Call -> removeTempFilter(updateState(chain, state))
        is CallChain.Chain -> callChainTransform(chain.tail, updateState(chain.head, state))
    }
}

fun removeTempFilter(input : FilterMapCall) : FilterMapCall {
    return when(input.filterCall.expr) {
        Expression.Element -> FilterMapCall(CallChain.Call.FilterCall(
            Expression.Binary(Expression.Element, Expression.Op.LogicType.EQ, Expression.Element)), input.mapCall)
        else -> input
    }
}