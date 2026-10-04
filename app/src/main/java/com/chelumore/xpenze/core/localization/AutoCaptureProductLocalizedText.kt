package com.chelumore.xpenze.core.localization

import java.util.Locale

internal data class AutoCaptureHomeCopy(
    val title: String,
    val body: String,
    val primaryCta: String
)

internal data class AutoCaptureSetupCopy(
    val screenTitle: String,
    val heroTitle: String,
    val heroBody: String,
    val whatItDoesTitle: String,
    val whatItDoesItems: List<String>,
    val whatItNeverDoesTitle: String,
    val whatItNeverDoesItems: List<String>,
    val mistakesTitle: String,
    val mistakesItems: List<String>,
    val permissionsTitle: String,
    val permissionsItems: List<String>,
    val turnOnNowTitle: String,
    val turnOnNowItems: List<String>,
    val primaryCta: String,
    val secondaryCta: String
)

internal fun localizedAutoCaptureHomeCopy(
    languageTag: String,
    hasPremiumAccess: Boolean,
    autoCaptureEnabled: Boolean,
    pendingDraftCount: Int
): AutoCaptureHomeCopy {
    val normalized = normalizeAutoCaptureUxTag(languageTag)
    val title = when {
        !hasPremiumAccess -> pickAutoCaptureUxText(
            normalized,
            en = "Stop typing every expense manually",
            lv = "Pārtrauciet rakstīt katru izdevumu manuāli",
            es = "Deja de escribir cada gasto a mano",
            fr = "N'ecrivez plus chaque depense a la main",
            de = "Schreiben Sie nicht mehr jede Ausgabe von Hand",
            pt = "Pare de digitar cada gasto manualmente",
            ar = "توقف عن كتابة كل مصروف يدويًا",
            hi = "हर खर्च हाथ से टाइप करना बंद करें",
            ja = "もう支出を毎回手入力しない",
            sw = "Acha kuandika kila matumizi kwa mkono",
            zh = "别再手动输入每一笔支出"
        )
        autoCaptureEnabled -> localizedAutoCaptureSettingsTitle(normalized)
        else -> localizedAutoCaptureSettingsTitle(normalized)
    }
    val body = when {
        !hasPremiumAccess -> pickAutoCaptureUxText(
            normalized,
            en = "Premium turns statement files, screenshots, receipts, familiar merchants, and supported money alerts into safe drafts you review first.",
            lv = "Premium pārvērš izrakstu failus, ekrānuzņēmumus, kvītis, pazīstamus tirgotājus un atbalstītos naudas brīdinājumus drošos melnrakstos, kurus vispirms pārskatāt.",
            es = "Premium convierte extractos, capturas, recibos, comercios conocidos y alertas de dinero compatibles en borradores seguros para revisar primero.",
            fr = "Premium transforme releves, captures, recus, marchands connus et alertes d'argent compatibles en brouillons surs a verifier d'abord.",
            de = "Premium macht aus Auszugen, Screenshots, Belegen, bekannten Handlern und passenden Geldalarmen sichere Entwurfe zur Prufung.",
            pt = "O Premium transforma extratos, capturas, recibos, comerciantes conhecidos e alertas de dinheiro compativeis em rascunhos seguros para sua revisao.",
            ar = "يحوّل Premium كشوف الحساب واللقطات والإيصالات والمتاجر المألوفة والتنبيهات المالية المدعومة إلى مسودات آمنة تراجعها أولاً.",
            hi = "Premium स्टेटमेंट, स्क्रीनशॉट, रसीद, जाने-पहचाने मर्चेंट और समर्थित मनी अलर्ट को सुरक्षित ड्राफ्ट में बदल देता है जिन्हें आप पहले जांचते हैं।",
            ja = "Premium では、明細、スクリーンショット、レシート、よく使う加盟店、対応する入出金通知を、まず確認できる安全な下書きに変えます。",
            sw = "Premium hubadilisha taarifa, picha za skrini, risiti, wafanyabiashara wa kawaida, na arifa za pesa zinazotumika kuwa rasimu salama za kukagua kwanza.",
            zh = "Premium 会把账单、截图、收据、常见商户和受支持的资金提醒变成安全草稿，先由你审核。"
        )
        autoCaptureEnabled && pendingDraftCount > 0 -> pickAutoCaptureUxText(
            normalized,
            en = "$pendingDraftCount draft(s) are waiting in your Draft Inbox.",
            lv = "$pendingDraftCount melnraksts(-i) gaida melnrakstu iesūtnē.",
            es = "Hay $pendingDraftCount borrador(es) esperandote en tu Bandeja de borradores.",
            fr = "$pendingDraftCount brouillon(s) vous attendent dans votre Boite de brouillons.",
            de = "$pendingDraftCount Entwurf(e) warten in Ihrem Entwurfs-Posteingang.",
            pt = "Ha $pendingDraftCount rascunho(s) aguardando na sua Caixa de rascunhos.",
            ar = "هناك $pendingDraftCount مسودة/مسودات تنتظرك في صندوق المسودات.",
            hi = "आपके Draft Inbox में $pendingDraftCount ड्राफ्ट इंतजार कर रहे हैं।",
            ja = "下書き受信箱に $pendingDraftCount 件の下書きが待機しています。",
            sw = "Kuna rasimu $pendingDraftCount zinakusubiri kwenye Kikasha cha Rasimu.",
            zh = "你的草稿收件箱中有 $pendingDraftCount 个草稿等待处理。"
        )
        autoCaptureEnabled -> pickAutoCaptureUxText(
            normalized,
            en = "Automatic mode is on. Import a statement, scan a receipt, or turn on notification capture to start filling your Draft Inbox.",
            lv = "Automātiskais režīms ir ieslēgts. Importējiet izrakstu, skenējiet kvīti vai ieslēdziet paziņojumu tveršanu, lai sāktu aizpildīt melnrakstu iesūtni.",
            es = "El modo automatico esta activado. Importa un extracto, escanea un recibo o activa la captura de notificaciones para llenar tu Bandeja de borradores.",
            fr = "Le mode automatique est active. Importez un releve, scannez un recu ou activez la capture des notifications pour remplir votre Boite de brouillons.",
            de = "Der automatische Modus ist aktiviert. Importieren Sie einen Auszug, scannen Sie einen Beleg oder aktivieren Sie die Benachrichtigungserfassung, um Entwurfe zu sammeln.",
            pt = "O modo automatico esta ligado. Importe um extrato, escaneie um recibo ou ative a captura de notificacoes para preencher sua Caixa de rascunhos.",
            ar = "الوضع التلقائي قيد التشغيل. استورد كشفًا أو امسح إيصالاً أو فعّل التقاط الإشعارات لبدء ملء صندوق المسودات.",
            hi = "ऑटोमैटिक मोड चालू है। Draft Inbox भरने के लिए स्टेटमेंट इम्पोर्ट करें, रसीद स्कैन करें या notification capture चालू करें।",
            ja = "自動モードはオンです。明細を取り込むか、レシートを読み取るか、通知取り込みを有効にして下書き受信箱を使い始めましょう。",
            sw = "Hali ya kiotomatiki imewashwa. Ingiza taarifa, skani risiti, au washa ukamataji wa arifa ili kuanza kujaza Kikasha cha Rasimu.",
            zh = "自动模式已开启。导入账单、扫描收据或开启通知捕捉，即可开始填充草稿收件箱。"
        )
        else -> pickAutoCaptureUxText(
            normalized,
            en = "Automatic mode is off. Turn it on to let Xpenze collect possible transactions as drafts instead of making you start from scratch.",
            lv = "Automātiskais režīms ir izslēgts. Ieslēdziet to, lai ļautu Xpenze apkopot iespējamos darījumus kā melnrakstus, nevis likt jums sākt no nulles.",
            es = "El modo automatico esta apagado. Activalo para que Xpenze recopile posibles transacciones como borradores en lugar de hacerte empezar desde cero.",
            fr = "Le mode automatique est desactive. Activez-le pour que Xpenze recueille les transactions possibles sous forme de brouillons.",
            de = "Der automatische Modus ist aus. Aktivieren Sie ihn, damit Xpenze mogliche Transaktionen als Entwurfe sammelt, statt Sie bei null anfangen zu lassen.",
            pt = "O modo automatico esta desligado. Ative-o para que o Xpenze colete possiveis transacoes como rascunhos em vez de fazer voce comecar do zero.",
            ar = "الوضع التلقائي متوقف. فعّله ليجمع Xpenze المعاملات المحتملة كمسودات بدل أن تبدأ من الصفر كل مرة.",
            hi = "ऑटोमैटिक मोड बंद है। इसे चालू करें ताकि Xpenze संभावित ट्रांजैक्शन को ड्राफ्ट के रूप में इकट्ठा करे और आपको हर बार शुरुआत से टाइप न करना पड़े।",
            ja = "自動モードはオフです。オンにすると、Xpenze が候補の取引を下書きとして集め、毎回最初から手入力しなくてよくなります。",
            sw = "Hali ya kiotomatiki imezimwa. Iwashe ili Xpenze ikusanye miamala inayowezekana kama rasimu badala ya kukuanzisha upya kila wakati.",
            zh = "自动模式当前关闭。开启后，Xpenze 会把可能的交易先收集成草稿，而不是每次都让你从零开始输入。"
        )
    }
    val primaryCta = when {
        !hasPremiumAccess -> pickAutoCaptureUxText(
            normalized,
            en = "See how it works",
            lv = "Skatiet, kā tas darbojas",
            es = "Ver como funciona",
            fr = "Voir comment cela marche",
            de = "So funktioniert es",
            pt = "Ver como funciona",
            ar = "شاهد كيف يعمل",
            hi = "देखें यह कैसे काम करता है",
            ja = "仕組みを見る",
            sw = "Ona jinsi inavyofanya kazi",
            zh = "看看它怎么工作"
        )
        autoCaptureEnabled -> localizedAutoCaptureDraftInboxTitle(normalized)
        else -> pickAutoCaptureUxText(
            normalized,
            en = "Set up automatic capture",
            lv = "Iestatiet automātisko uztveršanu",
            es = "Configurar captura automatica",
            fr = "Configurer la capture automatique",
            de = "Automatische Erfassung einrichten",
            pt = "Configurar captura automatica",
            ar = "إعداد الالتقاط التلقائي",
            hi = "ऑटोमैटिक कैप्चर सेट करें",
            ja = "自動取り込みを設定",
            sw = "Sanidi ukamataji wa kiotomatiki",
            zh = "设置自动捕捉"
        )
    }
    return AutoCaptureHomeCopy(
        title = title,
        body = body,
        primaryCta = primaryCta
    )
}

