export type StatementFormat = 'CSV' | 'OFX'
export type StatementLineError = 'missingDescription' | 'invalidAmount' | 'invalidDate'
export type StatementImportParseErrorCode = 'emptyFile' | 'noTransactions'

export interface ParsedStatementLine {
  row: number
  description: string
  amount: number | null
  income: boolean
  date: string | null
  sourceTransactionId?: string
  errors: StatementLineError[]
}

export interface ParsedStatement {
  format: StatementFormat
  lines: ParsedStatementLine[]
}

export class StatementImportParseError extends Error {
  code: StatementImportParseErrorCode

  constructor(code: StatementImportParseErrorCode) {
    super(code)
    this.name = 'StatementImportParseError'
    this.code = code
  }
}

const HEADER_PATTERN = /(data|date|descri|hist[oó]rico|lan[cç]amento|memo|valor|amount|cr[eé]dito|d[eé]bito|saldo|balance)/i

interface ColumnMap {
  date: number
  description: number
  amount: number
  credit: number
  debit: number
  direction: number
  fitid: number
}

export function parseStatementFile(content: string, fileName: string): ParsedStatement {
  const text = content.replace(/^\uFEFF/, '').trim()
  if (!text) throw new StatementImportParseError('emptyFile')

  if (/<OFX>/i.test(text) || /\.ofx$/i.test(fileName)) {
    return { format: 'OFX', lines: parseOfx(text) }
  }
  return { format: 'CSV', lines: parseCsv(text) }
}

export function sanitizeImportSource(fileName: string): string {
  const base = fileName.replace(/\.[^.]+$/, '')
  const normalized = base
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Za-z0-9._-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40)
  return normalized || 'import'
}

function parseCsv(text: string): ParsedStatementLine[] {
  const delimiter = detectDelimiter(text.slice(0, 5000))
  const rows = parseDelimited(text, delimiter)
  if (!rows.length) throw new StatementImportParseError('noTransactions')

  const hasHeader = isHeaderRow(rows[0]!)
  const map = hasHeader ? buildColumnMap(rows[0]!) : defaultColumnMap(rows[0]!.length)
  const dataRows = hasHeader ? rows.slice(1) : rows
  const lines: ParsedStatementLine[] = []

  dataRows.forEach((cells, index) => {
    if (cells.every(cell => !cell.trim())) return
    lines.push(buildCsvLine(cells, map, hasHeader ? index + 2 : index + 1))
  })

  if (!lines.length) throw new StatementImportParseError('noTransactions')
  return lines
}

function detectDelimiter(sample: string): string {
  const candidates = [';', ',', '\t']
  let best = ','
  let bestCount = 0
  for (const candidate of candidates) {
    const count = countDelimiter(sample, candidate)
    if (count > bestCount) {
      best = candidate
      bestCount = count
    }
  }
  return bestCount > 0 ? best : ','
}

function countDelimiter(text: string, delimiter: string): number {
  let count = 0
  let inQuotes = false
  for (let index = 0; index < text.length; index++) {
    const char = text[index]
    if (char === '"') {
      inQuotes = !inQuotes
    } else if (char === delimiter && !inQuotes) {
      count++
    }
  }
  return count
}

function parseDelimited(text: string, delimiter: string): string[][] {
  const rows: string[][] = []
  let row: string[] = []
  let field = ''
  let inQuotes = false

  for (let index = 0; index < text.length; index++) {
    const char = text[index]!
    if (inQuotes) {
      if (char === '"') {
        if (text[index + 1] === '"') {
          field += '"'
          index++
        } else {
          inQuotes = false
        }
      } else {
        field += char
      }
      continue
    }

    if (char === '"') {
      inQuotes = true
    } else if (char === delimiter) {
      row.push(field)
      field = ''
    } else if (char === '\n') {
      row.push(field)
      rows.push(row)
      row = []
      field = ''
    } else if (char === '\r') {
      if (text[index + 1] !== '\n') {
        row.push(field)
        rows.push(row)
        row = []
        field = ''
      }
    } else {
      field += char
    }
  }

  if (field.length || row.length) {
    row.push(field)
    rows.push(row)
  }

  return rows.filter(cells => cells.some(cell => cell.trim() !== ''))
}

function isHeaderRow(row: string[]): boolean {
  const matches = row.filter(cell => HEADER_PATTERN.test(cell)).length
  return matches >= 2
}

function defaultColumnMap(width: number): ColumnMap {
  return {
    date: 0,
    description: width > 1 ? 1 : -1,
    amount: width > 2 ? 2 : -1,
    credit: -1,
    debit: -1,
    direction: -1,
    fitid: -1
  }
}

