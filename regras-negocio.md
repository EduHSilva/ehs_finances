# Regras de negócio — EHS Finanças

> Controle financeiro pessoal e do negócio. Plataforma (auth, roles, isolamento):
> `D:/Projetos/ehs_microservices/regras-negocio.md`.

## Marca

- Nome exibido na web e no app: **Finanças**.
- Cor de marca indigo (`#4F46E5`, dark `#818CF8`) sobre neutros zinc, igual nos dois
  clientes; ícone próprio (gráfico ascendente), sem reaproveitar o ícone de outro app.
- Web (sidebar, login/cadastro) e app (login e perfil) exibem "Desenvolvido por EHS
  Solutions".

## Contas

- Tipos: `PF` (pessoal) e `PJ` (negócio). O tipo é só rótulo/visibilidade — não muda
  permissões nem cálculos.
- Um usuário pode ter várias contas. Ao listar ou ao lançar algo sem conta, o backend
  garante uma conta pessoal padrão (`PF-<userId>`, "Conta pessoal", saldo 0).
- Não existe conta "ativa" no servidor: a seleção é local no cliente
  (cookie `active-financial-account-<userId>`).
- Saldo (caixa) = saldo inicial + entradas − saídas **confirmadas** com data até hoje,
  somando lançamentos `NORMAL`, `TRANSFER` e `INVOICE_PAYMENT`; compras `CARD` só saem do
  caixa quando a fatura é paga. "Até hoje" inclui o dia inteiro: o corte é o início de
  amanhã (UTC), não o instante atual — lançamentos de hoje já entram no saldo.
- Cartão de crédito **não** é um tipo de conta e não entra no seletor de conta: é uma
  forma de pagamento vinculada à conta (ver "Cartões de crédito").
- Não há exclusão de conta (só criação/listagem/consulta). Moeda é fixa em `BRL`.

## Conta compartilhada

- Quem criou a conta é o **proprietário** (`role = OWNER`); convidados aceitos viram
  membros (`role = MEMBER`). Só o proprietário gerencia convites e membros.
- Convite = e-mail + linha em `finance_account_member` com status `PENDING`, token único
  e validade configurável (padrão 7 dias, `FINANCES_INVITE_TTL_HOURS`), enviado por
  e-mail com o link `<SERVER_URL>/app/finances/?invite=<token>`.
- Aceite: pelo link/token (web) ou pelo id do convite (app); o aceite por id só vale
  para o usuário logado convidado (id do usuário ou e-mail confirmado no `users`).
  Recusar → `DECLINED`; vencido → `EXPIRED` (avaliado a cada consulta). O proprietário
  pode reenviar (novo token/validade) ou cancelar um convite, e remover um membro
  `ACTIVE` revoga o acesso.
- Membro `ACTIVE` tem acesso **total** à conta: ver e editar lançamentos, recorrências,
  metas, cartões, transferências, faturas, importação de extrato e painéis.
- Dados criados por um membro são gravados com o `user_id` do **proprietário**; ao ser
  revogado, o membro deixa de acessá-los.
- Categorias continuam por usuário: cada um vê as próprias e as do proprietário da conta
  compartilhada; o proprietário também vê as criadas pelos membros.
- A listagem de contas devolve o `role` de cada conta; a conta ativa continua sendo
  escolhida localmente em cada dispositivo.

## Categorias

- Categorias são do **usuário** (não da conta), com as exceções de visibilidade das
  contas compartilhadas descritas acima.
- Existem 16 categorias padrão criadas automaticamente (receitas e despesas), com
  classificação `ESSENTIAL`, `RECREATION` ou `INVESTMENT`.
- Cada categoria tem direção (receita/despesa) e um `goal` (% da receita); para receita
  o `goal` é sempre 0.
- Renomear uma categoria padrão a marca como `customized`.
- Exclusão é soft delete; lançamentos antigos continuam apontando para ela (oculta).
- Nas telas, receitas aparecem primeiro, um separador e depois as despesas; dentro de cada
  grupo a ordem é meta (%) decrescente e depois título alfabético. A API devolve na mesma
  ordem (receitas primeiro, meta decrescente, título).