internal fun localizedAutoCaptureSetupCopy(
    languageTag: String,
    hasPremiumAccess: Boolean,
    autoCaptureEnabled: Boolean
): AutoCaptureSetupCopy {
    val normalized = normalizeAutoCaptureUxTag(languageTag)
    return AutoCaptureSetupCopy(
        screenTitle = pickAutoCaptureUxText(
            normalized,
            en = "How Automatic Capture Works",
            lv = "Kā darbojas automātiskā tveršana",
            es = "Como funciona la captura automatica",
            fr = "Comment fonctionne la capture automatique",
            de = "So funktioniert die automatische Erfassung",
            pt = "Como a captura automatica funciona",
            ar = "كيف يعمل الالتقاط التلقائي",
            hi = "ऑटोमैटिक कैप्चर कैसे काम करता है",
            ja = "自動取り込みの仕組み",
            sw = "Jinsi ukamataji wa kiotomatiki unavyofanya kazi",
            zh = "自动捕捉如何工作"
        ),
        heroTitle = pickAutoCaptureUxText(
            normalized,
            en = "Simple version: Xpenze helps you collect money events, but you stay in control.",
            lv = "Vienkārša versija: Xpenze palīdz iekasēt naudas notikumus, taču jūs saglabājat kontroli.",
            es = "Version simple: Xpenze te ayuda a reunir movimientos de dinero, pero tu mantienes el control.",
            fr = "Version simple : Xpenze vous aide a rassembler les mouvements d'argent, mais vous gardez le controle.",
            de = "Kurz gesagt: Xpenze sammelt mogliche Geldbewegungen, aber Sie behalten die Kontrolle.",
            pt = "Versao simples: o Xpenze ajuda voce a reunir movimentacoes de dinheiro, mas voce continua no controle.",
            ar = "ببساطة: يساعدك Xpenze على جمع الحركات المالية، لكنك تظل صاحب القرار.",
            hi = "सरल शब्दों में: Xpenze पैसों की गतिविधियां इकट्ठा करने में मदद करता है, लेकिन नियंत्रण आपके हाथ में रहता है।",
            ja = "簡単に言うと、Xpenze はお金の動きを集めるのを手伝いますが、最終判断はあなたです。",
            sw = "Kwa ufupi: Xpenze hukusaidia kukusanya matukio ya pesa, lakini udhibiti unabaki kwako.",
            zh = "简单说：Xpenze 帮你收集资金动态，但决定权始终在你手里。"
        ),
        heroBody = pickAutoCaptureUxText(
            normalized,
            en = "It does not move money. It does not approve payments. It does not change your balances on its own. It only creates drafts for you to review, edit, save, or ignore.",
            lv = "Tas naudu nepārvieto. Tas neapstiprina maksājumus. Tas pats par sevi nemaina jūsu atlikumus. Tas veido tikai melnrakstus, ko varat pārskatīt, rediģēt, saglabāt vai ignorēt.",
            es = "No mueve dinero. No aprueba pagos. No cambia tus saldos por si solo. Solo crea borradores para que los revises, edites, guardes o ignores.",
            fr = "Il ne deplace pas d'argent. Il n'approuve pas les paiements. Il ne change pas vos soldes tout seul. Il cree seulement des brouillons a verifier, modifier, enregistrer ou ignorer.",
            de = "Es bewegt kein Geld. Es genehmigt keine Zahlungen. Es andert Ihre Salden nicht selbststandig. Es erstellt nur Entwurfe zum Prufen, Bearbeiten, Speichern oder Ignorieren.",
            pt = "Nao movimenta dinheiro. Nao aprova pagamentos. Nao altera seus saldos sozinho. Apenas cria rascunhos para voce revisar, editar, salvar ou ignorar.",
            ar = "لا ينقل الأموال. ولا يوافق على المدفوعات. ولا يغيّر أرصدتك من تلقاء نفسه. بل ينشئ فقط مسودات لتراجعها أو تعدّلها أو تحفظها أو تتجاهلها.",
            hi = "यह पैसा नहीं भेजता। यह भुगतान स्वीकृत नहीं करता। यह अपने आप आपके बैलेंस नहीं बदलता। यह सिर्फ ऐसे ड्राफ्ट बनाता है जिन्हें आप देख, बदल, सेव या नज़रअंदाज़ कर सकते हैं।",
            ja = "お金を動かすことはありません。支払いを承認することもありません。残高を勝手に変えることもありません。確認、編集、保存、無視ができる下書きを作るだけです。",
            sw = "Haihamishi pesa. Haiidhinishi malipo. Haibadilishi salio lako yenyewe. Inatengeneza tu rasimu unazoweza kukagua, kuhariri, kuhifadhi, au kupuuza.",
            zh = "它不会转移资金，不会批准付款，也不会自行修改你的余额。它只会创建草稿，供你审核、编辑、保存或忽略。"
        ),
        whatItDoesTitle = pickAutoCaptureUxText(
            normalized,
            en = "What it does",
            lv = "Ko tas dara",
            es = "Lo que hace",
            fr = "Ce que cela fait",
            de = "Was es tut",
            pt = "O que faz",
            ar = "ما الذي يفعله",
            hi = "यह क्या करता है",
            ja = "できること",
            sw = "Inachofanya",
            zh = "它会做什么"
        ),
        whatItDoesItems = listOf(
            pickAutoCaptureUxText(
                normalized,
                en = "Imports statement CSV files into a Draft Inbox instead of posting transactions directly.",
                lv = "Importē izraksta CSV failus melnrakstu iesūtnē, nevis tieši publicē darījumus.",
                es = "Importa archivos CSV de extractos a una Bandeja de borradores en lugar de registrar transacciones directamente.",
                fr = "Importe les fichiers CSV de releves dans une Boite de brouillons au lieu d'ajouter directement des transactions.",
                de = "Importiert Auszugs-CSV-Dateien in einen Entwurfs-Posteingang, statt Transaktionen direkt zu verbuchen.",
                pt = "Importa arquivos CSV de extratos para uma Caixa de rascunhos em vez de lancar transacoes diretamente.",
                ar = "يستورد ملفات CSV الخاصة بالكشوف إلى صندوق المسودات بدلاً من إضافة المعاملات مباشرة.",
                hi = "यह स्टेटमेंट CSV फाइलों को सीधे ट्रांजैक्शन बनाने के बजाय Draft Inbox में लाता है।",
                ja = "明細 CSV を直接取引にするのではなく、下書き受信箱に取り込みます。",
                sw = "Huleta faili za CSV za taarifa kwenye Kikasha cha Rasimu badala ya kuongeza miamala moja kwa moja.",
                zh = "它会把账单 CSV 导入草稿收件箱，而不是直接记成交易。"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Reads screenshots and receipts to pull out likely amounts, merchants, and dates.",
                lv = "Nolasa ekrānuzņēmumus un kvītis, lai noskaidrotu iespējamās summas, tirgotājus un datumus.",
                es = "Lee capturas y recibos para extraer importes, comercios y fechas probables.",
                fr = "Lit les captures et recus pour extraire les montants, marchands et dates probables.",
                de = "Liest Screenshots und Belege, um wahrscheinliche Betrage, Handler und Daten zu erkennen.",
                pt = "Le capturas e recibos para extrair valores, comerciantes e datas provaveis.",
                ar = "يقرأ اللقطات والإيصالات لاستخراج المبالغ والمتاجر والتواريخ المحتملة.",
                hi = "यह स्क्रीनशॉट और रसीदें पढ़कर संभावित राशि, मर्चेंट और तारीख निकालता है।",
                ja = "スクリーンショットやレシートから、金額、加盟店、日付の候補を読み取ります。",
                sw = "Husoma picha za skrini na risiti ili kupata kiasi, mfanyabiashara, na tarehe zinazowezekana.",
                zh = "它会读取截图和收据，提取可能的金额、商户和日期。"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Optionally watches supported transaction notifications and saves matching ones as drafts.",
                lv = "Pēc izvēles skatās atbalstītos darījumu paziņojumus un saglabā atbilstošos paziņojumus kā melnrakstus.",
                es = "Opcionalmente vigila notificaciones de transacciones compatibles y guarda las que coincidan como borradores.",
                fr = "Peut surveiller les notifications de transaction compatibles et enregistrer les correspondances en brouillons.",
                de = "Kann passende Transaktionsbenachrichtigungen uberwachen und als Entwurfe speichern.",
                pt = "Opcionalmente observa notificacoes de transacoes compativeis e salva as correspondentes como rascunhos.",
                ar = "يراقب اختياريًا إشعارات المعاملات المدعومة ويحفظ المطابق منها كمسودات.",
                hi = "वैकल्पिक रूप से यह समर्थित ट्रांजैक्शन नोटिफिकेशन देखता है और मेल खाने वाले को ड्राफ्ट के रूप में सेव करता है।",
                ja = "必要に応じて対応する取引通知を監視し、一致したものを下書きとして保存します。",
                sw = "Kwa hiari huangalia arifa za miamala zinazotumika na kuhifadhi zinazolingana kama rasimu.",
                zh = "它还可以选择性监控受支持的交易通知，并把匹配的内容保存为草稿。"
            )
        ),
        whatItNeverDoesTitle = pickAutoCaptureUxText(
            normalized,
            en = "What it never does",
            lv = "Ko tas nekad nedara",
            es = "Lo que nunca hace",
            fr = "Ce qu'il ne fait jamais",
            de = "Was es niemals tut",
            pt = "O que nunca faz",
            ar = "ما الذي لا يفعله أبدًا",
            hi = "यह कभी क्या नहीं करता",
            ja = "絶対にしないこと",
            sw = "Kile ambacho haifanyi kamwe",
            zh = "它绝不会做什么"
        ),
        whatItNeverDoesItems = listOf(
            pickAutoCaptureUxText(
                normalized,
                en = "It never sends money, takes loans, pays bills, or approves transfers for you.",
                lv = "Tā nekad nesūta naudu, neņem aizdevumus, nemaksā rēķinus vai neapstiprina pārskaitījumus jūsu vietā.",
                es = "Nunca envia dinero, toma prestamos, paga facturas ni aprueba transferencias por ti.",
                fr = "Il n'envoie jamais d'argent, ne prend pas de pret, ne paie pas de factures et n'approuve pas de virements a votre place.",
                de = "Es sendet niemals Geld, nimmt keine Kredite auf, bezahlt keine Rechnungen und genehmigt keine Uberweisungen fur Sie.",
                pt = "Nunca envia dinheiro, faz emprestimos, paga contas nem aprova transferencias por voce.",
                ar = "لا يرسل الأموال، ولا يأخذ قروضًا، ولا يدفع الفواتير، ولا يوافق على التحويلات نيابةً عنك.",
                hi = "यह कभी आपके लिए पैसा नहीं भेजता, लोन नहीं लेता, बिल नहीं भरता और ट्रांसफर स्वीकृत नहीं करता।",
                ja = "送金、借入、請求の支払い、振込の承認を代わりに行うことはありません。",
                sw = "Haiwahi kutuma pesa, kuchukua mkopo, kulipa bili, au kuidhinisha uhamisho kwa niaba yako.",
                zh = "它绝不会替你转钱、贷款、缴费或批准转账。"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Nothing touches your real money history until you review it and save it yourself.",
                lv = "Nekas neskar jūsu īstās naudas vēsturi, kamēr neesat to pārskatījis un saglabājis pats.",
                es = "Nada toca tu historial real de dinero hasta que lo revises y lo guardes tu mismo.",
                fr = "Rien ne touche a votre historique financier reel tant que vous ne l'avez pas verifie et enregistre vous-meme.",
                de = "Nichts verandert Ihre echte Geldhistorie, bis Sie es selbst prufen und speichern.",
                pt = "Nada toca seu historico real de dinheiro ate que voce revise e salve por conta propria.",
                ar = "لا شيء يلمس سجلك المالي الحقيقي حتى تراجعه وتحفظه بنفسك.",
                hi = "जब तक आप खुद समीक्षा करके सेव नहीं करते, तब तक आपकी असली मनी हिस्ट्री में कुछ नहीं जाता।",
                ja = "自分で確認して保存するまでは、実際のお金の履歴には何も反映されません。",
                sw = "Hakuna kinachoingia kwenye historia yako halisi ya pesa mpaka wewe mwenyewe ukague na kuhifadhi.",
                zh = "在你亲自审核并保存之前，它不会动你的真实资金记录。"
            )
        ),
        mistakesTitle = pickAutoCaptureUxText(
            normalized,
            en = "How Xpenze reduces mistakes",
            lv = "Kā Xpenze samazina kļūdas",
            es = "Como reduce errores Xpenze",
            fr = "Comment Xpenze reduit les erreurs",
            de = "So reduziert Xpenze Fehler",
            pt = "Como o Xpenze reduz erros",
            ar = "كيف يقلل Xpenze الأخطاء",
            hi = "Xpenze गलतियां कैसे कम करता है",
            ja = "Xpenze がミスを減らす方法",
            sw = "Jinsi Xpenze inavyopunguza makosa",
            zh = "Xpenze 如何减少错误"
        ),
        mistakesItems = listOf(
            pickAutoCaptureUxText(
                normalized,
                en = "Unclear or risky items stay in Needs review instead of pretending certainty.",
                lv = "Neskaidri vai riskanti priekšmeti paliek sadaļā Nepieciešams pārskatīt, nevis izlikties par noteiktību.",
                es = "Los elementos poco claros o riesgosos se quedan en Necesita revision en lugar de fingir certeza.",
                fr = "Les elements flous ou risqués restent dans A verifier au lieu de faire semblant d'etre certains.",
                de = "Unklare oder riskante Elemente bleiben unter Prufung statt falsche Sicherheit vorzutauschen.",
                pt = "Itens incertos ou arriscados ficam em Precisa de revisao em vez de fingir certeza.",
                ar = "العناصر غير الواضحة أو الخطرة تبقى تحت \"تحتاج مراجعة\" بدلاً من الادعاء بأنها مؤكدة.",
                hi = "अस्पष्ट या जोखिम वाले आइटम सीधे तय नहीं किए जाते, बल्कि Needs review में रखे जाते हैं।",
                ja = "曖昧またはリスクのある項目は、確定したふりをせず「要確認」に残ります。",
                sw = "Vitu visivyo wazi au vya hatari hubaki kwenye Inahitaji ukaguzi badala ya kujifanya kuwa na uhakika.",
                zh = "不明确或风险较高的项目会停留在“需要审核”，而不会假装已经确定。"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "You choose whether a draft is an expense, income, transfer, reimbursement, pass-through, or excluded.",
                lv = "Jūs izvēlaties, vai melnraksts ir izdevumi, ienākumi, pārvedums, atlīdzība, caurlaide vai izslēgta.",
                es = "Tu eliges si un borrador es gasto, ingreso, transferencia, reembolso, paso temporal o excluido.",
                fr = "C'est vous qui decidez si un brouillon est une depense, un revenu, un transfert, un remboursement, un pass-through ou une exclusion.",
                de = "Sie entscheiden, ob ein Entwurf Ausgabe, Einnahme, Transfer, Erstattung, Durchlaufposten oder ausgeschlossen ist.",
                pt = "Voce decide se um rascunho e despesa, receita, transferencia, reembolso, pass-through ou excluido.",
                ar = "أنت من يختار ما إذا كانت المسودة مصروفًا أو دخلاً أو تحويلًا أو استردادًا أو مرورًا مؤقتًا أو مستبعدة.",
                hi = "आप तय करते हैं कि ड्राफ्ट खर्च है, आय है, ट्रांसफर है, reimbursement है, pass-through है या exclude करना है।",
                ja = "その下書きが支出、収入、振替、立替返金、通過項目、除外のどれかはあなたが選びます。",
                sw = "Wewe ndiye unaamua kama rasimu ni matumizi, mapato, uhamisho, marejesho, pass-through, au iondolewe.",
                zh = "草稿是支出、收入、转账、报销、代收代付还是排除，都由你来决定。"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Duplicate checks help stop the same statement row or alert from showing up again and again.",
                lv = "Dublētas pārbaudes palīdz novērst vienas un tās pašas paziņojuma rindas vai brīdinājuma rādīšanu atkal un atkal.",
                es = "Las comprobaciones de duplicados ayudan a evitar que la misma fila o alerta aparezca una y otra vez.",
                fr = "Les verifications de doublons evitent que la meme ligne ou alerte revienne encore et encore.",
                de = "Dublettenkontrollen helfen, dieselbe Auszugszeile oder Warnung nicht immer wieder anzuzeigen.",
                pt = "As verificacoes de duplicidade ajudam a impedir que a mesma linha ou alerta apareca repetidamente.",
                ar = "فحوصات التكرار تساعد على منع ظهور نفس السطر أو التنبيه مرارًا وتكرارًا.",
                hi = "डुप्लिकेट जांच एक ही स्टेटमेंट लाइन या अलर्ट को बार-बार आने से रोकती है।",
                ja = "重複チェックにより、同じ明細行や通知が何度も出てこないようにします。",
                sw = "Ukaguzi wa nakala husaidia kuzuia mstari au arifa ileile kuonekana tena na tena.",
                zh = "重复检查可以防止同一条账单记录或提醒反复出现。"
            )
        ),
        permissionsTitle = pickAutoCaptureUxText(
            normalized,
            en = "Permissions and access",
            lv = "Atļaujas un piekļuve",
            es = "Permisos y acceso",
            fr = "Autorisations et acces",
            de = "Berechtigungen und Zugriff",
            pt = "Permissoes e acesso",
            ar = "الأذونات والوصول",
            hi = "अनुमतियां और एक्सेस",
            ja = "権限とアクセス",
            sw = "Ruhusa na ufikiaji",
            zh = "权限与访问"
        ),
        permissionsItems = listOf(
            pickAutoCaptureUxText(
                normalized,
                en = "Statement import and receipt scan only work when you choose a file or image yourself.",
                lv = "Izrakstu importēšana un kvīšu skenēšana darbojas tikai tad, ja pats izvēlaties failu vai attēlu.",
                es = "La importacion de extractos y el escaneo de recibos solo funcionan cuando eliges un archivo o imagen por tu cuenta.",
                fr = "L'import de releves et le scan de recus ne fonctionnent que lorsque vous choisissez vous-meme un fichier ou une image.",
                de = "Auszugsimport und Belegscan funktionieren nur, wenn Sie selbst eine Datei oder ein Bild auswahlen.",
                pt = "A importacao de extratos e o escaneamento de recibos so funcionam quando voce escolhe um arquivo ou imagem.",
                ar = "يعمل استيراد الكشوف ومسح الإيصالات فقط عندما تختار بنفسك ملفًا أو صورة.",
                hi = "स्टेटमेंट इम्पोर्ट और रसीद स्कैन तभी काम करते हैं जब आप खुद कोई फाइल या इमेज चुनते हैं।",
                ja = "明細の取り込みとレシート読み取りは、自分でファイルや画像を選んだときだけ動作します。",
                sw = "Uingizaji wa taarifa na uskanishaji wa risiti hufanya kazi tu unapochagua faili au picha mwenyewe.",
                zh = "账单导入和收据扫描只有在你亲自选择文件或图片时才会工作。"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Notification capture needs Android notification access, and it stays off until you grant that access.",
                lv = "Paziņojumu tveršanai ir nepieciešama Android paziņojumu piekļuve, un tā paliek izslēgta, līdz piešķirat šo piekļuvi.",
                es = "La captura de notificaciones necesita acceso de Android a las notificaciones, y seguira apagada hasta que lo concedas.",
                fr = "La capture des notifications a besoin de l'acces Android aux notifications et reste desactivee jusqu'a votre autorisation.",
                de = "Die Benachrichtigungserfassung benotigt Android-Benachrichtigungszugriff und bleibt aus, bis Sie ihn erlauben.",
                pt = "A captura de notificacoes precisa do acesso do Android as notificacoes e permanece desligada ate voce permitir.",
                ar = "يحتاج التقاط الإشعارات إلى صلاحية وصول Android إلى الإشعارات، وسيبقى مغلقًا حتى تمنح هذه الصلاحية.",
                hi = "Notification capture के लिए Android notification access चाहिए, और आपकी अनुमति तक यह बंद रहता है।",
                ja = "通知取り込みには Android の通知アクセスが必要で、その許可を与えるまでオフのままです。",
                sw = "Ukamataji wa arifa unahitaji ufikiaji wa arifa za Android, na hubaki kuzimwa mpaka uruhusu.",
                zh = "通知捕捉需要 Android 通知访问权限，在你授权之前它会保持关闭。"
            )
        ),
        turnOnNowTitle = pickAutoCaptureUxText(
            normalized,
            en = "What turns on now",
            lv = "Kas tagad ieslēdzas",
            es = "Lo que se activara ahora",
            fr = "Ce qui s'active maintenant",
            de = "Das wird jetzt eingeschaltet",
            pt = "O que sera ativado agora",
            ar = "ما الذي سيتم تشغيله الآن",
            hi = "अभी क्या चालू होगा",
            ja = "今オンになるもの",
            sw = "Kinachowashwa sasa",
            zh = "现在会开启什么"
        ),
        turnOnNowItems = listOf(
            pickAutoCaptureUxText(
                normalized,
                en = "Automatic mode",
                lv = "Automātiskais režīms",
                es = "Modo automatico",
                fr = "Mode automatique",
                de = "Automatischer Modus",
                pt = "Modo automatico",
                ar = "الوضع التلقائي",
                hi = "ऑटोमैटिक मोड",
                ja = "自動モード",
                sw = "Hali ya kiotomatiki",
                zh = "自动模式"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Statement import",
                lv = "Izrakstu importēšana",
                es = "Importacion de extractos",
                fr = "Import de releves",
                de = "Auszugsimport",
                pt = "Importacao de extratos",
                ar = "استيراد الكشوف",
                hi = "स्टेटमेंट इम्पोर्ट",
                ja = "明細インポート",
                sw = "Uingizaji wa taarifa",
                zh = "账单导入"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Screenshot and receipt OCR",
                lv = "Ekrānuzņēmums un kvīts OCR",
                es = "OCR de capturas y recibos",
                fr = "OCR de captures et recus",
                de = "Screenshot- und Beleg-OCR",
                pt = "OCR de capturas e recibos",
                ar = "OCR للقطات الشاشة والإيصالات",
                hi = "स्क्रीनशॉट और रसीद OCR",
                ja = "スクリーンショットとレシート OCR",
                sw = "OCR ya picha za skrini na risiti",
                zh = "截图与收据 OCR"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Merchant memory and recurring suggestions",
                lv = "Tirgotāja atmiņa un regulāri ieteikumi",
                es = "Memoria de comercios y sugerencias recurrentes",
                fr = "Memoire des marchands et suggestions recurrentes",
                de = "Handler-Merkhilfe und Wiederholungsvorschlage",
                pt = "Memoria de comerciantes e sugestoes recorrentes",
                ar = "ذاكرة المتاجر واقتراحات التكرار",
                hi = "मर्चेंट मेमोरी और recurring सुझाव",
                ja = "加盟店メモリと定期提案",
                sw = "Kumbukumbu ya wafanyabiashara na mapendekezo yanayojirudia",
                zh = "商户记忆与周期建议"
            ),
            pickAutoCaptureUxText(
                normalized,
                en = "Notification capture is available on this page too. After you continue, Xpenze can ask Android for access and it still stays off unless you allow it.",
                lv = "Paziņojumu uztveršana ir pieejama arī šajā lapā. Pēc tam, kad turpināsiet, Xpenze var lūgt Android piekļuvi, un tas joprojām ir izslēgts, ja vien jūs to neatļaujat.",
                es = "La captura de notificaciones tambien esta en esta pagina. Despues de continuar, Xpenze puede pedir acceso a Android y seguira apagada si no lo permites.",
                fr = "La capture des notifications est aussi disponible sur cette page. Apres avoir continue, Xpenze peut demander l'acces Android et elle reste desactivee sans votre accord.",
                de = "Die Benachrichtigungserfassung ist auch auf dieser Seite verfugbar. Nach dem Fortfahren kann Xpenze Android-Zugriff anfragen und sie bleibt aus, wenn Sie nicht zustimmen.",
                pt = "A captura de notificacoes tambem aparece nesta pagina. Depois de continuar, o Xpenze pode pedir acesso ao Android e ela continua desligada se voce nao permitir.",
                ar = "التقاط الإشعارات متاح أيضًا في هذه الصفحة. بعد المتابعة يمكن لـ Xpenze طلب وصول Android، وسيبقى مغلقًا ما لم تسمح بذلك.",
                hi = "Notification capture इसी पेज पर भी मौजूद है। आगे बढ़ने के बाद Xpenze Android access मांग सकता है, और आपकी अनुमति के बिना यह फिर भी चालू नहीं होगा।",
                ja = "通知取り込みはこのページからも有効にできます。続行後に Xpenze が Android のアクセスを求めますが、許可しない限りオンにはなりません。",
                sw = "Ukamataji wa arifa pia upo kwenye ukurasa huu. Baada ya kuendelea, Xpenze inaweza kuomba ufikiaji wa Android na utaendelea kuzimwa hadi uruhusu.",
                zh = "通知捕捉在此页面也能开启。继续后，Xpenze 可以请求 Android 权限，但在你允许之前它仍会保持关闭。"
            )
        ),
        primaryCta = when {
            !hasPremiumAccess -> localizedAutoCaptureUpgradeButtonText(normalized)
            autoCaptureEnabled -> localizedAutoCaptureDraftInboxTitle(normalized)
            else -> pickAutoCaptureUxText(
                normalized,
                en = "I understand, turn it on",
                lv = "Es saprotu, ieslēdziet to",
                es = "Lo entiendo, activalo",
                fr = "J'ai compris, activez-le",
                de = "Ich verstehe, jetzt einschalten",
                pt = "Entendi, ativar agora",
                ar = "فهمت، قم بتشغيله",
                hi = "मैं समझ गया, इसे चालू करें",
                ja = "理解しました。オンにする",
                sw = "Nimeelewa, washa sasa",
                zh = "我明白了，立即开启"
            )
        },
        secondaryCta = pickAutoCaptureUxText(
            normalized,
            en = "Not now",
            lv = "Ne tagad",
            es = "Ahora no",
            fr = "Pas maintenant",
            de = "Jetzt nicht",
            pt = "Agora nao",
            ar = "ليس الآن",
            hi = "अभी नहीं",
            ja = "今はしない",
            sw = "Sio sasa",
            zh = "暂不"
        )
    )
}

