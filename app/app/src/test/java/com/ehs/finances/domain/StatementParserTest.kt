package com.ehs.finances.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementParserTest {
    @Test
    fun `CSV com cabecalho e ponto e virgula`() {
        val parsed = StatementParser.parseFile(
            """
            Data;Histórico;Valor
            16/11/2026;Mercado;-123,45
            15/11/2026;Salário;5000,00
            """.trimIndent(),
            "extrato.csv",
        )

        assertEquals(StatementFormat.CSV, parsed.format)
        assertEquals(2, parsed.lines.size)

        val expense = parsed.lines[0]
        assertEquals(2, expense.row)
        assertEquals("Mercado", expense.description)
        assertEquals(123.45, expense.amount!!, 0.001)
        assertFalse(expense.income)
        assertEquals("2026-11-16", expense.date)
        assertTrue(expense.isValid)

        val income = parsed.lines[1]
        assertEquals(3, income.row)
        assertEquals(5000.0, income.amount!!, 0.001)
        assertTrue(income.income)
        assertEquals("2026-11-15", income.date)
    }

    @Test
    fun `CSV sem cabecalho usa colunas padrao`() {
        val parsed = StatementParser.parseFile(
            "16/11/2026;Mercado;123,45",
            "extrato.csv",
        )

        val line = parsed.lines.single()
        assertEquals(1, line.row)
        assertEquals("Mercado", line.description)
        assertEquals(123.45, line.amount!!, 0.001)
        assertTrue(line.income)
        assertEquals("2026-11-16", line.date)
    }

    @Test
    fun `CSV com colunas de credito e debito`() {
        val parsed = StatementParser.parseFile(
            """
            Data,Descrição,Crédito,Débito
            16/11/2026,Pix recebido,100.00,
            16/11/2026,Conta,-,50.00
            """.trimIndent(),
            "extrato.csv",
        )

        val credit = parsed.lines[0]
        assertEquals(100.0, credit.amount!!, 0.001)
        assertTrue(credit.income)

        val debit = parsed.lines[1]
        assertEquals(50.0, debit.amount!!, 0.001)
        assertFalse(debit.income)
    }

    @Test
    fun `CSV com coluna tipo define a direcao`() {
        val parsed = StatementParser.parseFile(
            """
            Data,Histórico,Valor,Tipo
            16/11/2026,Salário,5000,Crédito
            16/11/2026,Mercado,123,Debito
            """.trimIndent(),
            "extrato.csv",
        )

        assertTrue(parsed.lines[0].income)
        assertFalse(parsed.lines[1].income)
        assertEquals(5000.0, parsed.lines[0].amount!!, 0.001)
        assertEquals(123.0, parsed.lines[1].amount!!, 0.001)
    }

    @Test
    fun `CSV aceita parenteses como debito`() {
        val parsed = StatementParser.parseFile(
            "16/11/2026,Compra,(123.45)",
            "extrato.csv",
        )

        val line = parsed.lines.single()
        assertEquals(123.45, line.amount!!, 0.001)
        assertFalse(line.income)
    }

    @Test
    fun `CSV aceita variacoes de data`() {
        val parsed = StatementParser.parseFile(
            """
            Data;Descrição;Valor
            2026-11-16;A;1,00
            16/11/2026;B;2,00
            16-11-2026;C;3,00
            16.11.2026;D;4,00
            16/11/26;E;5,00
            20261116;F;6,00
            """.trimIndent(),
            "extrato.csv",
        )

        assertEquals(
            listOf("2026-11-16", "2026-11-16", "2026-11-16", "2026-11-16", "2026-11-16", "2026-11-16"),
            parsed.lines.map { it.date },
        )
    }

    @Test
    fun `CSV reporta erros por linha`() {
        val parsed = StatementParser.parseFile(
            """
            Data;Descrição;Valor
            31/02/2026;Data inválida;10,00
            16/11/2026;;20,00
            16/11/2026;Sem valor;abc
            """.trimIndent(),
            "extrato.csv",
        )

        assertTrue(StatementLineError.INVALID_DATE in parsed.lines[0].errors)
        assertTrue(StatementLineError.MISSING_DESCRIPTION in parsed.lines[1].errors)
        assertTrue(StatementLineError.INVALID_AMOUNT in parsed.lines[2].errors)
        assertFalse(parsed.lines[0].isValid)
        assertFalse(parsed.lines[1].isValid)
        assertFalse(parsed.lines[2].isValid)
    }

    @Test
    fun `CSV com zeros nao conta como valor`() {
        val parsed = StatementParser.parseFile(
            "16/11/2026;Zerado;0,00",
            "extrato.csv",
        )

        assertNull(parsed.lines.single().amount)
        assertTrue(StatementLineError.INVALID_AMOUNT in parsed.lines.single().errors)
    }

    @Test
    fun `OFX le MEMO FITID e sinal do TRNAMT`() {
        val parsed = StatementParser.parseFile(
            """
            <OFX>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20261116
            <TRNAMT>-123.45
            <FITID>abc123
            <MEMO>Compra &amp; Cia</MEMO>
            </STMTTRN>
            <STMTTRN>
            <TRNTYPE>CREDIT
            <DTPOSTED>20261115
            <TRNAMT>5000.00
            <FITID>def456
            <MEMO>Salário</MEMO>
            </STMTTRN>
            </BANKTRANLIST>
            </OFX>
            """.trimIndent(),
            "extrato.ofx",
        )

        assertEquals(StatementFormat.OFX, parsed.format)
        assertEquals(2, parsed.lines.size)

        val expense = parsed.lines[0]
        assertEquals("Compra & Cia", expense.description)
        assertEquals(123.45, expense.amount!!, 0.001)
        assertFalse(expense.income)
        assertEquals("2026-11-16", expense.date)
        assertEquals("abc123", expense.sourceTransactionId)

        val income = parsed.lines[1]
        assertEquals(5000.0, income.amount!!, 0.001)
        assertTrue(income.income)
        assertEquals("def456", income.sourceTransactionId)
    }

    @Test
    fun `arquivo vazio ou sem transacoes falha`() {
        assertParseError(StatementParseErrorCode.EMPTY_FILE, "", "vazio.csv")
        assertParseError(
            StatementParseErrorCode.NO_TRANSACTIONS,
            "Data;Descrição;Valor",
            "sem-linhas.csv",
        )
        assertParseError(
            StatementParseErrorCode.NO_TRANSACTIONS,
            "<OFX></OFX>",
            "sem-transacoes.ofx",
        )
    }

    @Test
    fun `decode cai para windows-1252 quando o utf-8 e invalido`() {
        val utf8 = "Salário".toByteArray(Charsets.UTF_8)
        assertEquals("Salário", StatementParser.decode(utf8))

        val windows = byteArrayOf(
            'S'.code.toByte(),
            'a'.code.toByte(),
            'l'.code.toByte(),
            0xE1.toByte(),
            'r'.code.toByte(),
            'i'.code.toByte(),
            'o'.code.toByte(),
        )
        assertEquals("Salário", StatementParser.decode(windows))
    }

    @Test
    fun `sanitizeSource remove acentos e caracteres invalidos`() {
        assertEquals(
            "extrato-conta-corrente-nov-2026",
            StatementParser.sanitizeSource("extrato conta corrente (nov 2026).CSV"),
        )
        assertEquals("Extrato-Acao", StatementParser.sanitizeSource("Extrato Ação.pdf"))
        assertEquals("import", StatementParser.sanitizeSource("---.csv"))
    }

    @Test
    fun `isValidDate aceita apenas yyyy-MM-dd real`() {
        assertTrue(StatementParser.isValidDate("2026-11-16"))
        assertFalse(StatementParser.isValidDate("2026-02-30"))
        assertFalse(StatementParser.isValidDate("16/11/2026"))
        assertFalse(StatementParser.isValidDate(null))
        assertTrue(StatementParser.isValidDate(LocalDate.of(2026, 11, 16).toString()))
    }

    private fun assertParseError(
        expected: StatementParseErrorCode,
        content: String,
        fileName: String,
    ) {
        val error = runCatching { StatementParser.parseFile(content, fileName) }.exceptionOrNull()
        assertTrue(error is StatementParseException)
        assertEquals(expected, (error as StatementParseException).code)
    }
}
