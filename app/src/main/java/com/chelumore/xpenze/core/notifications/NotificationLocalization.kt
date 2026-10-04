package com.chelumore.xpenze.core.notifications

import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.core.growth.PremiumOpportunityType
import com.chelumore.xpenze.core.growth.buildMochiPromptForOpportunity
import com.chelumore.xpenze.core.growth.formatPremiumOpportunity
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.RecurringRule
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal enum class NotificationChannelKind {
    BUDGETS,
    DAILY,
    MOCHI,
    AUTO_CAPTURE
}

internal data class NotificationChannelCopy(
    val name: String,
    val description: String
)

internal data class LocalizedNotificationCopy(
    val title: String,
    val body: String
)

internal data class MochiNotificationContent(
    val title: String,
    val body: String,
    val prompt: String
)

internal enum class MochiConversationTopic {
    BUDGETS,
    CASH_FLOW,
    RECURRING,
    CATEGORY_CLEANUP,
    SAVINGS,
    GENERAL
}

private val historyJson = Json { ignoreUnknownKeys = true; isLenient = true }

private data class BudgetPressureCandidate(
    val budget: Budget,
    val usedPercent: Int,
    val spentCents: Long
)

internal fun notificationChannelCopy(
    kind: NotificationChannelKind,
    languageTag: String
): NotificationChannelCopy {
    return when (kind) {
        NotificationChannelKind.BUDGETS -> NotificationChannelCopy(
            name = pickLanguage(
                languageTag,
                en = "Budget alerts",
                lv = "Budžeta brīdinājumi",
                es = "Alertas de presupuesto",
                fr = "Alertes budget",
                de = "Budgetwarnungen",
                pt = "Alertas de orçamento",
                ar = "تنبيهات الميزانية",
                hi = "बजट अलर्ट",
                ja = "予算アラート",
                sw = "Arifa za bajeti",
                zh = "预算提醒"
            ),
            description = pickLanguage(
                languageTag,
                en = "Alerts when a budget reaches its warning level.",
                lv = "Brīdina, kad budžets sasniedz brīdinājuma līmeni.",
                es = "Alertas cuando un presupuesto llega a su nivel de aviso.",
                fr = "Alertes lorsqu'un budget atteint son niveau d'avertissement.",
                de = "Warnungen, wenn ein Budget seinen Hinweiswert erreicht.",
                pt = "Alertas quando um orçamento atinge o nível de aviso.",
                ar = "تنبيهات عندما تصل الميزانية إلى مستوى التحذير.",
                hi = "जब कोई बजट अपनी चेतावनी सीमा पर पहुंचता है तब अलर्ट।",
                ja = "予算が警告レベルに達したときの通知です。",
                sw = "Arifa bajeti inapofika kiwango cha onyo.",
                zh = "当预算达到预警水平时提醒。"
            )
        )

        NotificationChannelKind.DAILY -> NotificationChannelCopy(
            name = pickLanguage(
                languageTag,
                en = "Daily summary",
                lv = "Dienas kopsavilkums",
                es = "Resumen diario",
                fr = "Résumé quotidien",
                de = "Tagesübersicht",
                pt = "Resumo diário",
                ar = "الملخص اليومي",
                hi = "दैनिक सारांश",
                ja = "デイリー要約",
                sw = "Muhtasari wa kila siku",
                zh = "每日摘要"
            ),
            description = pickLanguage(
                languageTag,
                en = "Daily money check-ins and reminders.",
                lv = "Ikdienas naudas reģistrēšanās un atgādinājumi.",
                es = "Recordatorios y revisiones diarias de tu dinero.",
                fr = "Rappels et bilans quotidiens de vos finances.",
                de = "Tägliche Geld-Check-ins und Erinnerungen.",
                pt = "Lembretes e revisões diárias do seu dinheiro.",
                ar = "تذكيرات ومراجعات مالية يومية.",
                hi = "रोज़ाना पैसों की जाँच और रिमाइंडर।",
                ja = "毎日のお金の確認とリマインダーです。",
                sw = "Ukaguzi na vikumbusho vya fedha vya kila siku.",
                zh = "每日资金回顾与提醒。"
            )
        )

        NotificationChannelKind.MOCHI -> NotificationChannelCopy(
            name = pickLanguage(
                languageTag,
                en = "Mochi insights",
                lv = "Moči ieskati",
                es = "Ideas de Mochi",
                fr = "Insights de Mochi",
                de = "Mochi-Einblicke",
                pt = "Insights do Mochi",
                ar = "رؤى Mochi",
                hi = "Mochi इनसाइट्स",
                ja = "Mochiの気づき",
                sw = "Maarifa ya Mochi",
                zh = "Mochi 洞察"
            ),
            description = pickLanguage(
                languageTag,
                en = "Useful money nudges and opportunities from Mochi.",
                lv = "Noderīgi Mochi naudas pamudinājumi un iespējas.",
                es = "Recordatorios útiles y oportunidades detectadas por Mochi.",
                fr = "Conseils utiles et opportunités détectées par Mochi.",
                de = "Nützliche Geldhinweise und Chancen von Mochi.",
                pt = "Lembretes úteis e oportunidades encontradas pelo Mochi.",
                ar = "تنبيهات وفرص مالية مفيدة من Mochi.",
                hi = "Mochi की उपयोगी पैसों से जुड़ी सलाह और अवसर।",
                ja = "Mochiからのお金に役立つ気づきと機会です。",
                sw = "Vidokezo vya fedha na fursa muhimu kutoka kwa Mochi.",
                zh = "来自 Mochi 的实用理财提醒与机会。"
            )
        )

        NotificationChannelKind.AUTO_CAPTURE -> NotificationChannelCopy(
            name = pickLanguage(
                languageTag,
                en = "Draft capture alerts",
                lv = "Melnrakstu uztveršanas brīdinājumi",
                es = "Alertas de borradores capturados",
                fr = "Alertes de brouillons captures",
                de = "Hinweise zu erfassten Entwurfen",
                pt = "Alertas de rascunhos capturados",
                ar = "تنبيهات المسودات الملتقطة",
                hi = "कैप्चर किए गए ड्राफ्ट अलर्ट",
                ja = "下書き取り込みアラート",
                sw = "Arifa za rasimu zilizokamatwa",
                zh = "草稿捕捉提醒"
            ),
            description = pickLanguage(
                languageTag,
                en = "Alerts when Xpenze captures a new transaction draft from your automatic tools.",
                lv = "Brīdina, kad Xpenze tver jaunu darījuma melnrakstu no jūsu automātiskajiem rīkiem.",
                es = "Alertas cuando Xpenze captura un nuevo borrador de transaccion desde tus herramientas automaticas.",
                fr = "Alertes lorsque Xpenze capture un nouveau brouillon de transaction a partir de vos outils automatiques.",
                de = "Hinweise, wenn Xpenze einen neuen Transaktionsentwurf aus Ihren automatischen Tools erfasst.",
                pt = "Alertas quando o Xpenze captura um novo rascunho de transacao das suas ferramentas automaticas.",
                ar = "تنبيهات عندما يلتقط Xpenze مسودة معاملة جديدة من أدواتك التلقائية.",
                hi = "जब Xpenze आपके ऑटोमैटिक टूल्स से नया ट्रांजैक्शन ड्राफ्ट कैप्चर करे तब अलर्ट।",
                ja = "自動ツールで新しい取引下書きを取り込んだときの通知です。",
                sw = "Arifa wakati Xpenze inakamata rasimu mpya ya muamala kutoka zana zako za kiotomatiki.",
                zh = "当 Xpenze 从你的自动工具中捕捉到新的交易草稿时发出提醒。"
            )
        )
    }
}

