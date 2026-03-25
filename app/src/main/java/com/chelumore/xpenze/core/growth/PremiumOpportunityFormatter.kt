package com.chelumore.xpenze.core.growth

import com.chelumore.xpenze.core.common.toCurrencyString
import java.util.Locale

data class PremiumOpportunityCopy(
    val title: String,
    val body: String
)

data class PremiumOutcomeSummaryCopy(
    val headline: String,
    val body: String
)

data class PremiumValueTrackerCopy(
    val title: String,
    val amountText: String,
    val body: String
)

data class MochiWeeklyAlertCopy(
    val title: String,
    val body: String
)

fun premiumOpportunitySectionTitle(languageTag: String): String = whenLanguage(
    languageTag = languageTag,
    english = "Money-saving opportunities",
    spanish = "Oportunidades para ahorrar dinero",
    french = "Opportunites d economie",
    german = "Sparmöglichkeiten",
    portuguese = "Oportunidades de economia",
    arabic = "فرص لتوفير المال",
    hindi = "पैसा बचाने के मौके",
    japanese = "節約のチャンス",
    swahili = "Fursa za kuokoa pesa",
    chinese = "省钱机会"
)

fun premiumOpportunityEmptyText(languageTag: String): String = whenLanguage(
    languageTag = languageTag,
    english = "Keep tracking for a bit longer and Xpenze will start spotting real money-saving moves for you.",
    spanish = "Sigue registrando un poco mas y Xpenze empezara a detectar formas reales de ahorrarte dinero.",
    french = "Continuez a suivre un peu plus longtemps et Xpenze commencera a reperer de vraies occasions d economiser.",
    german = "Erfasse noch etwas mehr und Xpenze erkennt echte Sparchancen fur dich.",
    portuguese = "Continue registrando por mais um tempo e o Xpenze vai encontrar oportunidades reais de economia para voce.",
    arabic = "استمر في التسجيل لفترة أطول قليلا وسيبدأ Xpenze في اكتشاف فرص حقيقية لتوفير المال لك.",
    hindi = "कुछ समय और ट्रैक करते रहें, फिर Xpenze आपके लिए असली बचत के मौके दिखाना शुरू करेगा।",
    japanese = "もう少し記録を続けると、Xpenze が本当に節約できる動きを見つけ始めます。",
    swahili = "Endelea kufuatilia kidogo zaidi na Xpenze itaanza kuona njia halisi za kukuokoa pesa.",
    chinese = "继续记录一段时间，Xpenze 就会开始为你发现真正的省钱机会。"
)

fun premiumOpportunityDashboardMessage(
    languageTag: String,
    hasPremiumAccess: Boolean
): String = if (hasPremiumAccess) {
    whenLanguage(
        languageTag = languageTag,
        english = "Mochi found the clearest places you can save right now.",
        spanish = "Mochi encontro los lugares mas claros donde puedes ahorrar ahora mismo.",
        french = "Mochi a repere les endroits les plus clairs ou vous pouvez economiser maintenant.",
        german = "Mochi hat die klarsten Stellen gefunden, an denen du jetzt sparen kannst.",
        portuguese = "O Mochi encontrou os pontos mais claros em que voce pode economizar agora.",
        arabic = "وجد Mochi أوضح الأماكن التي يمكنك التوفير فيها الآن.",
        hindi = "Mochi ने अभी बचत करने की सबसे साफ जगहें ढूंढ ली हैं।",
        japanese = "Mochi が今すぐ節約しやすいポイントを見つけました。",
        swahili = "Mochi ameona maeneo yaliyo wazi zaidi ambapo unaweza kuokoa pesa sasa hivi.",
        chinese = "Mochi 已经找出你现在最容易省钱的地方。"
    )
} else {
    whenLanguage(
        languageTag = languageTag,
        english = "This is a preview of the savings guidance Premium unlocks with Mochi.",
        spanish = "Esta es una vista previa de la guia de ahorro que Premium desbloquea con Mochi.",
        french = "Ceci est un apercu des conseils d economie que Premium debloque avec Mochi.",
        german = "Das ist eine Vorschau auf die Spartipps, die Premium mit Mochi freischaltet.",
        portuguese = "Esta e uma previa da orientacao de economia que o Premium libera com o Mochi.",
        arabic = "هذه معاينة لإرشادات التوفير التي يفتحها Premium مع Mochi.",
        hindi = "यह उन बचत सुझावों की झलक है जिन्हें Premium, Mochi के साथ अनलॉक करता है।",
        japanese = "これは Premium と Mochi で解放される節約ガイドのプレビューです。",
        swahili = "Huu ni muhtasari wa mwongozo wa akiba ambao Premium hufungua kwa Mochi.",
        chinese = "这是 Premium 通过 Mochi 解锁的省钱指导预览。"
    )
}