- As despesas são exibidas em grupos por classificação — Essencial → Investimento → Lazer —
  cada um com subtotal da soma das metas (%) das categorias do grupo.

## Lançamentos (transações)

- Status: `OK`, `PENDING`, `REVIEW`, `DELAY`, `DELETED`. `DELAY` nunca é atribuído hoje.
- Tipo de movimento (`entryType`): `NORMAL` (caixa, padrão), `CARD` (compra no cartão),
  `TRANSFER` (perna de transferência) e `INVOICE_PAYMENT` (pagamento de fatura). O tipo
  decide se o lançamento entra no saldo e nos relatórios.
- Valores são sempre positivos; o sinal vem do campo `income` (receita × despesa).
  Valor mínimo 0,01, título obrigatório, data de execução obrigatória.
- Só `OK` entra no saldo (respeitando o tipo de movimento); `PENDING`/`REVIEW` não contam
  até a confirmação.
- A categoria precisa ter a mesma direção do lançamento.
- Categoria e direção podem ser trocadas a qualquer momento na edição. Em lançamento
  vinculado a recorrência, a troca atualiza a categoria da recorrência e das ocorrências
  pendentes; as confirmadas mantêm o histórico. `DELETED` no `PUT` é rejeitado — exclusão
  é só pelo `DELETE`.
- A listagem é ordenada por data de execução, mais recente primeiro.
- Confirmar um `PENDING` aceita um novo valor (> 0) e passa para `OK`; em parcela de
  recorrência, avança o contador e marca a recorrência como `PAYED` na última.
- Lançamentos importados nascem `REVIEW` e vão para `OK` na aprovação (com categoria
  opcional) ou soft delete na rejeição.
- `TRANSFER` e `INVOICE_PAYMENT` não passam pelo CRUD de lançamentos: são criados,
  alterados e excluídos pelos endpoints de transferência/fatura; `PUT` e `DELETE` em
  `/transactions/{id}` rejeitam esses tipos.
- Compra `CARD` pertence à conta do cartão e aceita categoria/direção como qualquer
  despesa; no v1 é sempre despesa (`income = false`) — estorno é editar/excluir a compra.
  Remover o cartão transforma o lançamento em `NORMAL` (volta a afetar o caixa).
- Compra `CARD` de ciclo totalmente pago fica bloqueada para edição/exclusão enquanto o
  pagamento existir; alterações em ciclo parcialmente pago não podem deixar o total do
  ciclo abaixo do valor já pago.

## Cartões de crédito

- Cartão pertence a uma **conta financeira**: nome, limite opcional, dia de fechamento e
  dia de vencimento (1–31, ajustados ao último dia em meses curtos). O cartão só aceita
  lançamentos/recorrências da própria conta.
- Ciclo: fecha no `closingDay` de cada mês e recebe as compras com data até o fechamento
  (compra no próprio dia entra nele). O vencimento é o primeiro `dueDay` **após** o
  fechamento; a fatura é identificada pelo mês do vencimento (`AAAA-MM`).
- Fatura = compras `CARD` `OK` do ciclo. v1 é só despesa (sem estorno).
- Pagamento: lançamento `INVOICE_PAYMENT` na conta, sem categoria e `income = false`, na
  data do pagamento. Valor > 0 e ≤ restante; pagamentos parciais são permitidos e o
  excedente é rejeitado. Fatura fica `PAID` quando pago ≥ total; senão `OPEN`/`PARTIAL`.
- Fatura com compra `CARD` `PENDING` no ciclo não pode ser paga até a compra ser
  confirmada ou excluída (senão a confirmação reabriria a fatura).
- O pagamento não é despesa de categoria: fica fora de `monthNet`, `topCategories`, custo
  de vida e investimentos; só move o saldo. As compras seguem contando pela data da compra.
- Excluir um pagamento reabre a fatura e libera a edição das compras do ciclo.
- Mudar fechamento/vencimento é bloqueado se o cartão já tem pagamento (ciclos históricos
  não são recalculados).
- Excluir cartão (soft delete) exige fatura sem saldo em aberto. Compras antigas ficam no
  histórico (relatórios) e nunca mais afetam o caixa.