internal fun budgetAlertCopy(
    languageTag: String,
    budgetName: String,
    percentUsed: Int,
    alertThresholdPercent: Int,
    spentText: String,
    limitText: String,
    overspentText: String?
): LocalizedNotificationCopy {
    val title = if (!overspentText.isNullOrBlank()) {
        pickLanguage(
            languageTag,
            en = "Budget exceeded: $budgetName",
            lv = "Pārsniegts budžets: $budgetName",
            es = "Presupuesto excedido: $budgetName",
            fr = "Budget dépassé : $budgetName",
            de = "Budget überschritten: $budgetName",
            pt = "Orçamento excedido: $budgetName",
            ar = "تم تجاوز الميزانية: $budgetName",
            hi = "बजट सीमा पार: $budgetName",
            ja = "予算超過: $budgetName",
            sw = "Bajeti imevukwa: $budgetName",
            zh = "预算已超出：$budgetName"
        )
    } else {
        pickLanguage(
            languageTag,
            en = "Budget alert: $budgetName",
            lv = "Budžeta brīdinājums: $budgetName",
            es = "Alerta de presupuesto: $budgetName",
            fr = "Alerte budget : $budgetName",
            de = "Budgetwarnung: $budgetName",
            pt = "Alerta de orçamento: $budgetName",
            ar = "تنبيه ميزانية: $budgetName",
            hi = "बजट अलर्ट: $budgetName",
            ja = "予算アラート: $budgetName",
            sw = "Arifa ya bajeti: $budgetName",
            zh = "预算提醒：$budgetName"
        )
    }

    val body = if (!overspentText.isNullOrBlank()) {
        pickLanguage(
            languageTag,
            en = "Overspent by $overspentText • $percentUsed% used ($spentText of $limitText)",
            lv = "Pārtērēts par $overspentText â¢ Izlietots $percentUsed% ($spentText no $limitText)",
            es = "Te pasaste por $overspentText • $percentUsed%% usado ($spentText de $limitText)",
            fr = "Dépassement de $overspentText • $percentUsed%% utilisé ($spentText sur $limitText)",
            de = "Um $overspentText überschritten • $percentUsed%% genutzt ($spentText von $limitText)",
            pt = "Ultrapassado em $overspentText • $percentUsed%% usado ($spentText de $limitText)",
            ar = "تجاوزت بمقدار $overspentText • تم استخدام $percentUsed%% ($spentText من $limitText)",
            hi = "$overspentText से अधिक खर्च • $percentUsed%% उपयोग ($spentText में से $limitText)",
            ja = "$overspentText 超過 • $percentUsed%% 使用済み（$spentText / $limitText）",
            sw = "Umezidi kwa $overspentText • imetumika $percentUsed%% ($spentText kati ya $limitText)",
            zh = "已超支 $overspentText • 已用 $percentUsed%%（$spentText / $limitText）"
        )
    } else {
        pickLanguage(
            languageTag,
            en = "$percentUsed% used • Threshold $alertThresholdPercent% ($spentText of $limitText)",
            lv = "$percentUsed% izmantots â¢ Slieksnis $alertThresholdPercent% ($spentText no $limitText)",
            es = "$percentUsed%% usado • Umbral $alertThresholdPercent%% ($spentText de $limitText)",
            fr = "$percentUsed%% utilisé • Seuil $alertThresholdPercent%% ($spentText sur $limitText)",
            de = "$percentUsed%% genutzt • Schwelle $alertThresholdPercent%% ($spentText von $limitText)",
            pt = "$percentUsed%% usado • Limite $alertThresholdPercent%% ($spentText de $limitText)",
            ar = "تم استخدام $percentUsed%% • الحد $alertThresholdPercent%% ($spentText من $limitText)",
            hi = "$percentUsed%% उपयोग • सीमा $alertThresholdPercent%% ($spentText में से $limitText)",
            ja = "$percentUsed%% 使用 • しきい値 $alertThresholdPercent%%（$spentText / $limitText）",
            sw = "$percentUsed%% imetumika • Kiwango $alertThresholdPercent%% ($spentText kati ya $limitText)",
            zh = "已用 $percentUsed%% • 阈值 $alertThresholdPercent%%（$spentText / $limitText）"
        )
    }

    return LocalizedNotificationCopy(title = title, body = body)
}