internal fun localizedAutoCaptureNotificationSetupPromptMessage(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "When you tap the button below, Xpenze will immediately offer notification capture here too. Android access is still required before supported money alerts can become drafts.",
        lv = "Pieskaroties zemāk esošajai pogai, Xpenze nekavējoties piedāvās paziņojumu tveršanu arī šeit. Lai atbalstītie naudas brīdinājumi varētu kļūt par melnrakstiem, joprojām ir nepieciešama Android piekļuve.",
        es = "Cuando toques el boton de abajo, Xpenze te ofrecera aqui mismo la captura de notificaciones. Aun hace falta acceso de Android antes de que las alertas compatibles se conviertan en borradores.",
        fr = "Quand vous appuierez sur le bouton ci-dessous, Xpenze proposera aussi ici la capture des notifications. L'acces Android reste necessaire avant que les alertes compatibles deviennent des brouillons.",
        de = "Wenn Sie unten auf die Schaltflache tippen, bietet Xpenze die Benachrichtigungserfassung auch hier sofort an. Android-Zugriff ist weiterhin erforderlich, bevor passende Geldalarme zu Entwurfen werden.",
        pt = "Quando voce tocar no botao abaixo, o Xpenze oferecera aqui mesmo a captura de notificacoes. O acesso do Android ainda e necessario antes que alertas compativeis virem rascunhos.",
        ar = "عندما تضغط الزر أدناه، سيعرض Xpenze هنا أيضًا التقاط الإشعارات فورًا. ما زلت بحاجة إلى وصول Android قبل أن تتحول التنبيهات المتوافقة إلى مسودات.",
        hi = "जब आप नीचे वाला बटन दबाएंगे, Xpenze यहीं notification capture भी तुरंत ऑफर करेगा। समर्थित money alerts को draft बनाने से पहले Android access अभी भी जरूरी है।",
        ja = "下のボタンを押すと、Xpenze はここで通知取り込みもすぐに案内します。対応する入出金通知を下書きにするには、Android のアクセス許可がまだ必要です。",
        sw = "Ukipiga kitufe kilicho hapa chini, Xpenze itatoa ukamataji wa arifa hapa hapa mara moja. Ufikiaji wa Android bado unahitajika kabla arifa za pesa zinazotumika hazijawa rasimu.",
        zh = "当你点击下方按钮时，Xpenze 也会立刻在这里提供通知捕捉。受支持的资金提醒要变成草稿，仍然需要先获得 Android 权限。"
    )