fun premiumOpportunityPrimaryCta(
    languageTag: String,
    hasPremiumAccess: Boolean
): String = if (hasPremiumAccess) {
    whenLanguage(
        languageTag = languageTag,
        english = "Ask Mochi about this",
        spanish = "Preguntale a Mochi sobre esto",
        french = "Demandez a Mochi a ce sujet",
        german = "Frag Mochi danach",
        portuguese = "Pergunte ao Mochi sobre isso",
        arabic = "اسأل Mochi عن هذا",
        hindi = "इस बारे में Mochi से पूछें",
        japanese = "これについて Mochi に聞く",
        swahili = "Muulize Mochi kuhusu hili",
        chinese = "向 Mochi 问问这个"
    )
} else {
    whenLanguage(
        languageTag = languageTag,
        english = "Unlock Mochi",
        spanish = "Desbloquear Mochi",
        french = "Debloquer Mochi",
        german = "Mochi freischalten",
        portuguese = "Desbloquear Mochi",
        arabic = "افتح Mochi",
        hindi = "Mochi अनलॉक करें",
        japanese = "Mochi をアンロック",
        swahili = "Fungua Mochi",
        chinese = "解锁 Mochi"
    )
}

fun premiumOpportunityActionLabel(
    languageTag: String,
    hasPremiumAccess: Boolean
): String = if (hasPremiumAccess) {
    whenLanguage(
        languageTag = languageTag,
        english = "Open in Mochi",
        spanish = "Abrir en Mochi",
        french = "Ouvrir dans Mochi",
        german = "In Mochi offnen",
        portuguese = "Abrir no Mochi",
        arabic = "افتح في Mochi",
        hindi = "Mochi में खोलें",
        japanese = "Mochi で開く",
        swahili = "Fungua kwenye Mochi",
        chinese = "在 Mochi 中打开"
    )
} else {
    whenLanguage(
        languageTag = languageTag,
        english = "Unlock with Premium",
        spanish = "Desbloquear con Premium",
        french = "Debloquer avec Premium",
        german = "Mit Premium freischalten",
        portuguese = "Desbloquear com Premium",
        arabic = "افتحه مع Premium",
        hindi = "Premium से अनलॉक करें",
        japanese = "Premium でアンロック",
        swahili = "Fungua kwa Premium",
        chinese = "用 Premium 解锁"
    )
}

fun mochiSuggestedPromptTitle(languageTag: String): String = whenLanguage(
    languageTag = languageTag,
    english = "Try one of these",
    spanish = "Prueba una de estas",
    french = "Essayez l une de celles-ci",
    german = "Probiere eine davon",
    portuguese = "Experimente uma destas",
    arabic = "جرّب أحد هذه",
    hindi = "इनमें से एक आज़माएं",
    japanese = "この中から試してみてください",
    swahili = "Jaribu mojawapo ya haya",
    chinese = "试试这些问题"
)

fun buildMochiPromptForOpportunity(
    opportunity: PremiumOpportunity,
    currencyCode: String,
    languageTag: String
): String {
    val locale = localeFor(languageTag)
    return when (opportunity.type) {
        PremiumOpportunityType.CATEGORY_CUTBACK -> {
            val impact = opportunity.impactCents.toCurrencyString(currencyCode, locale)
            whenLanguage(
                languageTag = languageTag,
                english = "How can I reduce my ${opportunity.label} spending by about $impact this month?",
                spanish = "Como puedo reducir mi gasto en ${opportunity.label} en unos $impact este mes?",
                french = "Comment puis-je reduire mes depenses en ${opportunity.label} d environ $impact ce mois-ci ?",
                german = "Wie kann ich meine Ausgaben fur ${opportunity.label} diesen Monat um etwa $impact senken?",
                portuguese = "Como posso reduzir meus gastos com ${opportunity.label} em cerca de $impact este mes?",
                arabic = "كيف يمكنني خفض إنفاقي على ${opportunity.label} بنحو $impact هذا الشهر؟",
                hindi = "मैं इस महीने ${opportunity.label} पर अपना खर्च लगभग $impact कैसे कम करूं?",
                japanese = "今月 ${opportunity.label} の支出を約 $impact 減らすにはどうすればいい？",
                swahili = "Ninawezaje kupunguza matumizi yangu ya ${opportunity.label} kwa karibu $impact mwezi huu?",
                chinese = "我这个月怎样才能把 ${opportunity.label} 支出大约减少 $impact？"
            )
        }

        PremiumOpportunityType.CATEGORY_SPIKE -> whenLanguage(
            languageTag = languageTag,
            english = "Why did my ${opportunity.label} spending jump, and what should I do next?",
            spanish = "Por que subio mi gasto en ${opportunity.label} y que deberia hacer ahora?",
            french = "Pourquoi mes depenses en ${opportunity.label} ont-elles augmente, et que dois-je faire ensuite ?",
            german = "Warum sind meine ${opportunity.label}-Ausgaben gestiegen, und was sollte ich als Nächstes tun?",
            portuguese = "Por que meus gastos com ${opportunity.label} subiram e o que devo fazer agora?",
            arabic = "لماذا ارتفع إنفاقي على ${opportunity.label}، وما الخطوة التالية؟",
            hindi = "${opportunity.label} पर मेरा खर्च क्यों बढ़ गया, और मुझे आगे क्या करना चाहिए?",
            japanese = "${opportunity.label} の支出が増えたのはなぜ？ 次に何をすべき？",
            swahili = "Kwa nini matumizi yangu ya ${opportunity.label} yamepanda, na nifanye nini baada ya hapo?",
            chinese = "为什么我在 ${opportunity.label} 上的支出涨了？我下一步该怎么做？"
        )

        PremiumOpportunityType.BUDGET_PRESSURE -> whenLanguage(
            languageTag = languageTag,
            english = "Help me avoid going over my ${opportunity.label} budget.",
            spanish = "Ayudame a no pasarme de mi presupuesto de ${opportunity.label}.",
            french = "Aidez-moi a ne pas depasser mon budget ${opportunity.label}.",
            german = "Hilf mir, mein Budget ${opportunity.label} nicht zu uberschreiten.",
            portuguese = "Ajude-me a nao ultrapassar meu orcamento de ${opportunity.label}.",
            arabic = "ساعدني على عدم تجاوز ميزانية ${opportunity.label}.",
            hindi = "मुझे ${opportunity.label} बजट से ऊपर जाने से बचने में मदद करें।",
            japanese = "${opportunity.label} の予算を超えないように手伝って。",
            swahili = "Nisaidie nisivuke bajeti yangu ya ${opportunity.label}.",
            chinese = "帮我避免超出 ${opportunity.label} 预算。"
        )

        PremiumOpportunityType.RECURRING_REVIEW -> whenLanguage(
            languageTag = languageTag,
            english = "Which recurring payments should I review first?",
            spanish = "Que pagos recurrentes deberia revisar primero?",
            french = "Quels paiements recurrents devrais-je verifier en premier ?",
            german = "Welche wiederkehrenden Zahlungen sollte ich zuerst prufen?",
            portuguese = "Quais pagamentos recorrentes devo revisar primeiro?",
            arabic = "ما المدفوعات المتكررة التي يجب أن أراجعها أولا؟",
            hindi = "मुझे सबसे पहले किन आवर्ती भुगतानों की समीक्षा करनी चाहिए?",
            japanese = "最初に見直すべき定期支払いはどれ？",
            swahili = "Ni malipo gani yanayojirudia ninapaswa kuyapitia kwanza?",
            chinese = "我应该先检查哪些定期付款？"
        )

        PremiumOpportunityType.BUDGET_SUGGESTION -> whenLanguage(
            languageTag = languageTag,
            english = "Help me set a realistic ${opportunity.label} budget.",
            spanish = "Ayudame a fijar un presupuesto realista para ${opportunity.label}.",
            french = "Aidez-moi a definir un budget realiste pour ${opportunity.label}.",
            german = "Hilf mir, ein realistisches Budget fur ${opportunity.label} festzulegen.",
            portuguese = "Ajude-me a definir um orcamento realista para ${opportunity.label}.",
            arabic = "ساعدني في وضع ميزانية واقعية لـ ${opportunity.label}.",
            hindi = "${opportunity.label} के लिए एक वास्तविक बजट तय करने में मेरी मदद करें।",
            japanese = "${opportunity.label} に現実的な予算を設定するのを手伝って。",
            swahili = "Nisaidie kuweka bajeti halisi ya ${opportunity.label}.",
            chinese = "帮我为 ${opportunity.label} 设置一个现实的预算。"
        )
    }
}