internal fun buildPersonalizedMochiAlert(
    rawHistory: String?,
    opportunities: List<PremiumOpportunity>,
    transactions: List<Transaction>,
    categories: List<Category>,
    budgets: List<Budget>,
    recurringRules: List<RecurringRule>,
    currencyCode: String,
    languageTag: String,
    nowMillis: Long = System.currentTimeMillis()
): MochiNotificationContent? {
    val topic = dominantConversationTopic(rawHistory) ?: return null
    val relevantOpportunity = chooseOpportunityForTopic(topic, opportunities)

    if (relevantOpportunity != null) {
        val copy = formatPremiumOpportunity(
            opportunity = relevantOpportunity,
            currencyCode = currencyCode,
            languageTag = languageTag
        )
        return MochiNotificationContent(
            title = mochiTopicTitle(languageTag, topic),
            body = copy.body,
            prompt = buildMochiPromptForOpportunity(
                opportunity = relevantOpportunity,
                currencyCode = currencyCode,
                languageTag = languageTag
            )
        )
    }

    return when (topic) {
        MochiConversationTopic.BUDGETS -> buildBudgetFocusedAlert(
            budgets = budgets,
            transactions = transactions,
            currencyCode = currencyCode,
            languageTag = languageTag,
            nowMillis = nowMillis
        )

        MochiConversationTopic.CASH_FLOW -> buildCashFlowAlert(
            transactions = transactions,
            currencyCode = currencyCode,
            languageTag = languageTag,
            nowMillis = nowMillis
        )

        MochiConversationTopic.RECURRING -> buildRecurringAlert(
            recurringRules = recurringRules,
            currencyCode = currencyCode,
            languageTag = languageTag
        )

        MochiConversationTopic.CATEGORY_CLEANUP -> buildCategoryCleanupAlert(
            transactions = transactions,
            categories = categories,
            currencyCode = currencyCode,
            languageTag = languageTag,
            nowMillis = nowMillis
        )

        MochiConversationTopic.SAVINGS,
        MochiConversationTopic.GENERAL -> buildSavingsFallbackAlert(
            opportunities = opportunities,
            transactions = transactions,
            categories = categories,
            currencyCode = currencyCode,
            languageTag = languageTag,
            nowMillis = nowMillis
        )
    }
}

private fun buildBudgetFocusedAlert(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    currencyCode: String,
    languageTag: String,
    nowMillis: Long
): MochiNotificationContent? {
    val candidate = budgets
        .asSequence()
        .filter { it.isActive && it.amountLimitCents > 0L }
        .mapNotNull { budget ->
            val window = budget.currentPeriodWindow(nowMillis) ?: return@mapNotNull null
            val spent = transactions.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .filter { it.dateTime in window.spentRange.startMillis..window.spentRange.endMillis }
                .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                .filter { budget.accountId == null || it.accountId == budget.accountId }
                .sumOf { it.money.amountCents }
            val usedPercent = ((spent.toDouble() / budget.amountLimitCents.toDouble()) * 100.0).toInt()
            BudgetPressureCandidate(
                budget = budget,
                usedPercent = usedPercent,
                spentCents = spent
            )
        }
        .maxByOrNull { it.usedPercent }
        ?: return null

    val budget = candidate.budget
    val usedPercent = candidate.usedPercent
    val spent = candidate.spentCents.toCurrencyString(currencyCode)
    val limit = budget.amountLimitCents.toCurrencyString(currencyCode)

    return MochiNotificationContent(
        title = mochiTopicTitle(languageTag, MochiConversationTopic.BUDGETS),
        body = pickLanguage(
            languageTag,
            en = "${budget.name} is the budget I would check first. It is at $usedPercent% ($spent of $limit).",
            lv = "${budget.name} ir budžets, kuru es vispirms pārbaudītu. Tas ir $usedPercent% ($spent no $limit).",
            es = "${budget.name} es el presupuesto que yo revisaría primero. Va en $usedPercent%% ($spent de $limit).",
            fr = "${budget.name} est le budget à vérifier en premier. Il est à $usedPercent%% ($spent sur $limit).",
            de = "${budget.name} ist das Budget, das ich zuerst prüfen würde. Es liegt bei $usedPercent%% ($spent von $limit).",
            pt = "${budget.name} é o orçamento que eu verificaria primeiro. Ele está em $usedPercent%% ($spent de $limit).",
            ar = "${budget.name} هو أول ميزانية أنصح بمراجعتها. وصلت إلى $usedPercent%% ($spent من $limit).",
            hi = "${budget.name} वह बजट है जिसे मैं पहले देखूंगा। यह $usedPercent%% पर है ($spent में से $limit)।",
            ja = "${budget.name} は最初に確認したい予算です。$usedPercent%%（$spent / $limit）まで進んでいます。",
            sw = "${budget.name} ndiyo bajeti ambayo ningekagua kwanza. Imefika $usedPercent%% ($spent kati ya $limit).",
            zh = "${budget.name} 是我建议你先查看的预算。它已经到 $usedPercent%%（$spent / $limit）。"
        ),
        prompt = pickLanguage(
            languageTag,
            en = "Review my budgets and tell me which one needs attention first and what to cut this week.",
            lv = "Pārskatiet manus budžetus un pastāstiet man, kuram vispirms jāpievērš uzmanība un ko samazināt šonedēļ.",
            es = "Revisa mis presupuestos y dime cual necesita atencion primero y que debo recortar esta semana.",
            fr = "Passe en revue mes budgets et dis-moi lequel demande de l'attention en premier et quoi réduire cette semaine.",
            de = "Prüfe meine Budgets und sag mir, welches zuerst Aufmerksamkeit braucht und was ich diese Woche kürzen sollte.",
            pt = "Revise meus orçamentos e diga qual precisa de atenção primeiro e o que devo cortar esta semana.",
            ar = "راجع ميزانياتي وأخبرني أيها يحتاج إلى الانتباه أولاً وما الذي يجب أن أخففه هذا الأسبوع.",
            hi = "मेरे बजट देखें और बताएं कि किसे सबसे पहले ध्यान चाहिए और इस सप्ताह मुझे कहाँ कटौती करनी चाहिए।",
            ja = "予算を見直して、最初に注意すべきものと今週どこを削るべきか教えて。",
            sw = "Kagua bajeti zangu na uniambie ipi inahitaji uangalizi kwanza na nikate nini wiki hii.",
            zh = "检查我的预算，告诉我哪个最需要先关注，以及这周该从哪里削减开支。"
        )
    )
}