internal fun localizedAutoCapturePremiumBundleLabel(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Automatic capture, Mochi, and recurring reports",
        lv = "Automātiska uztveršana, Mochi un periodiski pārskati",
        es = "Captura automatica, Mochi y reportes recurrentes",
        fr = "Capture automatique, Mochi et rapports recurrents",
        de = "Automatische Erfassung, Mochi und wiederkehrende Berichte",
        pt = "Captura automatica, Mochi e relatorios recorrentes",
        ar = "الالتقاط التلقائي وMochi وتقارير التكرار",
        hi = "ऑटोमैटिक कैप्चर, Mochi और recurring reports",
        ja = "自動取り込み、Mochi、定期レポート",
        sw = "Ukamataji wa kiotomatiki, Mochi, na ripoti za kurudiarudia",
        zh = "自动捕捉、Mochi 与周期报告"
    )

internal fun localizedAutoCapturePremiumFeatureLabel(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Automatic capture",
        lv = "Automātiska uztveršana",
        es = "Captura automatica",
        fr = "Capture automatique",
        de = "Automatische Erfassung",
        pt = "Captura automatica",
        ar = "الالتقاط التلقائي",
        hi = "ऑटोमैटिक कैप्चर",
        ja = "自動取り込み",
        sw = "Ukamataji wa kiotomatiki",
        zh = "自动捕捉"
    )