fun buildMochiSuggestedPrompts(
    opportunities: List<PremiumOpportunity>,
    currencyCode: String,
    languageTag: String
): List<String> {
    val prompts = opportunities.map { opportunity ->
        buildMochiPromptForOpportunity(
            opportunity = opportunity,
            currencyCode = currencyCode,
            languageTag = languageTag
        )
    }

    return prompts.take(3).ifEmpty { defaultMochiSuggestedPrompts(languageTag) }
}

fun formatPremiumOutcomeSummary(
    opportunities: List<PremiumOpportunity>,
    currencyCode: String,
    languageTag: String,
    hasPremiumAccess: Boolean
): PremiumOutcomeSummaryCopy? {
    val actionable = opportunities.filter { it.impactCents > 0L }.take(3)
    if (actionable.isEmpty()) return null

    val locale = localeFor(languageTag)
    val monthlyAmount = actionable.sumOf { it.impactCents }.toCurrencyString(currencyCode, locale)
    val count = actionable.size

    return if (hasPremiumAccess) {
        PremiumOutcomeSummaryCopy(
            headline = whenLanguage(
                languageTag = languageTag,
                english = "Mochi is watching about $monthlyAmount each month",
                spanish = "Mochi esta vigilando unos $monthlyAmount cada mes",
                french = "Mochi surveille environ $monthlyAmount chaque mois",
                german = "Mochi beobachtet etwa $monthlyAmount pro Monat",
                portuguese = "O Mochi esta monitorando cerca de $monthlyAmount por mes",
                arabic = "يراقب Mochi حوالي $monthlyAmount كل شهر",
                hindi = "Mochi हर महीने लगभग $monthlyAmount पर नजर रख रहा है",
                japanese = "Mochi は毎月およそ $monthlyAmount を見張っています",
                swahili = "Mochi anaangalia karibu $monthlyAmount kila mwezi",
                chinese = "Mochi 正在关注每月大约 $monthlyAmount 的金额"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "That is spread across $count concrete savings opportunities already found in your data.",
                spanish = "Eso se reparte en $count oportunidades concretas de ahorro que ya encontro en tus datos.",
                french = "Cela correspond a $count occasions concretes d economiser deja reperees dans vos donnees.",
                german = "Das verteilt sich auf $count konkrete Sparchancen, die bereits in deinen Daten gefunden wurden.",
                portuguese = "Isso esta distribuido em $count oportunidades concretas de economia ja encontradas nos seus dados.",
                arabic = "وهذا موزع على $count فرص توفير واضحة تم العثور عليها بالفعل في بياناتك.",
                hindi = "यह आपके डेटा में पहले से मिली $count ठोस बचत के मौकों में फैला हुआ है।",
                japanese = "これは、あなたのデータからすでに見つかった $count 件の具体的な節約ポイントに分かれています。",
                swahili = "Hiyo imegawanyika katika nafasi $count halisi za kuokoa pesa ambazo tayari zimeonekana kwenye data yako.",
                chinese = "这已经分布在你数据中发现的 $count 个具体省钱机会里。"
            )
        )
    } else {
        PremiumOutcomeSummaryCopy(
            headline = whenLanguage(
                languageTag = languageTag,
                english = "Premium is already spotting about $monthlyAmount a month",
                spanish = "Premium ya esta detectando unos $monthlyAmount al mes",
                french = "Premium repere deja environ $monthlyAmount par mois",
                german = "Premium erkennt bereits etwa $monthlyAmount pro Monat",
                portuguese = "O Premium ja esta detectando cerca de $monthlyAmount por mes",
                arabic = "يكتشف Premium بالفعل حوالي $monthlyAmount شهريا",
                hindi = "Premium पहले से हर महीने लगभग $monthlyAmount पहचान रहा है",
                japanese = "Premium はすでに毎月およそ $monthlyAmount を見つけています",
                swahili = "Premium tayari inaona karibu $monthlyAmount kwa mwezi",
                chinese = "Premium 已经在发现每月大约 $monthlyAmount 的金额"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "Mochi found $count clear savings moves in your real spending data, not generic tips.",
                spanish = "Mochi encontro $count movimientos claros de ahorro en tus datos reales, no consejos genericos.",
                french = "Mochi a trouve $count pistes claires d economie dans vos vraies donnees, pas des conseils generiques.",
                german = "Mochi hat $count klare Sparschritte in deinen echten Ausgabendaten gefunden, keine generischen Tipps.",
                portuguese = "O Mochi encontrou $count caminhos claros de economia nos seus dados reais, nao dicas genericas.",
                arabic = "وجد Mochi عدد $count من خطوات التوفير الواضحة في بيانات إنفاقك الحقيقية، وليس نصائح عامة.",
                hindi = "Mochi ने आपके असली खर्च डेटा में $count साफ बचत कदम ढूंढे हैं, कोई सामान्य सलाह नहीं।",
                japanese = "Mochi は一般論ではなく、あなたの実際の支出データから $count 件のはっきりした節約アクションを見つけました。",
                swahili = "Mochi ameona hatua $count wazi za akiba kwenye data yako halisi ya matumizi, si ushauri wa jumla.",
                chinese = "Mochi 从你的真实支出数据中找到了 $count 个明确的省钱动作，不是泛泛建议。"
            )
        )
    }
}