private fun buildCashFlowAlert(
    transactions: List<Transaction>,
    currencyCode: String,
    languageTag: String,
    nowMillis: Long
): MochiNotificationContent? {
    val lookbackStart = nowMillis - 30L * 24L * 60L * 60L * 1000L
    val recent = transactions.filter { it.dateTime in lookbackStart..nowMillis }
    if (recent.isEmpty()) return null

    val income = recent.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents }
    val expense = recent.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents }
    val net = income - expense

    return MochiNotificationContent(
        title = mochiTopicTitle(languageTag, MochiConversationTopic.CASH_FLOW),
        body = pickLanguage(
            languageTag,
            en = "Last 30 days: income ${income.toCurrencyString(currencyCode)}, expense ${expense.toCurrencyString(currencyCode)}, net ${net.toCurrencyString(currencyCode)}.",
            lv = "Pēdējās 30 dienas: ienākumi ${income.toCurrencyString(currencyCode)}, izdevumi ${expense.toCurrencyString(currencyCode)}, neto ${net.toCurrencyString(currencyCode)}.",
            es = "Ultimos 30 dias: ingresos ${income.toCurrencyString(currencyCode)}, gastos ${expense.toCurrencyString(currencyCode)}, neto ${net.toCurrencyString(currencyCode)}.",
            fr = "30 derniers jours : revenus ${income.toCurrencyString(currencyCode)}, dépenses ${expense.toCurrencyString(currencyCode)}, net ${net.toCurrencyString(currencyCode)}.",
            de = "Letzte 30 Tage: Einnahmen ${income.toCurrencyString(currencyCode)}, Ausgaben ${expense.toCurrencyString(currencyCode)}, netto ${net.toCurrencyString(currencyCode)}.",
            pt = "Ultimos 30 dias: receita ${income.toCurrencyString(currencyCode)}, gasto ${expense.toCurrencyString(currencyCode)}, saldo ${net.toCurrencyString(currencyCode)}.",
            ar = "آخر 30 يوماً: الدخل ${income.toCurrencyString(currencyCode)}، المصروف ${expense.toCurrencyString(currencyCode)}، الصافي ${net.toCurrencyString(currencyCode)}.",
            hi = "पिछले 30 दिन: आय ${income.toCurrencyString(currencyCode)}, खर्च ${expense.toCurrencyString(currencyCode)}, शुद्ध ${net.toCurrencyString(currencyCode)}।",
            ja = "直近30日: 収入 ${income.toCurrencyString(currencyCode)}、支出 ${expense.toCurrencyString(currencyCode)}、純額 ${net.toCurrencyString(currencyCode)}。",
            sw = "Siku 30 zilizopita: mapato ${income.toCurrencyString(currencyCode)}, matumizi ${expense.toCurrencyString(currencyCode)}, jumla halisi ${net.toCurrencyString(currencyCode)}.",
            zh = "过去30天：收入 ${income.toCurrencyString(currencyCode)}，支出 ${expense.toCurrencyString(currencyCode)}，净额 ${net.toCurrencyString(currencyCode)}。"
        ),
        prompt = pickLanguage(
            languageTag,
            en = "Review my cash flow and tell me what is getting better and what is risky right now.",
            lv = "Pārskatiet manu naudas plūsmu un pastāstiet man, kas šobrīd kļūst labāks un kas ir riskants.",
            es = "Revisa mi flujo de caja y dime que esta mejorando y que riesgo ves ahora.",
            fr = "Passe en revue mon flux de trésorerie et dis-moi ce qui s'améliore et ce qui est risqué en ce moment.",
            de = "Prüfe meinen Cashflow und sag mir, was besser wird und was gerade riskant ist.",
            pt = "Revise meu fluxo de caixa e diga o que está melhorando e o que está arriscado agora.",
            ar = "راجع التدفق النقدي لدي وأخبرني ما الذي يتحسن وما الذي يبدو خطيراً الآن.",
            hi = "मेरे कैश फ्लो की समीक्षा करें और बताएं कि क्या बेहतर हो रहा है और अभी क्या जोखिम है।",
            ja = "私のキャッシュフローを見直して、改善している点と今リスクな点を教えて。",
            sw = "Kagua mtiririko wangu wa fedha na uniambie kinachoboreka na kilicho hatarini sasa hivi.",
            zh = "检查我的现金流，告诉我现在哪些方面在变好，哪些地方有风险。"
        )
    )
}