internal fun localizedAutoCapturePremiumBenefit(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Automatic capture: statements, receipts, and alert drafts",
        lv = "Automātiska uztveršana: izraksti, kvītis un brīdinājumu melnraksti",
        es = "Captura automatica: extractos, recibos y borradores desde alertas",
        fr = "Capture automatique : releves, recus et brouillons depuis les alertes",
        de = "Automatische Erfassung: Auszuge, Belege und Alarm-Entwurfe",
        pt = "Captura automatica: extratos, recibos e rascunhos por alertas",
        ar = "الالتقاط التلقائي: كشوف وإيصالات ومسودات من التنبيهات",
        hi = "ऑटोमैटिक कैप्चर: स्टेटमेंट, रसीद और अलर्ट ड्राफ्ट",
        ja = "自動取り込み: 明細、レシート、通知下書き",
        sw = "Ukamataji wa kiotomatiki: taarifa, risiti, na rasimu za arifa",
        zh = "自动捕捉：账单、收据与提醒草稿"
    )

internal fun localizedAutoCapturePremiumComparisonBody(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Premium should save you time, reduce manual typing, and help you catch more of your real money activity.",
        lv = "Premium ietaupa jūsu laiku, samazina manuālo rakstīšanu un palīdzēs jums iegūt vairāk no reālās naudas darbībām.",
        es = "Premium debe ahorrarte tiempo, reducir el trabajo manual y ayudarte a captar mejor tu actividad real de dinero.",
        fr = "Premium doit vous faire gagner du temps, reduire la saisie manuelle et mieux capturer vos vraies activites d'argent.",
        de = "Premium soll Zeit sparen, manuelle Eingaben reduzieren und mehr Ihrer echten Geldbewegungen erfassen.",
        pt = "O Premium deve economizar seu tempo, reduzir digitacao manual e ajudar voce a captar mais da sua atividade real de dinheiro.",
        ar = "يجب أن يوفر لك Premium الوقت، ويقلل الكتابة اليدوية، ويساعدك على التقاط نشاطك المالي الحقيقي بشكل أفضل.",
        hi = "Premium का मतलब समय बचाना, मैनुअल टाइपिंग कम करना और आपकी असली मनी एक्टिविटी को बेहतर पकड़ना होना चाहिए।",
        ja = "Premium は時間を節約し、手入力を減らし、実際のお金の動きをより多く捉えられるべきです。",
        sw = "Premium inapaswa kukuokoa muda, kupunguza kuandika kwa mkono, na kusaidia kushika zaidi ya shughuli zako halisi za pesa.",
        zh = "Premium 应该帮你节省时间、减少手动输入，并更完整地捕捉真实资金活动。"
    )

