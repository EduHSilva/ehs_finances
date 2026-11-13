# Regras de negócio — EHS Finanças

> Controle financeiro pessoal e do negócio. Plataforma (auth, roles, isolamento):
> `D:/Projetos/ehs_microservices/regras-negocio.md`.

## Contas

- Tipos: `PF` (pessoal) e `PJ` (negócio). O tipo é só rótulo/visibilidade — não muda
  permissões nem cálculos.
- Um usuário pode ter várias contas. Ao listar ou ao lançar algo sem conta, o backend
  garante uma conta pessoal padrão (`PF-<userId>`, "Conta pessoal", saldo 0).
- Não existe conta "ativa" no servidor: a seleção é local no cliente
  (cookie `active-financial-account-<userId>`).
- Saldo = saldo inicial + receitas − despesas **confirmadas** com data anterior a hoje.
- Não há exclusão de conta (só criação/listagem/consulta). Moeda é fixa em `BRL`.

## Categorias

- Categorias são do **usuário** (não da conta).
- Existem 16 categorias padrão criadas automaticamente (receitas e despesas), com
  classificação `ESSENTIAL`, `RECREATION` ou `INVESTMENT`.
- Cada categoria tem direção (receita/despesa) e um `goal` (% da receita); para receita
  o `goal` é sempre 0.
- Renomear uma categoria padrão a marca como `customized`.
- Exclusão é soft delete; lançamentos antigos continuam apontando para ela (oculta).

## Lançamentos (transações)

- Status: `OK`, `PENDING`, `REVIEW`, `DELAY`, `DELETED`. `DELAY` nunca é atribuído hoje.
- Valores são sempre positivos; o sinal vem do campo `income` (receita × despesa).
  Valor mínimo 0,01, título obrigatório, data de execução obrigatória.
- Só `OK` entra no saldo; `PENDING`/`REVIEW` não contam até a confirmação.
- A categoria precisa ter a mesma direção do lançamento.
- Na edição, categoria e direção só podem ser trocadas se o lançamento ainda não tem
  categoria. `DELETED` no `PUT` é rejeitado — exclusão é só pelo `DELETE`.
- Confirmar um `PENDING` aceita um novo valor (> 0) e passa para `OK`; em parcela de
  recorrência, avança o contador e marca a recorrência como `PAYED` na última.
- Lançamentos importados nascem `REVIEW` e vão para `OK` na aprovação (com categoria
  opcional) ou soft delete na rejeição.

## Recorrências

- Tipos: `INSTALLMENT` (número de parcelas) e `MONTH` (todo mês).
- Status: `ACTIVE`, `PENDING`, `INACTIVE`, `PAYED`, `DELETED`.
- O dia do mês é 1–31 e é ajustado para o último dia em meses curtos.
- As ocorrências são geradas sob demanda como lançamentos `PENDING`, uma por mês,
  olhando até 12 meses atrás; parcelamento respeita o total de parcelas.
- Confirmar a ocorrência pode alterar o valor **somente daquele lançamento**; o modelo
  da recorrência não muda. Se o mês já tem ocorrência, não duplica.
- Parar = status `INACTIVE`. Excluir a recorrência remove (soft delete) as ocorrências
  pendentes; as confirmadas permanecem.

## Metas

- Modo `FIXED` (valor fixo) ou `PERCENT_INCOME` (% da receita confirmada do mês, ≤ 100).
- Campos: valor (≥ 0,01), saldo inicial (≥ 0), objetivo opcional (saldo-alvo) e período
  (mês, ou intervalo início/fim). Fim não pode ser anterior ao início.
- Com intervalo, a meta vale para todos os meses sobrepostos; sem intervalo, só para o
  mês informado.
- Esperado = valor fixo ou % da receita confirmada; realizado = soma dos aportes do mês
  (excluindo ajustes); progresso = realizado ÷ esperado (0 quando esperado = 0).
- Aporte pode vincular um lançamento existente (despesa `OK`, mesma conta, valor e data)
  ou criar um automaticamente na categoria de investimentos ("Aporte: <meta>").
  Só lançamentos do próprio usuário são aceitos (id de outro usuário responde 404).
- Aporte do tipo **ajuste** (ex.: rendimento) não gera lançamento: soma ao saldo/objetivo
  da meta, mas não entra no realizado do mês. Ajuste não pode vincular lançamento.
- Apagar aporte/meta apaga os lançamentos gerados por ele.
- Saldo inicial só é editável enquanto a meta não tem aportes ("meta zerada" = ainda sem
  aportes); não existe estado "concluída" automático.

## Importação de extrato

- CSV/OFX, até 1000 linhas e 5 MB, com prévia antes de confirmar.
- Duplicata = mesma conta + identificador estável da linha (id da transação ou
  data/valor/direção/descrição normalizada), via hash; duplicatas são ignoradas, nunca
  atualizadas. Existe índice único por usuário/conta/chave.
- A checagem de duplicidade ignora linhas `DELETED`: reimportar um extrato cujas linhas
  foram apagadas reaproveita a mesma linha (volta para `REVIEW`) em vez de bloquear.
- Cada linha importada gera auditoria (`IMPORTED`, `APPROVED`, `DISCARDED`).

## Preferências

- `showBusinessAccount` (padrão `true`) apenas **oculta** contas PJ na interface; não é
  autorização — a API continua devolvendo todas as contas do usuário.
- Moeda: só `BRL` é aceita hoje.

## Projeção e painéis

- `/dashboard/projection`: horizonte de 30, 90 (padrão) ou 180 dias; parte do saldo
  atual, soma lançamentos futuros `OK`, recorrências `MONTH` ativas e parcelas restantes,
  e estima o fluxo variável pela média de 6 meses. Confiança: alta (≥ 24 amostras),
  média (≥ 6), baixa (menos).
- `/dashboard/pending`: pendências dos próximos N dias (padrão 30, máx. 365) e
  materializa as recorrências antes.
- `/dashboard/investments`: aportes (despesas) e resgates (receitas) da classificação
  `INVESTMENT`, com líquido.