private fun buildRecurringAlert(
    recurringRules: List<RecurringRule>,
    currencyCode: String,
    languageTag: String
): MochiNotificationContent? {
    val recurringExpenses = recurringRules
        .filter { it.isActive && it.type == TransactionType.EXPENSE }
        .sortedByDescending { it.money.amountCents }
    val biggest = recurringExpenses.firstOrNull() ?: return null

    return MochiNotificationContent(
        title = mochiTopicTitle(languageTag, MochiConversationTopic.RECURRING),
        body = pickLanguage(
            languageTag,
            en = "You have ${recurringExpenses.size} active recurring expenses. ${biggest.title} is the biggest at ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            lv = "Jums ir aktīvi ${recurringExpenses.size} regulārie izdevumi. ${biggest.title} ir lielākais no ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            es = "Tienes ${recurringExpenses.size} gastos recurrentes activos. ${biggest.title} es el mayor con ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            fr = "Vous avez ${recurringExpenses.size} dépenses récurrentes actives. ${biggest.title} est la plus importante à ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            de = "Du hast ${recurringExpenses.size} aktive wiederkehrende Ausgaben. ${biggest.title} ist mit ${biggest.money.amountCents.toCurrencyString(currencyCode)} die größte.",
            pt = "Você tem ${recurringExpenses.size} despesas recorrentes ativas. ${biggest.title} é a maior com ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            ar = "لديك ${recurringExpenses.size} مصروفات متكررة نشطة. ${biggest.title} هي الأكبر بقيمة ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            hi = "आपके पास ${recurringExpenses.size} सक्रिय आवर्ती खर्च हैं। ${biggest.title} सबसे बड़ा है: ${biggest.money.amountCents.toCurrencyString(currencyCode)}।",
            ja = "有効な定期支出が ${recurringExpenses.size} 件あります。最大は ${biggest.title} の ${biggest.money.amountCents.toCurrencyString(currencyCode)} です。",
            sw = "Una matumizi ya mara kwa mara ${recurringExpenses.size} yaliyo hai. ${biggest.title} ndiyo kubwa zaidi kwa ${biggest.money.amountCents.toCurrencyString(currencyCode)}.",
            zh = "你有 ${recurringExpenses.size} 个活跃的经常性支出。最大的是 ${biggest.title}，金额为 ${biggest.money.amountCents.toCurrencyString(currencyCode)}。"
        ),
        prompt = pickLanguage(
            languageTag,
            en = "Review my recurring payments and tell me what to keep, cut, or renegotiate first.",
            lv = "Pārskatiet manus periodiskos maksājumus un pastāstiet man, ko vispirms paturēt, samazināt vai pārrunāt.",
            es = "Revisa mis pagos recurrentes y dime que debo mantener, recortar o renegociar primero.",
            fr = "Passe en revue mes paiements récurrents et dis-moi quoi garder, réduire ou renégocier d'abord.",
            de = "Prüfe meine wiederkehrenden Zahlungen und sag mir, was ich zuerst behalten, kürzen oder neu verhandeln sollte.",
            pt = "Revise meus pagamentos recorrentes e diga o que devo manter, cortar ou renegociar primeiro.",
            ar = "راجع مدفوعاتي المتكررة وأخبرني بما يجب الإبقاء عليه أو تقليله أو إعادة التفاوض عليه أولاً.",
            hi = "मेरे आवर्ती भुगतानों की समीक्षा करें और बताएं कि पहले क्या रखना, घटाना या फिर से बातचीत करना चाहिए।",
            ja = "定期支払いを見直して、最初に残すもの、削るもの、再交渉するものを教えて。",
            sw = "Kagua malipo yangu ya mara kwa mara na uniambie niyapi nibaki nayo, nikate, au nijadiliane upya kwanza.",
            zh = "检查我的经常性付款，告诉我哪些该保留、削减，或优先重新谈判。"
        )
    )
}

