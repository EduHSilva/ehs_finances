package com.ehs.finances.domain

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/**
 * Porte fiel do parser de extratos do web (`web/app/utils/statementImport.ts`).
 * Aceita CSV (com ou sem cabeçalho) e OFX, devolvendo linhas normalizadas.
 */
enum class StatementFormat { CSV, OFX }

enum class StatementLineError { MISSING_DESCRIPTION, INVALID_AMOUNT, INVALID_DATE }

enum class StatementParseErrorCode { EMPTY_FILE, NO_TRANSACTIONS }

class StatementParseException(
    val code: StatementParseErrorCode,
) : Exception(code.name)

data class ParsedStatementLine(
    val row: Int,
    val description: String,
    val amount: Double?,
    val income: Boolean,
    val date: String?,
    val sourceTransactionId: String?,
    val errors: List<StatementLineError>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

data class ParsedStatement(
    val format: StatementFormat,
    val lines: List<ParsedStatementLine>,
)

object StatementParser {
    private val headerPattern = Regex(
        "(data|date|descri|hist[oó]rico|lan[cç]amento|memo|valor|amount|cr[eé]dito|d[eé]bito|saldo|balance)",
        RegexOption.IGNORE_CASE,
    )

    private val dateOnly = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun parseFile(content: String, fileName: String): ParsedStatement {
        val text = content.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) throw StatementParseException(StatementParseErrorCode.EMPTY_FILE)
        return if (text.contains("<OFX>", ignoreCase = true) || fileName.endsWith(".ofx", ignoreCase = true)) {
            ParsedStatement(StatementFormat.OFX, parseOfx(text))
        } else {
            ParsedStatement(StatementFormat.CSV, parseCsv(text))
        }
    }

    /** Decodifica como UTF-8 estrito e cai para Windows-1252 em arquivos de banco antigos. */
    fun decode(bytes: ByteArray): String {
        val utf8 = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return runCatching { utf8.decode(ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { String(bytes, charset("windows-1252")) }
    }

    fun sanitizeSource(fileName: String): String {
        val base = fileName.replace(Regex("\\.[^.]+$"), "")
        val normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('-')
            .take(40)
        return normalized.ifBlank { "import" }
    }

    fun isValidDate(value: String?): Boolean =
        value != null && dateOnly.matches(value) &&
            runCatching { LocalDate.parse(value) }.isSuccess

    // ----- CSV --------------------------------------------------------------

    private fun parseCsv(text: String): List<ParsedStatementLine> {
        val delimiter = detectDelimiter(text.take(5000))
        val rows = parseDelimited(text, delimiter)
        if (rows.isEmpty()) throw StatementParseException(StatementParseErrorCode.NO_TRANSACTIONS)

        val hasHeader = isHeaderRow(rows.first())
        val map = if (hasHeader) buildColumnMap(rows.first()) else defaultColumnMap(rows.first().size)
        val dataRows = if (hasHeader) rows.drop(1) else rows
        val lines = dataRows.mapIndexedNotNull { index, cells ->
            if (cells.all { it.isBlank() }) {
                null
            } else {
                buildCsvLine(cells, map, if (hasHeader) index + 2 else index + 1)
            }
        }
        if (lines.isEmpty()) throw StatementParseException(StatementParseErrorCode.NO_TRANSACTIONS)
        return lines
    }

    private fun detectDelimiter(sample: String): Char {
        val candidates = listOf(';', ',', '\t')
        var best = ','
        var bestCount = 0
        for (candidate in candidates) {
            val count = countDelimiter(sample, candidate)
            if (count > bestCount) {
                best = candidate
                bestCount = count
            }
        }
        return if (bestCount > 0) best else ','
    }

    private fun countDelimiter(text: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        for (char in text) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> count++
            }
        }
        return count
    }

    private fun parseDelimited(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < text.length) {
            val char = text[index]
            if (inQuotes) {
                if (char == '"') {
                    if (index + 1 < text.length && text[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(char)
                }
                index++
                continue
            }

            when (char) {
                '"' -> inQuotes = true
                delimiter -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                '\n' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row)
                    row = mutableListOf()
                }
                '\r' -> {
                    if (index + 1 >= text.length || text[index + 1] != '\n') {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                    }
                }
                else -> field.append(char)
            }
            index++
        }

        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }

        return rows.filter { cells -> cells.any { it.isNotBlank() } }
    }

    private fun isHeaderRow(row: List<String>): Boolean =
        row.count { headerPattern.containsMatchIn(it) } >= 2

    private data class ColumnMap(
        val date: Int,
        val description: Int,
        val amount: Int,
        val credit: Int,
        val debit: Int,
        val direction: Int,
        val fitid: Int,
    )

    private fun defaultColumnMap(width: Int): ColumnMap = ColumnMap(
        date = 0,
        description = if (width > 1) 1 else -1,
        amount = if (width > 2) 2 else -1,
        credit = -1,
        debit = -1,
        direction = -1,
        fitid = -1,
    )

    private fun buildColumnMap(header: List<String>): ColumnMap {
        val map = ColumnMap(
            date = columnIndex(header, listOf("data", "date", "dia", "vencimento", "compet[eê]ncia")),
            description = columnIndex(
                header,
                listOf(
                    "descri", "hist[oó]rico", "lan[cç]amento", "memo", "estabelec",
                    "favorecid", "detalh", "t[ií]tulo", "title",
                ),
            ),
            credit = columnIndex(header, listOf("cr[eé]dito", "credit", "entrada")),
            debit = columnIndex(header, listOf("d[eé]bito", "debit", "sa[ií]da")),
            amount = columnIndex(header, listOf("valor", "amount", "montante", "value", "quantia")),
            direction = columnIndex(header, listOf("tipo", "type", "natureza", "dire[cç][aã]o")),
            fitid = columnIndex(header, listOf("fitid", "documento", "autentica", "identificador")),
        )

        return when {
            map.direction >= 0 -> map.copy(credit = -1, debit = -1)
            map.credit >= 0 || map.debit >= 0 -> map.copy(amount = -1)
            else -> map
        }
    }

    private fun columnIndex(header: List<String>, patterns: List<String>): Int {
        for (pattern in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val index = header.indexOfFirst { regex.containsMatchIn(it) }
            if (index >= 0) return index
        }
        return -1
    }

    private fun buildCsvLine(cells: List<String>, map: ColumnMap, row: Int): ParsedStatementLine {
        val errors = mutableListOf<StatementLineError>()
        val description = cleanDescription(cellAt(cells, map.description))
        val date = parseStatementDate(cellAt(cells, map.date))
        var amount: Double? = null
        var income = false

        if (map.credit >= 0 || map.debit >= 0) {
            val credit = parseAmountCell(cellAt(cells, map.credit))
            val debit = parseAmountCell(cellAt(cells, map.debit))
            if (credit.first != null && kotlin.math.abs(credit.first!!) > 0) {
                amount = kotlin.math.abs(credit.first!!)
                income = true
            } else if (debit.first != null && kotlin.math.abs(debit.first!!) > 0) {
                amount = kotlin.math.abs(debit.first!!)
                income = false
            }
        } else {
            val parsed = parseAmountCell(cellAt(cells, map.amount))
            val value = parsed.first
            if (value != null && kotlin.math.abs(value) > 0) {
                amount = kotlin.math.abs(value)
                income = resolveIncome(cellAt(cells, map.direction), parsed.second, value)
            }
        }

        if (description.isBlank()) errors += StatementLineError.MISSING_DESCRIPTION
        if (date == null) errors += StatementLineError.INVALID_DATE
        if (amount == null) errors += StatementLineError.INVALID_AMOUNT

        return ParsedStatementLine(
            row = row,
            description = description,
            amount = amount,
            income = income,
            date = date,
            sourceTransactionId = cellAt(cells, map.fitid).ifBlank { null },
            errors = errors,
        )
    }

    // ----- OFX --------------------------------------------------------------

    private val ofxBlock = Regex("<STMTTRN>([\\s\\S]*?)</STMTTRN>", RegexOption.IGNORE_CASE)

    private fun parseOfx(text: String): List<ParsedStatementLine> {
        val blocks = ofxBlock.findAll(text).toList()
        if (blocks.isEmpty()) throw StatementParseException(StatementParseErrorCode.NO_TRANSACTIONS)

        return blocks.mapIndexed { index, block ->
            val body = block.groupValues.getOrElse(1) { "" }
            fun tag(name: String): String {
                val match = Regex("<$name>([^<\\r\\n]*)", RegexOption.IGNORE_CASE).find(body)
                return decodeXml(match?.groupValues?.getOrNull(1)?.trim().orEmpty())
            }

            val description = cleanDescription(
                tag("MEMO").ifBlank { tag("NAME") }.ifBlank { tag("TRNTYPE") },
            )
            val date = parseOfxDate(tag("DTPOSTED"))
            val signedAmount = normalizeDecimal(tag("TRNAMT"))
            val amount = signedAmount?.let { kotlin.math.abs(it) }
            val errors = mutableListOf<StatementLineError>()

            if (description.isBlank()) errors += StatementLineError.MISSING_DESCRIPTION
            if (date == null) errors += StatementLineError.INVALID_DATE
            if (amount == null || amount == 0.0) errors += StatementLineError.INVALID_AMOUNT

            ParsedStatementLine(
                row = index + 1,
                description = description,
                amount = if (amount != null && amount > 0) amount else null,
                income = signedAmount != null && signedAmount >= 0,
                date = date,
                sourceTransactionId = tag("FITID").ifBlank { null },
                errors = errors,
            )
        }
    }

    private fun parseOfxDate(raw: String): String? {
        val compact = Regex("^(\\d{4})(\\d{2})(\\d{2})").find(raw.trim())
        if (compact != null) {
            val groups = compact.groupValues
            return validDate(
                groups[1].toIntOrNull() ?: return null,
                groups[2].toIntOrNull() ?: return null,
                groups[3].toIntOrNull() ?: return null,
            )
        }
        return parseStatementDate(raw)
    }

    private fun parseStatementDate(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(value)?.let { match ->
            return validDate(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
            )
        }

        Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})").find(value)?.let { match ->
            val rawYear = match.groupValues[3].toInt()
            val year = if (rawYear < 100) rawYear + 2000 else rawYear
            return validDate(year, match.groupValues[2].toInt(), match.groupValues[1].toInt())
        }

        Regex("^(\\d{4})[/.](\\d{1,2})[/.](\\d{1,2})").find(value)?.let { match ->
            return validDate(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
            )
        }

        if (Regex("^\\d{8}$").matches(value)) {
            return validDate(
                value.substring(0, 4).toInt(),
                value.substring(4, 6).toInt(),
                value.substring(6, 8).toInt(),
            )
        }

        return null
    }

    private fun validDate(year: Int, month: Int, day: Int): String? {
        if (month !in 1..12 || day !in 1..31) return null
        val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return null
        return String.format(Locale.US, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)
    }

    // ----- Valores ----------------------------------------------------------

    private fun parseAmountCell(raw: String): Pair<Double?, String?> {
        var cell = raw.trim()
        if (cell.isEmpty()) return null to null

        var direction: String? = null

        Regex("\\(([^)]*)\\)").find(cell)?.let { match ->
            cell = match.groupValues[1]
            direction = "debit"
        }

        Regex("^(.*\\d.*?)\\s*([CD])$", RegexOption.IGNORE_CASE).find(cell)?.let { match ->
            direction = if (match.groupValues[2].uppercase() == "C") "credit" else "debit"
            cell = match.groupValues[1]
        }

        val trimmed = cell.trim()
        val negative = trimmed.startsWith('-') || trimmed.endsWith('-')
        val amount = normalizeDecimal(cell)
        if (amount == null) return null to direction
        if (direction == null && negative) direction = "debit"
        return amount to direction
    }

    private fun normalizeDecimal(raw: String): Double? {
        var value = raw.trim().replace(Regex("[^\\d.,-]"), "")
        if (value.isEmpty()) return null

        var negative = false
        if (value.startsWith('-')) {
            negative = true
            value = value.substring(1)
        }
        if (value.endsWith('-')) {
            negative = true
            value = value.dropLast(1)
        }
        value = value.replace("-", "")
        if (value.isEmpty()) return null

        val comma = value.lastIndexOf(',')
        val dot = value.lastIndexOf('.')
        val normalized = when {
            comma >= 0 && dot >= 0 ->
                if (comma > dot) value.replace(".", "").replace(',', '.')
                else value.replace(",", "")
            comma >= 0 -> {
                val fraction = value.length - comma - 1
                if (fraction in 1..2) value.replace(".", "").replace(',', '.')
                else value.replace(",", "")
            }
            else -> {
                val parts = value.split('.')
                if (parts.size > 2) parts.dropLast(1).joinToString("") + "." + parts.last()
                else value
            }
        }

        val amount = normalized.toDoubleOrNull() ?: return null
        if (!amount.isFinite()) return null
        return if (negative) -amount else amount
    }

    private fun resolveIncome(
        rawDirection: String,
        amountDirection: String?,
        signedAmount: Double,
    ): Boolean {
        val direction = directionFromType(rawDirection)
        if (direction != null) return direction == "credit"
        if (amountDirection != null) return amountDirection == "credit"
        return signedAmount >= 0
    }

    private fun directionFromType(raw: String): String? {
        val value = raw.trim().lowercase(Locale.ROOT)
        if (value.isEmpty()) return null
        if (Regex("^(c|cr|cred|credit|cr[eé]dito|entrada|receita|income|deposito|dep[oó]sito|deposit)$").matches(value)) {
            return "credit"
        }
        if (Regex("^(d|db|deb|debit|d[eé]bito|saida|sa[ií]da|despesa|expense|pagamento|payment)$").matches(value)) {
            return "debit"
        }
        if (Regex("cr[eé]dito|credit|entrada|receita|income").containsMatchIn(value)) return "credit"
        if (Regex("d[eé]bito|debit|sa[ií]da|despesa|expense").containsMatchIn(value)) return "debit"
        return null
    }

    private fun cellAt(cells: List<String>, index: Int): String =
        if (index >= 0) cells.getOrElse(index) { "" }.trim() else ""

    private fun cleanDescription(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").take(300)

    private fun decodeXml(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&apos;", "'", ignoreCase = true)
}
