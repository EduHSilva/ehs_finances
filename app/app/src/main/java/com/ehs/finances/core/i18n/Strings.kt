package com.ehs.finances.core.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val tag: String) {
    PT_BR("pt-BR"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = if (tag == EN.tag) EN else PT_BR
        fun toggle(current: AppLanguage): AppLanguage = if (current == PT_BR) EN else PT_BR
    }
}

/** Textos do app em pt-BR e en. Chaves ausentes caem para o próprio identificador. */
class Strings(val language: AppLanguage = AppLanguage.PT_BR) {
    val locale: Locale = Locale.forLanguageTag(language.tag)

    operator fun get(key: String): String = table[language]?.get(key) ?: key

    fun format(key: String, vararg params: Pair<String, Any>): String {
        val template = get(key)
        return params.fold(template) { acc, (name, value) ->
            acc.replace("{$name}", value.toString())
        }
    }

    private companion object {
        val table = mapOf(
            AppLanguage.PT_BR to ptBr,
            AppLanguage.EN to enStrings,
        )
    }
}

val LocalStrings = staticCompositionLocalOf { Strings() }

private val ptBr: Map<String, String> = mapOf(
    "appName" to "EHS Finances",
    "menu" to "Menu",
    "goBack" to "Voltar",
    "close" to "Fechar",
    "save" to "Salvar",
    "cancel" to "Cancelar",
    "delete" to "Excluir",
    "edit" to "Editar",
    "add" to "Adicionar",
    "retry" to "Tentar novamente",
    "loading" to "Carregando…",
    "empty" to "Nada por aqui ainda.",
    "all" to "Todas",
    "confirmDeleteTitle" to "Confirmar exclusão",
    "confirmDeleteText" to "Deseja realmente excluir este registro?",
    "comingSoon" to "EM BREVE",

    // Auth
    "loginTitle" to "Acessar conta",
    "loginSubtitle" to "Entre para continuar para o seu painel financeiro.",
    "registerTitle" to "Criar conta",
    "registerSubtitle" to "Comece a organizar suas finanças em poucos minutos.",
    "landingTitle" to "Suas finanças, no controle.",
    "landingSubtitle" to "Acompanhe receitas, despesas e recorrências em um só lugar, com uma visão clara do seu mês.",
    "landingPointOne" to "Resumo mensal de receitas, despesas e saldo",
    "landingPointTwo" to "Categorias com metas de gasto",
    "landingPointThree" to "Transações e recorrências organizadas",
    "emailLabel" to "E-mail",
    "emailPlaceholder" to "voce@email.com",
    "passwordLabel" to "Senha",
    "usernameLabel" to "Nome de usuário",
    "usernamePlaceholder" to "Como devemos te chamar?",
    "loginAction" to "Entrar",
    "loggingIn" to "Entrando…",
    "registerAction" to "Criar conta",
    "registering" to "Criando…",
    "needAccount" to "Ainda não tem conta?",
    "haveAccount" to "Já tem uma conta?",
    "goToRegister" to "Criar conta",
    "goToLogin" to "Fazer login",
    "registerSuccessTitle" to "Conta criada!",
    "registerSuccessText" to "Enviamos um e-mail de confirmação. Verifique sua caixa de entrada para ativar a conta e depois faça login.",
    "loginNow" to "Entrar",
    "showPassword" to "Mostrar senha",
    "hidePassword" to "Ocultar senha",
    "invalidEmail" to "Informe um e-mail válido.",
    "passwordTooShort" to "A senha deve ter pelo menos 6 caracteres.",
    "usernameTooShort" to "O nome de usuário deve ter pelo menos 2 caracteres.",
    "loginFailed" to "Não foi possível entrar. Verifique suas credenciais.",
    "registerFailed" to "Não foi possível criar a conta. Tente novamente.",

    // Navegação
    "dashboard" to "Visão geral",
    "transactions" to "Transações",
    "categories" to "Categorias",
    "recurrences" to "Recorrências",
    "profile" to "Perfil",

    // Dashboard
    "dashboardSubtitle" to "Um resumo do seu mês financeiro.",
    "monthIncome" to "Receitas",
    "monthExpense" to "Despesas",
    "monthBalance" to "Saldo",
    "savingsRate" to "Taxa de poupança",
    "savingsRateCaption" to "Do que entrou, quanto sobrou.",
    "recentTransactions" to "Transações recentes",
    "seeAll" to "Ver todas",
    "byCategory" to "Gastos por categoria",
    "noTransactions" to "Nenhuma transação neste mês.",
    "dashboardLoading" to "Carregando seu resumo…",
    "dashboardError" to "Não foi possível carregar o resumo.",
    "income" to "Receita",
    "expense" to "Despesa",

    // Transações
    "transactionsSubtitle" to "Todas as receitas e despesas registradas.",
    "newTransaction" to "Nova transação",
    "editTransaction" to "Editar transação",
    "transactionTitleLabel" to "Título",
    "transactionTitlePlaceholder" to "Ex.: Mercado do mês",
    "valueLabel" to "Valor",
    "dateLabel" to "Data",
    "typeLabel" to "Tipo",
    "categoryLabel" to "Categoria",
    "selectCategory" to "Selecione uma categoria",
    "noCategoriesHint" to "Cadastre uma categoria antes de criar transações.",
    "transactionSaved" to "Transação salva!",
    "transactionDeleted" to "Transação excluída.",
    "deleteTransactionConfirm" to "Excluir esta transação?",
    "transactionSaveFailed" to "Não foi possível salvar a transação.",
    "transactionDeleteFailed" to "Não foi possível excluir a transação.",
    "titleRequired" to "Informe o título.",
    "valueInvalid" to "Informe um valor maior que zero.",
    "categoryRequired" to "Selecione uma categoria.",
    "dateInvalid" to "Informe uma data válida.",
    "filterMonth" to "Mês",
    "filterAll" to "Todas",

    // Categorias
    "categoriesSubtitle" to "Organize seus gastos e defina metas por categoria.",
    "newCategory" to "Nova categoria",
    "editCategory" to "Editar categoria",
    "categoryTitleLabel" to "Nome da categoria",
    "categoryTitlePlaceholder" to "Ex.: Casa",
    "colorLabel" to "Cor",
    "goalLabel" to "Meta (%)",
    "classificationLabel" to "Classificação",
    "essential" to "Essencial",
    "recreation" to "Lazer",
    "categorySaved" to "Categoria salva!",
    "categoryDeleted" to "Categoria excluída.",
    "noCategories" to "Nenhuma categoria cadastrada.",
    "deleteCategoryConfirm" to "Excluir esta categoria? As transações vinculadas podem ficar sem categoria.",
    "categorySaveFailed" to "Não foi possível salvar a categoria.",
    "categoryDeleteFailed" to "Não foi possível excluir a categoria.",
    "goalInvalid" to "A meta deve estar entre 0 e 100.",
    "goalCaption" to "Percentual do orçamento mensal.",

    // Recorrências
    "recurrencesSubtitle" to "Contas fixas e compras parceladas do mês.",
    "newRecurrence" to "Nova recorrência",
    "editRecurrence" to "Editar recorrência",
    "recurrenceTitleLabel" to "Título",
    "recurrenceTitlePlaceholder" to "Ex.: Aluguel",
    "recurrenceTypeLabel" to "Tipo",
    "monthType" to "Mensal",
    "installmentType" to "Parcelado",
    "dayLabel" to "Dia do mês",
    "installmentsLabel" to "Parcelas",
    "payedLabel" to "Pagas",
    "noRecurrences" to "Nenhuma recorrência cadastrada.",
    "recurrenceSaved" to "Recorrência salva!",
    "recurrenceDeleted" to "Recorrência excluída.",
    "deleteRecurrenceConfirm" to "Excluir esta recorrência?",
    "recurrenceSaveFailed" to "Não foi possível salvar a recorrência.",
    "recurrenceDeleteFailed" to "Não foi possível excluir a recorrência.",
    "dayInvalid" to "O dia deve estar entre 1 e 31.",
    "installmentsInvalid" to "Informe a quantidade de parcelas.",
    "payedInvalid" to "As parcelas pagas não podem passar do total.",

    // Perfil / configurações
    "profileSubtitle" to "Gerencie seus dados de acesso e preferências.",
    "profileNameLabel" to "Nome de usuário",
    "profileEmailLabel" to "E-mail",
    "profileSave" to "Salvar alterações",
    "profileSaving" to "Salvando…",
    "profileSaveSuccess" to "Perfil atualizado!",
    "profileSaveFailed" to "Não foi possível salvar o perfil.",
    "preferences" to "Preferências",
    "themeLabel" to "Tema",
    "themeDarkAction" to "Ativar modo escuro",
    "themeLightAction" to "Ativar modo claro",
    "languageLabel" to "Idioma",
    "logoutAction" to "Sair da conta",
    "versionLabel" to "Versão",
)