private fun buildCategoryCleanupAlert(
    transactions: List<Transaction>,
    categories: List<Category>,
    currencyCode: String,
    languageTag: String,
    nowMillis: Long
): MochiNotificationContent? {
    val lookbackStart = nowMillis - 45L * 24L * 60L * 60L * 1000L
    val recentExpenses = transactions.filter {
        it.type == TransactionType.EXPENSE && it.dateTime in lookbackStart..nowMillis
    }
    if (recentExpenses.isEmpty()) return null

    val categoriesById = categories.associateBy { it.id }
    val uncategorizedAmount = recentExpenses
        .filter { it.categoryId == null }
        .sumOf { it.money.amountCents }

    if (uncategorizedAmount > 0L) {
        return MochiNotificationContent(
            title = mochiTopicTitle(languageTag, MochiConversationTopic.CATEGORY_CLEANUP),
            body = pickLanguage(
                languageTag,
                en = "Uncategorized spending still hides ${uncategorizedAmount.toCurrencyString(currencyCode)} in the last 45 days.",
                lv = "Nekategorizētie izdevumi joprojām slēpj ${uncategorizedAmount.toCurrencyString(currencyCode)} pēdējo 45 dienu laikā.",
                es = "El gasto sin categoría todavía esconde ${uncategorizedAmount.toCurrencyString(currencyCode)} en los últimos 45 días.",
                fr = "Les dépenses non classées cachent encore ${uncategorizedAmount.toCurrencyString(currencyCode)} sur les 45 derniers jours.",
                de = "Nicht zugeordnete Ausgaben verbergen in den letzten 45 Tagen noch ${uncategorizedAmount.toCurrencyString(currencyCode)}.",
                pt = "Os gastos sem categoria ainda escondem ${uncategorizedAmount.toCurrencyString(currencyCode)} nos últimos 45 dias.",
                ar = "لا تزال المصروفات غير المصنفة تخفي ${uncategorizedAmount.toCurrencyString(currencyCode)} خلال آخر 45 يوماً.",
                hi = "पिछले 45 दिनों में बिना श्रेणी वाले खर्च अभी भी ${uncategorizedAmount.toCurrencyString(currencyCode)} छिपा रहे हैं।",
                ja = "過去45日で未分類の支出がまだ ${uncategorizedAmount.toCurrencyString(currencyCode)} 隠れています。",
                sw = "Matumizi yasiyo na kategoria bado yanaficha ${uncategorizedAmount.toCurrencyString(currencyCode)} katika siku 45 zilizopita.",
                zh = "过去45天里，未分类支出仍隐藏着 ${uncategorizedAmount.toCurrencyString(currencyCode)}。"
            ),
            prompt = pickLanguage(
                languageTag,
                en = "Show me my uncategorized spending and tell me how to clean it up fast.",
                lv = "Parādiet man savus tēriņus bez kategorijas un pastāstiet man, kā tos ātri iztīrīt.",
                es = "Muestrame mis gastos sin categoría y dime como ordenarlos rápido.",
                fr = "Montre-moi mes dépenses non classées et dis-moi comment les ranger vite.",
                de = "Zeig mir meine nicht zugeordneten Ausgaben und sag mir, wie ich sie schnell aufräume.",
                pt = "Mostre meus gastos sem categoria e diga como posso organizá-los rápido.",
                ar = "أظهر لي المصروفات غير المصنفة وأخبرني كيف أنظمها بسرعة.",
                hi = "मुझे मेरे बिना श्रेणी वाले खर्च दिखाएं और बताएं कि इन्हें जल्दी कैसे व्यवस्थित करूं।",
                ja = "未分類の支出を見せて、すぐ整理する方法を教えて。",
                sw = "Nionyeshe matumizi yangu yasiyo na kategoria na uniambie jinsi ya kuyapanga haraka.",
                zh = "把我的未分类支出展示出来，并告诉我如何快速整理。"
            )
        )
    }

    val topCategory = recentExpenses
        .groupBy { it.categoryId }
        .maxByOrNull { (_, txs) -> txs.sumOf { it.money.amountCents } }
        ?: return null
    val categoryName = topCategory.key?.let { categoriesById[it]?.name }
        ?: pickLanguage(
            languageTag,
            en = "Uncategorized",
            lv = "Bez kategorijas",
            es = "Sin categoría",
            fr = "Non classé",
            de = "Nicht zugeordnet",
            pt = "Sem categoria",
            ar = "غير مصنف",
            hi = "बिना श्रेणी",
            ja = "未分類",
            sw = "Isiyo na kategoria",
            zh = "未分类"
        )
    val amount = topCategory.value.sumOf { it.money.amountCents }.toCurrencyString(currencyCode)

    return MochiNotificationContent(
        title = mochiTopicTitle(languageTag, MochiConversationTopic.CATEGORY_CLEANUP),
        body = pickLanguage(
            languageTag,
            en = "$categoryName is leading your recent spending at $amount.",
            lv = "$categoryName nosaka jūsu nesenos tēriņus vietnē $amount.",
            es = "$categoryName lidera tu gasto reciente con $amount.",
            fr = "$categoryName mène vos dépenses récentes avec $amount.",
            de = "$categoryName führt deine jüngsten Ausgaben mit $amount an.",
            pt = "$categoryName lidera seus gastos recentes com $amount.",
            ar = "يتصدر $categoryName مصروفاتك الأخيرة بقيمة $amount.",
            hi = "$categoryName हाल के खर्च में $amount के साथ सबसे ऊपर है।",
            ja = "$categoryName が最近の支出で $amount と最も大きいです。",
            sw = "$categoryName inaongoza matumizi yako ya hivi karibuni kwa $amount.",
            zh = "$categoryName 是你近期支出最高的类别，达到 $amount。"
        ),
        prompt = pickLanguage(
            languageTag,
            en = "Break down my recent spending by category and tell me where the easiest reduction is.",
            lv = "Sadaliet manus nesenos izdevumus pa kategorijām un pastāstiet man, kur ir visvieglāk samazināt.",
            es = "Desglosa mis gastos recientes por categoría y dime dónde está el recorte más fácil.",
            fr = "Décompose mes dépenses récentes par catégorie et dis-moi où la réduction la plus simple se trouve.",
            de = "Schlüssle meine jüngsten Ausgaben nach Kategorien auf und sag mir, wo die einfachste Kürzung liegt.",
            pt = "Detalhe meus gastos recentes por categoria e diga onde está o corte mais fácil.",
            ar = "قسّم مصروفاتي الأخيرة حسب الفئة وأخبرني أين أسهل تقليل ممكن.",
            hi = "मेरे हाल के खर्च को श्रेणियों में बाँटें और बताएं कि सबसे आसान कटौती कहाँ है।",
            ja = "最近の支出をカテゴリ別に分けて、いちばん削りやすい所を教えて。",
            sw = "Vunja matumizi yangu ya hivi karibuni kwa kategoria na uniambie sehemu rahisi zaidi ya kupunguza.",
            zh = "按类别拆分我最近的支出，并告诉我最容易削减的是哪里。"
        )
    )
}

private fun buildSavingsFallbackAlert(
    opportunities: List<PremiumOpportunity>,
    transactions: List<Transaction>,
    categories: List<Category>,
    currencyCode: String,
    languageTag: String,
    nowMillis: Long
): MochiNotificationContent? {
    val firstOpportunity = opportunities.firstOrNull()
    if (firstOpportunity != null) {
        val copy = formatPremiumOpportunity(firstOpportunity, currencyCode, languageTag)
        return MochiNotificationContent(
            title = mochiTopicTitle(languageTag, MochiConversationTopic.SAVINGS),
            body = copy.body,
            prompt = buildMochiPromptForOpportunity(firstOpportunity, currencyCode, languageTag)
        )
    }

    val lookbackStart = nowMillis - 30L * 24L * 60L * 60L * 1000L
    val recentExpenses = transactions.filter {
        it.type == TransactionType.EXPENSE && it.dateTime in lookbackStart..nowMillis
    }
    val categoriesById = categories.associateBy { it.id }
    val topCategory = recentExpenses
        .groupBy { it.categoryId }
        .maxByOrNull { (_, txs) -> txs.sumOf { it.money.amountCents } }
        ?: return null
    val categoryName = topCategory.key?.let { categoriesById[it]?.name } ?: pickLanguage(
        languageTag,
        en = "Uncategorized",
        lv = "Bez kategorijas",
        es = "Sin categoría",
        fr = "Non classé",
        de = "Nicht zugeordnet",
        pt = "Sem categoria",
        ar = "غير مصنف",
        hi = "बिना श्रेणी",
        ja = "未分類",
        sw = "Isiyo na kategoria",
        zh = "未分类"
    )
    val amount = topCategory.value.sumOf { it.money.amountCents }.toCurrencyString(currencyCode)

    return MochiNotificationContent(
        title = mochiTopicTitle(languageTag, MochiConversationTopic.SAVINGS),
        body = pickLanguage(
            languageTag,
            en = "$categoryName is still your biggest recent spending area at $amount.",
            lv = "$categoryName joprojām ir jūsu lielākā pēdējā laika izdevumu daļa uzņēmumā $amount.",
            es = "$categoryName sigue siendo tu mayor área de gasto reciente con $amount.",
            fr = "$categoryName reste votre plus grand poste de dépense récent avec $amount.",
            de = "$categoryName bleibt dein größter jüngster Ausgabenbereich mit $amount.",
            pt = "$categoryName continua sendo sua maior área de gasto recente com $amount.",
            ar = "لا يزال $categoryName أكبر مجال إنفاق حديث لديك بقيمة $amount.",
            hi = "$categoryName अभी भी $amount के साथ आपका सबसे बड़ा हालिया खर्च क्षेत्र है।",
            ja = "$categoryName は依然として最近の最大支出項目で、$amount です。",
            sw = "$categoryName bado ndiyo eneo lako kubwa zaidi la matumizi ya hivi karibuni kwa $amount.",
            zh = "$categoryName 仍然是你最近最大的支出类别，达到 $amount。"
        ),
        prompt = pickLanguage(
            languageTag,
            en = "Show me the best savings move in my data right now and how to act on it this week.",
            lv = "Parādiet man pašlaik labākos ietaupījumus manos datos un kā rīkoties šajā nedēļā.",
            es = "Muestrame la mejor oportunidad de ahorro en mis datos ahora y como actuar esta semana.",
            fr = "Montre-moi la meilleure économie possible dans mes données maintenant et comment agir cette semaine.",
            de = "Zeig mir die beste Sparchance in meinen Daten und wie ich diese Woche darauf reagieren sollte.",
            pt = "Mostre a melhor oportunidade de economia nos meus dados agora e como agir esta semana.",
            ar = "أظهر لي أفضل فرصة توفير في بياناتي الآن وكيف أتصرف هذا الأسبوع.",
            hi = "मेरे डेटा में अभी सबसे अच्छा बचत अवसर दिखाएँ और बताएं कि इस सप्ताह क्या करना है।",
            ja = "今のデータで最も良い節約チャンスを見せて、今週どう動くべきか教えて。",
            sw = "Nionyeshe fursa bora ya kuokoa kwenye data yangu sasa hivi na namna ya kuchukua hatua wiki hii.",
            zh = "告诉我我当前数据里最好的节省机会，以及这周应该怎么做。"
        )
    )
}