internal fun localizedAutoCapturePremiumFreeValue(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Manual entry",
        lv = "Manuāla ievade",
        es = "Entrada manual",
        fr = "Saisie manuelle",
        de = "Manuelle Eingabe",
        pt = "Entrada manual",
        ar = "إدخال يدوي",
        hi = "मैनुअल एंट्री",
        ja = "手入力",
        sw = "Uingizaji wa mkono",
        zh = "手动录入"
    )

internal fun localizedAutoCapturePremiumValue(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Statements, OCR, alerts",
        lv = "Paziņojumi, OCR, brīdinājumi",
        es = "Extractos, OCR, alertas",
        fr = "Releves, OCR, alertes",
        de = "Auszuge, OCR, Alarme",
        pt = "Extratos, OCR, alertas",
        ar = "كشوف وOCR وتنبيهات",
        hi = "स्टेटमेंट, OCR, अलर्ट",
        ja = "明細、OCR、通知",
        sw = "Taarifa, OCR, arifa",
        zh = "账单、OCR、提醒"
    )

internal fun localizedAutoCaptureNotificationAccessMessage(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Android will open notification access settings. If you allow Xpenze here, it can read matching money alerts and turn them into drafts for review. It still cannot move money or add real transactions by itself.",
        lv = "Android atvērs paziņojumu piekļuves iestatījumus. Ja šeit atļausiet Xpenze, tas var nolasīt atbilstošos naudas brīdinājumus un pārvērst tos melnrakstos pārskatīšanai. Tas joprojām nevar pārvietot naudu vai pievienot reālus darījumus.",
        es = "Android abrira los ajustes de acceso a notificaciones. Si permites Xpenze aqui, podra leer alertas de dinero compatibles y convertirlas en borradores para revisar. Aun asi no puede mover dinero ni agregar transacciones reales por si solo.",
        fr = "Android ouvrira les parametres d'acces aux notifications. Si vous autorisez Xpenze ici, il pourra lire les alertes d'argent correspondantes et les transformer en brouillons a verifier. Il ne peut toujours ni deplacer de l'argent ni ajouter de vraies transactions tout seul.",
        de = "Android offnet die Einstellungen fur den Benachrichtigungszugriff. Wenn Sie Xpenze hier erlauben, kann es passende Geldalarme lesen und als Entwurfe zur Prufung speichern. Geld bewegen oder echte Transaktionen selbststandig anlegen kann es trotzdem nicht.",
        pt = "O Android abrira as configuracoes de acesso a notificacoes. Se voce permitir o Xpenze aqui, ele podera ler alertas de dinheiro compativeis e transforma-los em rascunhos para revisao. Mesmo assim, ele nao pode movimentar dinheiro nem adicionar transacoes reais sozinho.",
        ar = "سيفتح Android إعدادات الوصول إلى الإشعارات. إذا سمحت لـ Xpenze هنا، فسيستطيع قراءة التنبيهات المالية المطابقة وتحويلها إلى مسودات للمراجعة. ومع ذلك، لا يزال غير قادر على تحريك الأموال أو إضافة معاملات حقيقية من تلقاء نفسه.",
        hi = "Android notification access settings खोलेगा। अगर आप यहां Xpenze को अनुमति देते हैं, तो यह मेल खाने वाले मनी अलर्ट पढ़कर उन्हें review drafts में बदल सकता है। फिर भी यह अपने आप पैसा नहीं भेज सकता और न ही असली ट्रांजैक्शन जोड़ सकता है।",
        ja = "Android が通知アクセス設定を開きます。ここで Xpenze を許可すると、一致した入出金通知を読み取り、確認用の下書きにできます。それでも、お金を動かしたり、本物の取引を勝手に追加したりはできません。",
        sw = "Android itafungua mipangilio ya ufikiaji wa arifa. Ukiruhusu Xpenze hapa, inaweza kusoma arifa za pesa zinazolingana na kuzigeuza kuwa rasimu za kukaguliwa. Bado haiwezi kuhamisha pesa au kuongeza miamala halisi yenyewe.",
        zh = "Android 将打开通知访问设置。如果你在这里允许 Xpenze，它就能读取匹配的资金提醒，并把它们变成待审核草稿。即便如此，它仍然不能自行转移资金或添加真实交易。"
    )