fun trackedMonthlyProtectionCents(opportunities: List<PremiumOpportunity>): Long {
    return opportunities
        .filter { it.impactCents > 0L }
        .take(3)
        .sumOf { it.impactCents }
}

fun trackedOpportunityCount(opportunities: List<PremiumOpportunity>): Int {
    return opportunities
        .filter { it.impactCents > 0L }
        .take(3)
        .size
}

fun formatPremiumValueTracker(
    opportunities: List<PremiumOpportunity>,
    currencyCode: String,
    languageTag: String,
    hasPremiumAccess: Boolean
): PremiumValueTrackerCopy? {
    val monthlyProtection = trackedMonthlyProtectionCents(opportunities)
    if (monthlyProtection <= 0L) return null

    val count = trackedOpportunityCount(opportunities).coerceAtLeast(1)
    val locale = localeFor(languageTag)
    val amountText = monthlyProtection.toCurrencyString(currencyCode, locale)

    return if (hasPremiumAccess) {
        PremiumValueTrackerCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "Money Mochi can protect this month",
                spanish = "Dinero que Mochi puede proteger este mes",
                french = "Argent que Mochi peut proteger ce mois-ci",
                german = "Geld, das Mochi diesen Monat schutzen kann",
                portuguese = "Dinheiro que o Mochi pode proteger neste mes",
                arabic = "المال الذي يمكن لـ Mochi حمايته هذا الشهر",
                hindi = "इस महीने Mochi जितना पैसा बचाने में मदद कर सकता है",
                japanese = "今月 Mochi が守れそうなお金",
                swahili = "Pesa ambazo Mochi anaweza kusaidia kulinda mwezi huu",
                chinese = "本月 Mochi 可帮助守住的金额"
            ),
            amountText = amountText,
            body = whenLanguage(
                languageTag = languageTag,
                english = "Mochi has $count clear savings moves on your radar right now.",
                spanish = "Mochi tiene $count movimientos claros de ahorro en tu radar ahora mismo.",
                french = "Mochi a $count pistes claires d economie sur votre radar en ce moment.",
                german = "Mochi hat gerade $count klare Sparchancen fur dich im Blick.",
                portuguese = "O Mochi tem $count movimentos claros de economia no seu radar agora.",
                arabic = "لدى Mochi الآن $count خطوات واضحة للتوفير على رادارك.",
                hindi = "Mochi ने अभी आपके लिए $count साफ बचत मौके पहचान रखे हैं।",
                japanese = "Mochi は今、$count 件のはっきりした節約ポイントを見つけています。",
                swahili = "Mochi ana $count hatua wazi za akiba kwenye rada yako sasa hivi.",
                chinese = "Mochi 现在已经为你锁定了 $count 个明确的省钱动作。"
            )
        )
    } else {
        PremiumValueTrackerCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "Money Premium can help protect this month",
                spanish = "Dinero que Premium puede ayudar a proteger este mes",
                french = "Argent que Premium peut aider a proteger ce mois-ci",
                german = "Geld, das Premium diesen Monat schutzen kann",
                portuguese = "Dinheiro que o Premium pode ajudar a proteger neste mes",
                arabic = "المال الذي يمكن لـ Premium المساعدة في حمايته هذا الشهر",
                hindi = "इस महीने Premium जितना पैसा बचाने में मदद कर सकता है",
                japanese = "今月 Premium が守るのを手伝えるお金",
                swahili = "Pesa ambazo Premium inaweza kusaidia kulinda mwezi huu",
                chinese = "本月 Premium 可帮助守住的金额"
            ),
            amountText = amountText,
            body = whenLanguage(
                languageTag = languageTag,
                english = "Mochi already found $count real savings moves in your spending data.",
                spanish = "Mochi ya encontro $count movimientos reales de ahorro en tus datos.",
                french = "Mochi a deja trouve $count vraies pistes d economie dans vos donnees.",
                german = "Mochi hat bereits $count echte Sparchancen in deinen Daten gefunden.",
                portuguese = "O Mochi ja encontrou $count caminhos reais de economia nos seus dados.",
                arabic = "وجد Mochi بالفعل $count فرصا حقيقية للتوفير في بيانات إنفاقك.",
                hindi = "Mochi ने आपके खर्च डेटा में पहले ही $count असली बचत मौके ढूंढ लिए हैं।",
                japanese = "Mochi はあなたの支出データからすでに $count 件の本当の節約ポイントを見つけています。",
                swahili = "Mochi tayari ameona nafasi $count za kweli za kuokoa kwenye data yako ya matumizi.",
                chinese = "Mochi 已经从你的支出数据中找到了 $count 个真实的省钱机会。"
            )
        )
    }
}