function buildColumnMap(header: string[]): ColumnMap {
  const map: ColumnMap = {
    date: columnIndex(header, [/data/i, /date/i, /dia/i, /vencimento/i, /compet[eê]ncia/i]),
    description: columnIndex(header, [/descri/i, /hist[oó]rico/i, /lan[cç]amento/i, /memo/i, /estabelec/i, /favorecid/i, /detalh/i, /t[ií]tulo/i, /title/i]),
    credit: columnIndex(header, [/cr[eé]dito/i, /credit/i, /entrada/i]),
    debit: columnIndex(header, [/d[eé]bito/i, /debit/i, /sa[ií]da/i]),
    amount: columnIndex(header, [/valor/i, /amount/i, /montante/i, /value/i, /quantia/i]),
    direction: columnIndex(header, [/tipo/i, /type/i, /natureza/i, /dire[cç][aã]o/i]),
    fitid: columnIndex(header, [/fitid/i, /documento/i, /autentica/i, /identificador/i])
  }

  if (map.direction >= 0) {
    map.credit = -1
    map.debit = -1
  } else if (map.credit >= 0 || map.debit >= 0) {
    map.amount = -1
  }

  return map
}

function columnIndex(header: string[], patterns: RegExp[]): number {
  for (const pattern of patterns) {
    const index = header.findIndex(cell => pattern.test(cell))
    if (index >= 0) return index
  }
  return -1
}

function buildCsvLine(cells: string[], map: ColumnMap, row: number): ParsedStatementLine {
  const errors: StatementLineError[] = []
  const description = cleanDescription(cellAt(cells, map.description))
  const date = parseStatementDate(cellAt(cells, map.date))
  let amount: number | null = null
  let income = false

  if (map.credit >= 0 || map.debit >= 0) {
    const credit = parseAmountCell(cellAt(cells, map.credit))
    const debit = parseAmountCell(cellAt(cells, map.debit))
    if (credit.amount !== null && Math.abs(credit.amount) > 0) {
      amount = Math.abs(credit.amount)
      income = true
    } else if (debit.amount !== null && Math.abs(debit.amount) > 0) {
      amount = Math.abs(debit.amount)
      income = false
    }
  } else {
    const parsed = parseAmountCell(cellAt(cells, map.amount))
    if (parsed.amount !== null && Math.abs(parsed.amount) > 0) {
      amount = Math.abs(parsed.amount)
      income = resolveIncome(cellAt(cells, map.direction), parsed.direction, parsed.amount)
    }
  }

  if (!description) errors.push('missingDescription')
  if (!date) errors.push('invalidDate')
  if (amount === null) errors.push('invalidAmount')

  return {
    row,
    description,
    amount,
    income,
    date,
    sourceTransactionId: cellAt(cells, map.fitid) || undefined,
    errors
  }
}

function parseOfx(text: string): ParsedStatementLine[] {
  const blocks = [...text.matchAll(/<STMTTRN>([\s\S]*?)<\/STMTTRN>/gi)]
  if (!blocks.length) throw new StatementImportParseError('noTransactions')

  return blocks.map((block, index) => {
    const body = block[1] || ''
    const tag = (name: string) => {
      const match = new RegExp(`<${name}>([^<\\r\\n]*)`, 'i').exec(body)
      return decodeXml(match?.[1]?.trim() || '')
    }

    const description = cleanDescription(tag('MEMO') || tag('NAME') || tag('TRNTYPE'))
    const date = parseOfxDate(tag('DTPOSTED'))
    const signedAmount = normalizeDecimal(tag('TRNAMT'))
    const amount = signedAmount === null ? null : Math.abs(signedAmount)
    const errors: StatementLineError[] = []

    if (!description) errors.push('missingDescription')
    if (!date) errors.push('invalidDate')
    if (amount === null || amount === 0) errors.push('invalidAmount')

    return {
      row: index + 1,
      description,
      amount: amount && amount > 0 ? amount : null,
      income: signedAmount !== null && signedAmount >= 0,
      date,
      sourceTransactionId: tag('FITID') || undefined,
      errors
    }
  })
}

function parseOfxDate(raw: string): string | null {
  const compact = /^(\d{4})(\d{2})(\d{2})/.exec(raw.trim())
  if (compact) return validDate(Number(compact[1]), Number(compact[2]), Number(compact[3]))
  return parseStatementDate(raw)
}

