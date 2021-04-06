import org.junit.Test
import kotlin.test.assertEquals

class TestFilterMapParser {
    private val correctInputs = listOf("map{(element+10)}%>%filter{(element>10)}%>%map{(element*element)}%>%filter{(element=30)}",
        "filter{((element+10)<(-6+78))}%>%map{((element+10)*(6-78))}",
        "filter{(element>10)}%>%filter{(element<20)}",
        "map{(element*element)}")

    @Test
    fun testParseCorrectImport() {
        correctInputs.forEach {
            val groups = it.splitIntoGroups()
            val p = Parser(groups)
            assertEquals(it, p.parse().convertToString())
        }
    }

    @Test
    fun testIncorrectInputsSyntaxError() {
        val s1 = "filter{(qwq)}filter{(element<20)}"
        val s2 = "(76 + 21)"
        val s3 = "ewqeqwrwer"
        assertEquals("SYNTAX ERROR", parseAndTransform(s1))
        assertEquals("SYNTAX ERROR", parseAndTransform(s2))
        assertEquals("SYNTAX ERROR", parseAndTransform(s3))
    }

    @Test
    fun testIncorrectInputsTypeError() {
        val s1 = "map{(element>10)}%>%filter{(element<20)}"
        val s2 = "map{(element*10)}%>%filter{(element-20)}"
        val s3 = "map{(12)}%>%filter{(element)}"
        assertEquals("TYPE ERROR", parseAndTransform(s1))
        assertEquals("TYPE ERROR", parseAndTransform(s2))
        assertEquals("TYPE ERROR", parseAndTransform(s3))
    }

    @Test
    fun testTransform() {
        assertEquals("filter{(((element+10)>10)&(((element+10)*(element+10))=30))}%>%map{((element+10)*(element+10))}",
            parseAndTransform(correctInputs[0]))
        assertEquals("filter{((element>10)&(element<20))}%>%map{element}", parseAndTransform(correctInputs[2]))
        assertEquals("filter{(element=element)}%>%map{(element*element)}", parseAndTransform(correctInputs[3]))
    }
}