fun formatMochiWeeklyAlert(
    opportunities: List<PremiumOpportunity>,
    currencyCode: String,
    languageTag: String,
    userName: String?
): MochiWeeklyAlertCopy? {
    val primaryOpportunity = opportunities.firstOrNull() ?: return null
    val summary = formatPremiumOutcomeSummary(
        opportunities = opportunities,
        currencyCode = currencyCode,
        languageTag = languageTag,
        hasPremiumAccess = true
    ) ?: return null
    val promptLead = userName?.trim().orEmpty().takeIf { it.isNotBlank() }
    val address = if (promptLead != null) "$promptLead, " else ""

    val title = whenLanguage(
        languageTag = languageTag,
        english = address + "Mochi found a money move for this week",
        spanish = address + "Mochi encontro una jugada de dinero para esta semana",
        french = address + "Mochi a trouve un vrai mouvement d argent pour cette semaine",
        german = address + "Mochi hat einen klaren Geldschritt fur diese Woche gefunden",
        portuguese = address + "O Mochi encontrou um movimento financeiro para esta semana",
        arabic = address + "وجد Mochi خطوة مالية واضحة لهذا الأسبوع",
        hindi = address + "Mochi ने इस हफ्ते के लिए एक साफ money move ढूंढा है",
        japanese = address + "Mochi が今週のはっきりしたお金の動きを見つけました",
        swahili = address + "Mochi ameona hatua ya pesa ya wiki hii",
        chinese = address + "Mochi 找到了本周最值得处理的一笔钱"
    )

    val body = when (primaryOpportunity.type) {
        PremiumOpportunityType.CATEGORY_CUTBACK -> whenLanguage(
            languageTag = languageTag,
            english = "${summary.headline}. ${primaryOpportunity.label} is the clearest place to start.",
            spanish = "${summary.headline}. ${primaryOpportunity.label} es el lugar mas claro para empezar.",
            french = "${summary.headline}. ${primaryOpportunity.label} est l endroit le plus clair pour commencer.",
            german = "${summary.headline}. ${primaryOpportunity.label} ist der klarste Startpunkt.",
            portuguese = "${summary.headline}. ${primaryOpportunity.label} e o ponto mais claro para comecar.",
            arabic = "${summary.headline}. ${primaryOpportunity.label} هو أوضح مكان للبدء.",
            hindi = "${summary.headline}. ${primaryOpportunity.label} सबसे साफ शुरुआत की जगह है।",
            japanese = "${summary.headline}. ${primaryOpportunity.label} から始めるのがいちばん分かりやすいです。",
            swahili = "${summary.headline}. ${primaryOpportunity.label} ndio sehemu ya wazi zaidi kuanzia.",
            chinese = "${summary.headline}。${primaryOpportunity.label} 是最值得先下手的地方。"
        )
        PremiumOpportunityType.BUDGET_PRESSURE -> whenLanguage(
            languageTag = languageTag,
            english = "${primaryOpportunity.label} is under pressure. Open Xpenze and Mochi will help you protect it early.",
            spanish = "${primaryOpportunity.label} esta bajo presion. Abre Xpenze y Mochi te ayudara a protegerlo a tiempo.",
            french = "${primaryOpportunity.label} est sous pression. Ouvrez Xpenze et Mochi vous aidera a le proteger a temps.",
            german = "${primaryOpportunity.label} steht unter Druck. Offne Xpenze und Mochi hilft dir, es rechtzeitig zu schutzen.",
            portuguese = "${primaryOpportunity.label} esta sob pressao. Abra o Xpenze e o Mochi vai ajudar voce a proteger isso cedo.",
            arabic = "${primaryOpportunity.label} تحت ضغط. افتح Xpenze وسيساعدك Mochi على حمايته مبكرا.",
            hindi = "${primaryOpportunity.label} दबाव में है। Xpenze खोलें, Mochi इसे जल्दी संभालने में मदद करेगा।",
            japanese = "${primaryOpportunity.label} は余裕がありません。Xpenze を開けば、Mochi が早めに守るのを手伝います。",
            swahili = "${primaryOpportunity.label} iko kwenye presha. Fungua Xpenze na Mochi atakusaidia kuilinda mapema.",
            chinese = "${primaryOpportunity.label} 预算正在吃紧。打开 Xpenze，Mochi 会帮你尽早守住它。"
        )
        else -> whenLanguage(
            languageTag = languageTag,
            english = "${summary.headline}. Open Xpenze and ask Mochi what to do first.",
            spanish = "${summary.headline}. Abre Xpenze y preguntale a Mochi que hacer primero.",
            french = "${summary.headline}. Ouvrez Xpenze et demandez a Mochi quoi faire d abord.",
            german = "${summary.headline}. Offne Xpenze und frag Mochi, was du zuerst tun solltest.",
            portuguese = "${summary.headline}. Abra o Xpenze e pergunte ao Mochi o que fazer primeiro.",
            arabic = "${summary.headline}. افتح Xpenze واسأل Mochi ما الذي يجب فعله أولا.",
            hindi = "${summary.headline}. Xpenze खोलें और Mochi से पूछें कि पहले क्या करना है।",
            japanese = "${summary.headline}. Xpenze を開いて、まず何をすべきか Mochi に聞いてください。",
            swahili = "${summary.headline}. Fungua Xpenze na umuulize Mochi uanze na nini.",
            chinese = "${summary.headline}。打开 Xpenze，问问 Mochi 先做什么。"
        )
    }

    return MochiWeeklyAlertCopy(title = title, body = body)
}