private fun localizedAutoCaptureSettingsTitle(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Automatic capture",
        lv = "Automātiska uztveršana",
        es = "Captura automatica",
        fr = "Capture automatique",
        de = "Automatische Erfassung",
        pt = "Captura automatica",
        ar = "الالتقاط التلقائي",
        hi = "ऑटोमैटिक कैप्चर",
        ja = "自動取り込み",
        sw = "Ukamataji wa kiotomatiki",
        zh = "自动捕捉"
    )

private fun localizedAutoCaptureDraftInboxTitle(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Open Draft Inbox",
        lv = "Atveriet Melnrakstu iesūtni",
        es = "Abrir Bandeja de borradores",
        fr = "Ouvrir la Boite de brouillons",
        de = "Entwurfs-Posteingang offnen",
        pt = "Abrir Caixa de rascunhos",
        ar = "افتح صندوق المسودات",
        hi = "Draft Inbox खोलें",
        ja = "下書き受信箱を開く",
        sw = "Fungua Kikasha cha Rasimu",
        zh = "打开草稿收件箱"
    )

private fun localizedAutoCaptureUpgradeButtonText(languageTag: String): String =
    pickAutoCaptureUxText(
        normalizeAutoCaptureUxTag(languageTag),
        en = "Unlock Premium",
        lv = "Atbloķējiet Premium",
        es = "Desbloquear Premium",
        fr = "Debloquer Premium",
        de = "Premium freischalten",
        pt = "Desbloquear Premium",
        ar = "افتح Premium",
        hi = "Premium अनलॉक करें",
        ja = "Premium を開放",
        sw = "Fungua Premium",
        zh = "解锁 Premium"
    )

private fun normalizeAutoCaptureUxTag(languageTag: String?): String {
    return AppLanguages.normalizeTag(languageTag ?: Locale.getDefault().toLanguageTag())
}

private fun pickAutoCaptureUxText(
    normalizedTag: String,
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
    return when (normalizedTag) {
        "lv" -> lv
        "es" -> es
        "fr" -> fr
        "de" -> de
        "pt" -> pt
        "ar" -> ar
        "hi" -> hi
        "ja" -> ja
        "sw" -> sw
        "zh" -> zh
        else -> en
    }
}