- Limite é informativo (uso = faturas abertas ÷ limite); não bloqueia compras.
- Recorrência pode ser vinculada a um cartão da conta; as ocorrências nascem `CARD`
  (ver "Recorrências").
- Saldo inicial opcional na **criação** do cartão: vira uma compra `CARD` `OK` com a data
  de hoje (portanto no ciclo da fatura em aberto), sem categoria e título
  "Saldo inicial do cartão". Não é campo editável — para corrigir, edite/exclua a compra.

## Transferências entre contas

- Transferência move valor entre **duas contas** do usuário: par de lançamentos
  `TRANSFER` com o mesmo `transferId` — saída na origem, entrada no destino, mesma data,
  valor e status (`OK`; data futura é agendada e entra só na projeção).
- Contas precisam ser diferentes, acessíveis ao usuário (dono ou membro ativo) e em BRL.
- Não é receita nem despesa: fica fora de `monthNet`, `topCategories`, custo de vida,
  investimentos, médias variáveis e vínculo de aporte; afeta o saldo das duas contas.
- Editar/excluir opera no par: `PUT /transfers/{id}` altera valor/data/título/status das
  duas pernas; `DELETE /transfers/{id}` remove as duas (soft delete). Origem/destino não
  mudam — recrie a transferência.
- Não existe transferência a partir de/para cartão nem entre moedas.
- As pernas aparecem no extrato comum identificadas como transferência e com a conta
  contraparte.
- Importação de extrato nunca cria transferências.

## Recorrências

- Tipos: `INSTALLMENT` (número de parcelas) e `MONTH` (todo mês).
- Status: `ACTIVE`, `PENDING`, `INACTIVE`, `PAYED`, `DELETED`.
- O dia do mês é 1–31 e é ajustado para o último dia em meses curtos.
- A recorrência pode ser vinculada a um cartão da mesma conta; as ocorrências nascem
  `CARD`. Trocar/remover o cartão atualiza as ocorrências pendentes (as confirmadas
  mantêm o histórico), como acontece com a categoria.
- A categoria pode ser editada a qualquer momento; a troca atualiza as ocorrências
  pendentes e futuras (as confirmadas mantêm o histórico).
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
  Só lançamentos visíveis ao usuário (próprios ou de conta compartilhada) são aceitos;
  id fora do escopo responde 404.
- Aporte vinculado só aceita lançamento `NORMAL` confirmado; `CARD`, `TRANSFER` e
  `INVOICE_PAYMENT` não podem ser vinculados.
- Aporte do tipo **ajuste** (ex.: rendimento) não gera lançamento: soma ao saldo/objetivo
  da meta, mas não entra no realizado do mês. Ajuste não pode vincular lançamento.
- Apagar aporte/meta apaga os lançamentos gerados por ele.
- Saldo inicial só é editável enquanto a meta não tem aportes ("meta zerada" = ainda sem
  aportes); não existe estado "concluída" automático.
- `reserve`: no máximo uma meta por conta pode ser marcada como reserva; marcar outra
  desmarca a anterior. A reserva não é o caixa: é o valor acumulado da meta (saldo inicial +
  aportes/ajustes) e alimenta os "meses de reserva" na dashboard.
- Relatório "Metas por categoria": agrupa as categorias de despesa com lançamentos no mês
  por classificação (Essencial/Investimento/Lazer); cada grupo mostra a meta somada (%) e o
  % realizado sobre as entradas (mesma base da meta), além do subtotal realizado ÷ esperado.
  Categorias sem lançamentos no mês não aparecem.
- Telas (web e app): a aba **Investimentos** lista as metas mensais (esperado, realizado,
  aportes e objetivo); a aba **Metas** mostra só o relatório por categoria, com um card no
  topo destacando o % realizado das entradas em essenciais, lazer e investimentos.

## Importação de extrato

- CSV/OFX, até 1000 linhas e 5 MB, com prévia antes de confirmar.
- Duplicata = mesma conta + identificador estável da linha (id da transação ou
  data/valor/direção/descrição normalizada), via hash; duplicatas são ignoradas, nunca
  atualizadas. Existe índice único por usuário/conta/chave.