private fun chooseOpportunityForTopic(
    topic: MochiConversationTopic,
    opportunities: List<PremiumOpportunity>
): PremiumOpportunity? {
    return when (topic) {
        MochiConversationTopic.BUDGETS -> listOf(
            PremiumOpportunityType.BUDGET_PRESSURE,
            PremiumOpportunityType.BUDGET_SUGGESTION
        ).firstNotNullOfOrNull { type ->
            opportunities.firstOrNull { it.type == type }
        }

        MochiConversationTopic.RECURRING -> opportunities.firstOrNull {
            it.type == PremiumOpportunityType.RECURRING_REVIEW
        }

        MochiConversationTopic.CATEGORY_CLEANUP -> listOf(
            PremiumOpportunityType.CATEGORY_SPIKE,
            PremiumOpportunityType.CATEGORY_CUTBACK
        ).firstNotNullOfOrNull { type ->
            opportunities.firstOrNull { it.type == type }
        }

        MochiConversationTopic.SAVINGS -> listOf(
            PremiumOpportunityType.CATEGORY_CUTBACK,
            PremiumOpportunityType.CATEGORY_SPIKE,
            PremiumOpportunityType.BUDGET_PRESSURE
        ).firstNotNullOfOrNull { type ->
            opportunities.firstOrNull { it.type == type }
        } ?: opportunities.firstOrNull()

        MochiConversationTopic.CASH_FLOW,
        MochiConversationTopic.GENERAL -> null
    }
}

private fun dominantConversationTopic(rawHistory: String?): MochiConversationTopic? {
    if (rawHistory.isNullOrBlank()) return null
    val messages = runCatching {
        historyJson.decodeFromString(ListSerializer(StoredChatMessage.serializer()), rawHistory)
    }.getOrElse { return null }

    val counts = mutableMapOf<MochiConversationTopic, Int>()
    messages
        .filter { it.isUser }
        .takeLast(30)
        .forEach { message ->
            val topic = classifyUserPrompt(message.text) ?: return@forEach
            counts[topic] = (counts[topic] ?: 0) + 1
        }
    return counts.maxByOrNull { it.value }?.key
}

private fun classifyUserPrompt(text: String): MochiConversationTopic? {
    val normalized = text.lowercase()
    return when {
        containsAny(
            normalized,
            "budget", "presupuesto", "orçamento", "orcamento", "budget", "ميزانية", "बजट", "予算", "预算", "bajeti"
        ) -> MochiConversationTopic.BUDGETS

        containsAny(
            normalized,
            "recurring", "subscription", "subscriptions", "repeat", "repeating", "suscripcion", "suscripciones",
            "assinatura", "assinaturas", "abonnement", "abonnements", "abo", "اشتراك", "اشتراكات", "متكرر",
            "सदस्यता", "दोहर", "定期", "サブスク", "订阅", "定期", "mara kwa mara"
        ) -> MochiConversationTopic.RECURRING

        containsAny(
            normalized,
            "cash flow", "income", "expense", "net", "balance", "flujo", "ingres", "gasto", "saldo", "fluxo",
            "receita", "despesa", "solde", "revenu", "depense", "einnah", "ausgab", "saldo", "تدفق", "دخل",
            "مصروف", "صافي", "आय", "खर्च", "नकद", "収入", "支出", "残高", "现金流", "收入", "支出", "净额",
            "mapato", "matumizi", "salio"
        ) -> MochiConversationTopic.CASH_FLOW

        containsAny(
            normalized,
            "uncategorized", "category", "categories", "categoría", "categorias", "categoria", "catégorie", "kategorie",
            "frais", "غير مصنف", "فئة", "श्रेणी", "カテゴリー", "カテゴリ", "分类", "kategoria"
        ) -> MochiConversationTopic.CATEGORY_CLEANUP

        containsAny(
            normalized,
            "save", "saving", "savings", "reduce", "cut", "trim", "lower", "afford", "ahorrar", "ahorro", "reducir",
            "recortar", "economizar", "poupar", "epargner", "economiser", "sparen", "senken", "ادخار", "خفض",
            "تقليل", "बचत", "कम", "節約", "減ら", "储蓄", "减少", "akiba", "punguza"
        ) -> MochiConversationTopic.SAVINGS

        normalized.isNotBlank() -> MochiConversationTopic.GENERAL
        else -> null
    }
}

