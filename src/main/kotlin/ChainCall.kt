sealed class CallChain {
    sealed class Call : CallChain() {
        data class MapCall (
            val expr : Expression
        ) : Call()

        data class FilterCall (
            val expr : Expression
        ) : Call()

        fun subst(new: Expression) : Call {
            return when(this) {
                is MapCall -> MapCall(expr.subst(new))
                is FilterCall -> FilterCall(expr.subst(new))
            }
        }

        fun convertCallToString() : String {
            return when(this) {
                is FilterCall -> "filter{$expr}"
                is MapCall -> "map{$expr}"
            }
        }

    }

    class Chain(val head : Call, val tail : CallChain) : CallChain()
}



class FilterMapCall(var filterCall: CallChain.Call.FilterCall, var mapCall: CallChain.Call.MapCall) {
    fun toCallChain() : CallChain {
        return CallChain.Chain(filterCall, mapCall)
    }
}



fun CallChain.convertToString() : String {
    return when(this) {
        is CallChain.Call -> convertCallToString()
        is CallChain.Chain -> head.convertCallToString() + "%>%" + tail.convertToString()
    }
}

