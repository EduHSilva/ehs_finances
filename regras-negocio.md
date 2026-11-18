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
- Saldo = saldo inicial + receitas − despesas **confirmadas** com data anterior a hoje.
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
  metas, importação de extrato e painéis.
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
  Só lançamentos visíveis ao usuário (próprios ou de conta compartilhada) são aceitos;
  id fora do escopo responde 404.
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