- A checagem de duplicidade ignora linhas `DELETED`: reimportar um extrato cujas linhas
  foram apagadas reaproveita a mesma linha (volta para `REVIEW`) em vez de bloquear.
- Cada linha importada gera auditoria (`IMPORTED`, `APPROVED`, `DISCARDED`).
- Linhas importadas são sempre `NORMAL` (nunca `CARD`, `TRANSFER` ou `INVOICE_PAYMENT`).
- Pagamentos de fatura lançados pelo app não são casados automaticamente com linhas do
  extrato no v1: a linha duplicada deve ser descartada na revisão.

## Preferências

- `showBusinessAccount` (padrão `true`) apenas **oculta** contas PJ na interface; não é
  autorização — a API continua devolvendo todas as contas do usuário.
- Moeda: só `BRL` é aceita hoje.

## Projeção e painéis

- `/dashboard/projection`, `/dashboard/pending` e `/dashboard/summary` aceitam
  `month=YYYY-MM` (mês atual ou futuro, até 12 meses à frente): o horizonte vai até o
  último dia do mês escolhido. Sem `month` vale o parâmetro `days` (limites de cada rota).
- `/dashboard/projection`: parte do saldo
  atual, soma lançamentos futuros `OK`, recorrências `MONTH` ativas e parcelas restantes,
  e estima o fluxo variável pela média de 6 meses. Compras `CARD` entram pelo vencimento
  da fatura (ciclo já pago fica de fora; fatura aberta atrasada entra como saída imediata);
  `TRANSFER` entra na própria data; a média variável ignora `TRANSFER` e
  `INVOICE_PAYMENT`. Confiança: alta (≥ 24 amostras), média (≥ 6), baixa (menos).
- `/dashboard/pending`: pendências no horizonte (padrão 30 dias, máx. 365) e
  materializa as recorrências antes. Inclui compras `CARD` pendentes como `TRANSACTION`,
  faturas abertas como `INVOICE` (vencimento na janela ou atrasadas, exibidas com a data
  de hoje) e transferências futuras como `TRANSFER`.
- `/dashboard/investments`: aportes (despesas) e resgates (receitas) da classificação
  `INVESTMENT`, com líquido.
- `/dashboard/summary`: agrega o mês corrente e o horizonte de projeção. O bloco "mês"
  (resultado, categorias, custo de vida) continua sempre no mês corrente, mesmo com outro
  mês selecionado; só a projeção acompanha o `month`.
  - Mês: receitas e despesas até hoje por competência (`NORMAL` + `CARD`); `TRANSFER` e
    `INVOICE_PAYMENT` ficam fora. `monthNet = receitas − despesas` (resultado do mês).
  - Cartões: `cards.openInvoices` traz o total e o detalhe por cartão das faturas com
    saldo em aberto até hoje (compras `OK` até hoje menos pagamentos).
  - Investimentos: `total` = líquido `INVESTMENT` de todos os tempos + `initialBalance` das
    metas + contribuições de ajuste de meta. `monthContributions`/`monthWithdrawals` são do
    mês corrente. Projeção = total + média do líquido `INVESTMENT` dos 3 meses completos
    anteriores dividida pelos meses dessa janela que têm dados (até 3; sem dados = 0)
    × dias/30, nunca negativa.
  - Custo médio de vida: soma das despesas `ESSENTIAL` dos 6 meses completos anteriores ao
    mês corrente, dividida pela quantidade de meses dessa janela que têm dados (1–6).
    `livingCost.months` é essa quantidade. Com menos de 3 meses de dados, `monthlyAverage`
    vem nulo e web/app mostram "continue usando o sistema" no lugar do valor.
  - `livingCost.reserve`: meta marcada como reserva (`total` = acumulado da meta desde o
    início; `months` = total ÷ custo médio de vida, 1 casa). Sem meta marcada ou sem custo
    médio suficiente, `reserve.months` vem nulo e a dashboard mostra "—".
  - `topCategories`: 5 categorias com maior despesa no mês corrente, com participação %
    sobre as despesas do mês.
  - Patrimônio: `current = saldo em caixa + total investido − faturas em aberto`;
    `projected = saldo projetado + investimentos projetados` (o saldo projetado já inclui
    os pagamentos de fatura previstos).