fun defaultMochiSuggestedPrompts(languageTag: String): List<String> = listOf(
    whenLanguage(
        languageTag = languageTag,
        english = "Where did most of my money go this month?",
        spanish = "En que se fue la mayor parte de mi dinero este mes?",
        french = "Ou est parti l essentiel de mon argent ce mois-ci ?",
        german = "Wofur ist der Grossteil meines Geldes diesen Monat draufgegangen?",
        portuguese = "Para onde foi a maior parte do meu dinheiro neste mes?",
        arabic = "أين ذهب معظم مالي هذا الشهر؟",
        hindi = "इस महीने मेरा ज्यादातर पैसा कहां गया?",
        japanese = "今月は何に一番お金を使った？",
        swahili = "Pesa yangu nyingi zimeenda wapi mwezi huu?",
        chinese = "我这个月大部分钱花到哪里了？"
    ),
    whenLanguage(
        languageTag = languageTag,
        english = "Which budget needs attention first?",
        spanish = "Que presupuesto necesita atencion primero?",
        french = "Quel budget a besoin d attention en premier ?",
        german = "Welches Budget braucht zuerst Aufmerksamkeit?",
        portuguese = "Qual orcamento precisa de atencao primeiro?",
        arabic = "أي ميزانية تحتاج إلى اهتمام أولا؟",
        hindi = "किस बजट को सबसे पहले ध्यान देने की जरूरत है?",
        japanese = "最初に注意すべき予算はどれ？",
        swahili = "Ni bajeti ipi inahitaji uangalizi kwanza?",
        chinese = "哪个预算最需要先关注？"
    ),
    whenLanguage(
        languageTag = languageTag,
        english = "What is the fastest way for me to save money this week?",
        spanish = "Cual es la forma mas rapida de ahorrar dinero esta semana?",
        french = "Quel est le moyen le plus rapide pour moi d economiser cette semaine ?",
        german = "Wie kann ich diese Woche am schnellsten Geld sparen?",
        portuguese = "Qual e a forma mais rapida de eu economizar dinheiro esta semana?",
        arabic = "ما أسرع طريقة يمكنني بها توفير المال هذا الأسبوع؟",
        hindi = "इस हफ्ते पैसे बचाने का मेरे लिए सबसे तेज तरीका क्या है?",
        japanese = "今週いちばん早く節約できる方法は？",
        swahili = "Ni njia gani ya haraka zaidi kwangu kuokoa pesa wiki hii?",
        chinese = "这周我最快的省钱方式是什么？"
    )
)