private fun mochiTopicTitle(
    languageTag: String,
    topic: MochiConversationTopic
): String {
    return when (topic) {
        MochiConversationTopic.BUDGETS -> pickLanguage(
            languageTag,
            en = "Mochi found budget pressure",
            lv = "Moči atrada budžeta spiedienu",
            es = "Mochi encontró presión en tu presupuesto",
            fr = "Mochi a trouvé une pression budgétaire",
            de = "Mochi hat Budgetdruck gefunden",
            pt = "Mochi encontrou pressão no orçamento",
            ar = "اكتشف Mochi ضغطاً على الميزانية",
            hi = "Mochi ने बजट दबाव पकड़ा",
            ja = "Mochiが予算の圧迫を見つけました",
            sw = "Mochi ameona shinikizo la bajeti",
            zh = "Mochi 发现了预算压力"
        )

        MochiConversationTopic.CASH_FLOW -> pickLanguage(
            languageTag,
            en = "Mochi spotted a cash flow signal",
            lv = "Moči pamanīja naudas plūsmas signālu",
            es = "Mochi detectó una señal en tu flujo de caja",
            fr = "Mochi a repéré un signal de trésorerie",
            de = "Mochi hat ein Cashflow-Signal erkannt",
            pt = "Mochi detectou um sinal no seu fluxo de caixa",
            ar = "لاحظ Mochi إشارة في التدفق النقدي",
            hi = "Mochi ने कैश फ्लो संकेत देखा",
            ja = "Mochiがキャッシュフローの兆候を見つけました",
            sw = "Mochi ameona ishara kwenye mtiririko wa fedha",
            zh = "Mochi 发现了现金流信号"
        )

        MochiConversationTopic.RECURRING -> pickLanguage(
            languageTag,
            en = "Mochi found a recurring payment to review",
            lv = "Mochi atrada periodisku maksājumu, kas jāpārskata",
            es = "Mochi encontró un pago recurrente para revisar",
            fr = "Mochi a trouvé un paiement récurrent à revoir",
            de = "Mochi hat eine wiederkehrende Zahlung zum Prüfen gefunden",
            pt = "Mochi encontrou um pagamento recorrente para revisar",
            ar = "عثر Mochi على دفعة متكررة تستحق المراجعة",
            hi = "Mochi ने समीक्षा के लिए एक आवर्ती भुगतान पाया",
            ja = "Mochiが見直すべき定期支払いを見つけました",
            sw = "Mochi ameona malipo ya mara kwa mara ya kukaguliwa",
            zh = "Mochi 发现了一项值得检查的经常性付款"
        )

        MochiConversationTopic.CATEGORY_CLEANUP -> pickLanguage(
            languageTag,
            en = "Mochi found where your money is clustering",
            lv = "Mochi atklāja, kur uzkrājas jūsu nauda",
            es = "Mochi encontró dónde se está concentrando tu dinero",
            fr = "Mochi a trouvé où votre argent se concentre",
            de = "Mochi hat gefunden, wo sich dein Geld sammelt",
            pt = "Mochi encontrou onde seu dinheiro está se concentrando",
            ar = "وجد Mochi أين يتركز إنفاقك",
            hi = "Mochi ने देखा कि आपका पैसा कहाँ जमा हो रहा है",
            ja = "Mochiがどこにお金が集中しているか見つけました",
            sw = "Mochi ameona pesa yako inakusanyika wapi",
            zh = "Mochi 发现了你的钱主要流向哪里"
        )

        MochiConversationTopic.SAVINGS -> pickLanguage(
            languageTag,
            en = "Mochi found a savings move",
            lv = "Moči atrada ietaupījumu",
            es = "Mochi encontró una oportunidad de ahorro",
            fr = "Mochi a trouvé une piste d'économie",
            de = "Mochi hat eine Sparchance gefunden",
            pt = "Mochi encontrou uma oportunidade de economia",
            ar = "وجد Mochi فرصة للتوفير",
            hi = "Mochi ने बचत का एक अवसर पाया",
            ja = "Mochiが節約のチャンスを見つけました",
            sw = "Mochi ameona nafasi ya kuokoa",
            zh = "Mochi 发现了一个节省机会"
        )

        MochiConversationTopic.GENERAL -> pickLanguage(
            languageTag,
            en = "Mochi has a useful money nudge",
            lv = "Močim ir noderīgs naudas dūriens",
            es = "Mochi tiene un recordatorio útil para tu dinero",
            fr = "Mochi a un rappel utile pour vos finances",
            de = "Mochi hat einen nützlichen Geldhinweis",
            pt = "Mochi tem um lembrete útil para o seu dinheiro",
            ar = "لدى Mochi تنبيه مالي مفيد لك",
            hi = "Mochi के पास पैसों के लिए एक उपयोगी संकेत है",
            ja = "Mochiから役立つお金のヒントがあります",
            sw = "Mochi ana kidokezo muhimu cha fedha",
            zh = "Mochi 有一个实用的理财提醒"
        )
    }
}

private fun pickLanguage(
    languageTag: String,
    en: String,
    lv: String = en,
    es: String,
    fr: String,
    de: String,
    pt: String,
    ar: String,
    hi: String,
    ja: String,
    sw: String,
    zh: String
): String {
    return when {
        languageTag.startsWith("lv") -> lv
        languageTag.startsWith("es") -> es
        languageTag.startsWith("fr") -> fr
        languageTag.startsWith("de") -> de
        languageTag.startsWith("pt") -> pt
        languageTag.startsWith("ar") -> ar
        languageTag.startsWith("hi") -> hi
        languageTag.startsWith("ja") -> ja
        languageTag.startsWith("sw") -> sw
        languageTag.startsWith("zh") -> zh
        else -> en
    }
}

private fun containsAny(text: String, vararg needles: String): Boolean {
    return needles.any { text.contains(it) }
}

@Serializable
private data class StoredChatMessage(
    val text: String = "",
    val isUser: Boolean = false
)