function parseStatementDate(raw: string): string | null {
  const value = raw.trim()
  if (!value) return null

  const iso = /^(\d{4})-(\d{1,2})-(\d{1,2})/.exec(value)
  if (iso) return validDate(Number(iso[1]), Number(iso[2]), Number(iso[3]))

  const brazilian = /^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2,4})/.exec(value)
  if (brazilian) {
    const year = Number(brazilian[3]) < 100 ? Number(brazilian[3]) + 2000 : Number(brazilian[3])
    return validDate(year, Number(brazilian[2]), Number(brazilian[1]))
  }

  const yearFirst = /^(\d{4})[/.](\d{1,2})[/.](\d{1,2})/.exec(value)
  if (yearFirst) return validDate(Number(yearFirst[1]), Number(yearFirst[2]), Number(yearFirst[3]))

  const compact = /^(\d{8})$/.exec(value)
  if (compact) {
    return validDate(
      Number(value.slice(0, 4)),
      Number(value.slice(4, 6)),
      Number(value.slice(6, 8))
    )
  }

  return null
}

function validDate(year: number, month: number, day: number): string | null {
  if (month < 1 || month > 12 || day < 1 || day > 31) return null
  const date = new Date(Date.UTC(year, month - 1, day))
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    return null
  }
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function parseAmountCell(raw: string): { amount: number | null, direction: 'credit' | 'debit' | null } {
  let cell = raw.trim()
  if (!cell) return { amount: null, direction: null }

  let direction: 'credit' | 'debit' | null = null

  const parentheses = /\(([^)]*)\)/.exec(cell)
  if (parentheses) {
    cell = parentheses[1]!
    direction = 'debit'
  }

  const suffix = /^(.*\d.*?)\s*([CD])$/i.exec(cell)
  if (suffix) {
    direction = suffix[2]!.toUpperCase() === 'C' ? 'credit' : 'debit'
    cell = suffix[1]!
  }

  const negative = cell.trim().startsWith('-') || cell.trim().endsWith('-')
  const amount = normalizeDecimal(cell)
  if (amount === null) return { amount: null, direction }
  if (direction === null && negative) direction = 'debit'
  return { amount, direction }
}

function normalizeDecimal(raw: string): number | null {
  let value = raw.trim().replace(/[^\d.,-]/g, '')
  if (!value) return null

  let negative = false
  if (value.startsWith('-')) {
    negative = true
    value = value.slice(1)
  }
  if (value.endsWith('-')) {
    negative = true
    value = value.slice(0, -1)
  }
  value = value.replace(/-/g, '')
  if (!value) return null

  const comma = value.lastIndexOf(',')
  const dot = value.lastIndexOf('.')
  let normalized: string

  if (comma >= 0 && dot >= 0) {
    normalized = comma > dot
      ? value.replace(/\./g, '').replace(',', '.')
      : value.replace(/,/g, '')
  } else if (comma >= 0) {
    const fraction = value.length - comma - 1
    normalized = fraction > 0 && fraction <= 2
      ? value.replace(/\./g, '').replace(',', '.')
      : value.replace(/,/g, '')
  } else {
    const parts = value.split('.')
    normalized = parts.length > 2
      ? parts.slice(0, -1).join('') + '.' + parts[parts.length - 1]
      : value
  }

  const amount = Number(normalized)
  if (!Number.isFinite(amount)) return null
  return negative ? -amount : amount
}

function resolveIncome(rawDirection: string, amountDirection: 'credit' | 'debit' | null, signedAmount: number): boolean {
  const direction = directionFromType(rawDirection)
  if (direction !== null) return direction === 'credit'
  if (amountDirection !== null) return amountDirection === 'credit'
  return signedAmount >= 0
}

function directionFromType(raw: string): 'credit' | 'debit' | null {
  const value = raw.trim().toLowerCase()
  if (!value) return null
  if (/^(c|cr|cred|credit|cr[eé]dito|entrada|receita|income|deposito|dep[oó]sito|deposit)$/.test(value)) return 'credit'
  if (/^(d|db|deb|debit|d[eé]bito|saida|sa[ií]da|despesa|expense|pagamento|payment)$/.test(value)) return 'debit'
  if (/cr[eé]dito|credit|entrada|receita|income/.test(value)) return 'credit'
  if (/d[eé]bito|debit|sa[ií]da|despesa|expense/.test(value)) return 'debit'
  return null
}

function cellAt(cells: string[], index: number): string {
  return index >= 0 ? (cells[index] ?? '').trim() : ''
}

function cleanDescription(value: string): string {
  return value.trim().replace(/\s+/g, ' ').slice(0, 300)
}

function decodeXml(value: string): string {
  return value
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&apos;/gi, '\'')
}