fun formatPremiumOpportunity(
    opportunity: PremiumOpportunity,
    currencyCode: String,
    languageTag: String
): PremiumOpportunityCopy {
    val locale = localeFor(languageTag)
    val impact = opportunity.impactCents.toCurrencyString(currencyCode, locale)
    val amount = opportunity.amountCents.toCurrencyString(currencyCode, locale)
    val comparison = opportunity.comparisonAmountCents.toCurrencyString(currencyCode, locale)

    return when (opportunity.type) {
        PremiumOpportunityType.CATEGORY_CUTBACK -> PremiumOpportunityCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "Quick save in ${opportunity.label}",
                spanish = "Ahorro rapido en ${opportunity.label}",
                french = "Economie rapide sur ${opportunity.label}",
                german = "Schnelle Ersparnis bei ${opportunity.label}",
                portuguese = "Economia rapida em ${opportunity.label}",
                arabic = "توفير سريع في ${opportunity.label}",
                hindi = "${opportunity.label} में जल्दी बचत",
                japanese = "${opportunity.label} で今すぐ節約",
                swahili = "Akiba ya haraka kwenye ${opportunity.label}",
                chinese = "${opportunity.label} 的快速节省"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "${opportunity.label} cost $amount recently. Cutting about 10% could save roughly $impact a month.",
                spanish = "${opportunity.label} costo $amount recientemente. Reducir cerca del 10% podria ahorrarte unos $impact al mes.",
                french = "${opportunity.label} a coute $amount recemment. Reduire d environ 10% pourrait vous faire economiser environ $impact par mois.",
                german = "${opportunity.label} kostete zuletzt $amount. Wenn du dort etwa 10% reduzierst, konntest du rund $impact im Monat sparen.",
                portuguese = "${opportunity.label} custou $amount recentemente. Reduzir cerca de 10% pode economizar aproximadamente $impact por mes.",
                arabic = "بلغ إنفاق ${opportunity.label} مؤخرا $amount. خفضه بحوالي 10% قد يوفر نحو $impact شهريا.",
                hindi = "${opportunity.label} पर हाल में $amount खर्च हुआ। लगभग 10% कटौती से करीब $impact प्रति माह बच सकता है।",
                japanese = "${opportunity.label} には最近 $amount かかっています。約10%減らせれば、月におよそ $impact 節約できます。",
                swahili = "${opportunity.label} imegharimu $amount hivi karibuni. Ukipunguza karibu 10%, unaweza kuokoa takribani $impact kwa mwezi.",
                chinese = "${opportunity.label} 最近花了 $amount。若减少大约 10%，每月大概可省下 $impact。"
            )
        )

        PremiumOpportunityType.CATEGORY_SPIKE -> PremiumOpportunityCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "${opportunity.label} is rising",
                spanish = "${opportunity.label} esta subiendo",
                french = "${opportunity.label} augmente",
                german = "${opportunity.label} steigt an",
                portuguese = "${opportunity.label} esta subindo",
                arabic = "${opportunity.label} يرتفع",
                hindi = "${opportunity.label} बढ़ रहा है",
                japanese = "${opportunity.label} が増えています",
                swahili = "${opportunity.label} inaongezeka",
                chinese = "${opportunity.label} 正在上升"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "You spent $amount here, up ${opportunity.percentValue}% from $comparison last month.",
                spanish = "Gastaste $amount aqui, un ${opportunity.percentValue}% mas que los $comparison del mes pasado.",
                french = "Vous avez depense $amount ici, soit ${opportunity.percentValue}% de plus que les $comparison du mois dernier.",
                german = "Du hast hier $amount ausgegeben, also ${opportunity.percentValue}% mehr als die $comparison im letzten Monat.",
                portuguese = "Voce gastou $amount aqui, ${opportunity.percentValue}% acima dos $comparison do mes passado.",
                arabic = "أنفقت هنا $amount، بزيادة ${opportunity.percentValue}% عن $comparison في الشهر الماضي.",
                hindi = "आपने यहां $amount खर्च किया, जो पिछले महीने के $comparison से ${opportunity.percentValue}% ज्यादा है।",
                japanese = "ここでの支出は $amount で、先月の $comparison より ${opportunity.percentValue}% 増えています。",
                swahili = "Umetumia $amount hapa, ambayo ni ${opportunity.percentValue}% juu ya $comparison ya mwezi uliopita.",
                chinese = "你在这里花了 $amount，比上个月的 $comparison 高出 ${opportunity.percentValue}%。"
            )
        )

        PremiumOpportunityType.BUDGET_PRESSURE -> PremiumOpportunityCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "${opportunity.label} is under pressure",
                spanish = "${opportunity.label} esta bajo presion",
                french = "${opportunity.label} est sous pression",
                german = "${opportunity.label} steht unter Druck",
                portuguese = "${opportunity.label} esta sob pressao",
                arabic = "${opportunity.label} تحت ضغط",
                hindi = "${opportunity.label} दबाव में है",
                japanese = "${opportunity.label} は余裕がありません",
                swahili = "${opportunity.label} iko kwenye presha",
                chinese = "${opportunity.label} 预算压力较大"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "You have already used ${opportunity.percentValue}% of this budget: $amount out of $comparison.",
                spanish = "Ya usaste el ${opportunity.percentValue}% de este presupuesto: $amount de $comparison.",
                french = "Vous avez deja utilise ${opportunity.percentValue}% de ce budget : $amount sur $comparison.",
                german = "Du hast bereits ${opportunity.percentValue}% dieses Budgets verbraucht: $amount von $comparison.",
                portuguese = "Voce ja usou ${opportunity.percentValue}% deste orcamento: $amount de $comparison.",
                arabic = "لقد استخدمت بالفعل ${opportunity.percentValue}% من هذه الميزانية: $amount من أصل $comparison.",
                hindi = "आप इस बजट का ${opportunity.percentValue}% पहले ही उपयोग कर चुके हैं: $amount / $comparison।",
                japanese = "この予算はすでに ${opportunity.percentValue}% 使用済みです: $amount / $comparison。",
                swahili = "Tayari umetumia ${opportunity.percentValue}% ya bajeti hii: $amount kati ya $comparison.",
                chinese = "这个预算已使用 ${opportunity.percentValue}%：$amount / $comparison。"
            )
        )

        PremiumOpportunityType.RECURRING_REVIEW -> PremiumOpportunityCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "Recurring bills are adding up",
                spanish = "Los pagos recurrentes se estan acumulando",
                french = "Les depenses recurrentes s accumulent",
                german = "Wiederkehrende Kosten summieren sich",
                portuguese = "As contas recorrentes estao pesando",
                arabic = "الفواتير المتكررة تتراكم",
                hindi = "आवर्ती बिल बढ़ रहे हैं",
                japanese = "定期支払いが積み上がっています",
                swahili = "Malipo ya kurudia yanaongezeka",
                chinese = "定期账单正在累积"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "${opportunity.countValue} repeating payments cost $amount recently. Reviewing them could free about $impact a month.",
                spanish = "${opportunity.countValue} pagos repetidos costaron $amount recientemente. Revisarlos podria liberar unos $impact al mes.",
                french = "${opportunity.countValue} paiements repetes ont coute $amount recemment. Les revoir pourrait liberer environ $impact par mois.",
                german = "${opportunity.countValue} wiederkehrende Zahlungen kosteten zuletzt $amount. Eine Prufung konnte etwa $impact im Monat freimachen.",
                portuguese = "${opportunity.countValue} pagamentos repetidos custaram $amount recentemente. Revisar isso pode liberar cerca de $impact por mes.",
                arabic = "بلغت تكلفة ${opportunity.countValue} دفعات متكررة مؤخرا $amount. مراجعتها قد توفر نحو $impact شهريا.",
                hindi = "${opportunity.countValue} आवर्ती भुगतानों पर हाल में $amount खर्च हुआ। इन्हें देखने से लगभग $impact प्रति माह बच सकता है।",
                japanese = "${opportunity.countValue} 件の定期支払いで最近 $amount かかっています。見直せば月に約 $impact 浮く可能性があります。",
                swahili = "Malipo ${opportunity.countValue} yanayojirudia yamegharimu $amount hivi karibuni. Kuyapitia kunaweza kuachilia takribani $impact kwa mwezi.",
                chinese = "最近 ${opportunity.countValue} 笔定期付款共花费 $amount。检查它们每月大约可释放 $impact。"
            )
        )

        PremiumOpportunityType.BUDGET_SUGGESTION -> PremiumOpportunityCopy(
            title = whenLanguage(
                languageTag = languageTag,
                english = "A ${opportunity.label} budget would help",
                spanish = "Un presupuesto para ${opportunity.label} ayudaria",
                french = "Un budget ${opportunity.label} aiderait",
                german = "Ein Budget fur ${opportunity.label} wurde helfen",
                portuguese = "Um orcamento para ${opportunity.label} ajudaria",
                arabic = "سيكون من المفيد إنشاء ميزانية لـ ${opportunity.label}",
                hindi = "${opportunity.label} के लिए बजट मदद करेगा",
                japanese = "${opportunity.label} の予算を作ると役立ちます",
                swahili = "Bajeti ya ${opportunity.label} itasaidia",
                chinese = "为 ${opportunity.label} 设置预算会更有帮助"
            ),
            body = whenLanguage(
                languageTag = languageTag,
                english = "Your recent average is $comparison. A starting budget around $amount would fit your pattern better.",
                spanish = "Tu promedio reciente es $comparison. Un presupuesto inicial cerca de $amount encajaria mejor con tu patron.",
                french = "Votre moyenne recente est de $comparison. Un budget de depart autour de $amount correspondrait mieux a votre tendance.",
                german = "Dein aktueller Durchschnitt liegt bei $comparison. Ein Startbudget von etwa $amount passt besser zu deinem Muster.",
                portuguese = "Sua media recente e $comparison. Um orcamento inicial em torno de $amount combinaria melhor com seu padrao.",
                arabic = "متوسطك الأخير هو $comparison. ستكون ميزانية ابتدائية بحدود $amount أنسب لنمطك.",
                hindi = "आपका हाल का औसत $comparison है। लगभग $amount का शुरुआती बजट आपके पैटर्न के लिए बेहतर रहेगा।",
                japanese = "最近の平均は $comparison です。約 $amount の予算から始めると今の傾向に合います。",
                swahili = "Wastani wako wa hivi karibuni ni $comparison. Bajeti ya kuanzia karibu $amount italingana vizuri zaidi na mwenendo wako.",
                chinese = "你最近的平均值是 $comparison。设置大约 $amount 的起始预算会更贴合你的消费模式。"
            )
        )
    }
}

private fun whenLanguage(
    languageTag: String,
    english: String,
    spanish: String,
    french: String,
    german: String,
    portuguese: String,
    arabic: String,
    hindi: String,
    japanese: String,
    swahili: String,
    chinese: String
): String = when {
    languageTag.startsWith("es") -> spanish
    languageTag.startsWith("fr") -> french
    languageTag.startsWith("de") -> german
    languageTag.startsWith("pt") -> portuguese
    languageTag.startsWith("ar") -> arabic
    languageTag.startsWith("hi") -> hindi
    languageTag.startsWith("ja") -> japanese
    languageTag.startsWith("sw") -> swahili
    languageTag.startsWith("zh") -> chinese
    else -> english
}

private fun localeFor(languageTag: String): Locale {
    return runCatching { Locale.forLanguageTag(languageTag) }
        .getOrDefault(Locale.getDefault())
}
