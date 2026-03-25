package com.chelumore.xpenze.core.localization

import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureSource
import java.util.Locale

internal fun localizedAutoCaptureSourceLabel(
    source: AutoCaptureSource,
    languageTag: String? = null
): String = when (source) {
    AutoCaptureSource.STATEMENT_IMPORT -> pickAutoCaptureText(
        languageTag,
        en = "Statement",
        es = "Extracto",
        fr = "Releve",
        de = "Auszug",
        pt = "Extrato",
        ar = "كشف",
        hi = "स्टेटमेंट",
        ja = "明細",
        sw = "Taarifa",
        zh = "账单"
    )
    AutoCaptureSource.OCR -> "OCR"
    AutoCaptureSource.NOTIFICATION -> pickAutoCaptureText(
        languageTag,
        en = "Notification",
        es = "Notificacion",
        fr = "Notification",
        de = "Benachrichtigung",
        pt = "Notificacao",
        ar = "إشعار",
        hi = "नोटिफिकेशन",
        ja = "通知",
        sw = "Arifa",
        zh = "通知"
    )
}

internal fun localizedAutoCaptureStatusLabel(
    status: AutoCaptureDraftStatus,
    languageTag: String? = null
): String = when (status) {
    AutoCaptureDraftStatus.PENDING -> pickAutoCaptureText(
        languageTag,
        en = "Ready",
        es = "Listo",
        fr = "Pret",
        de = "Bereit",
        pt = "Pronto",
        ar = "جاهز",
        hi = "तैयार",
        ja = "準備完了",
        sw = "Tayari",
        zh = "就绪"
    )
    AutoCaptureDraftStatus.NEEDS_REVIEW -> pickAutoCaptureText(
        languageTag,
        en = "Needs review",
        es = "Necesita revision",
        fr = "A verifier",
        de = "Prufung erforderlich",
        pt = "Precisa de revisao",
        ar = "يحتاج مراجعة",
        hi = "समीक्षा चाहिए",
        ja = "確認が必要",
        sw = "Inahitaji ukaguzi",
        zh = "需要审核"
    )
    AutoCaptureDraftStatus.RESOLVED -> pickAutoCaptureText(
        languageTag,
        en = "Resolved",
        es = "Resuelto",
        fr = "Traite",
        de = "Erledigt",
        pt = "Resolvido",
        ar = "تمت المعالجة",
        hi = "सुलझा हुआ",
        ja = "処理済み",
        sw = "Imetatuliwa",
        zh = "已处理"
    )
}

internal fun localizedAutoCapturePremiumRequiredMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Auto-capture is part of Premium.",
        es = "La captura automatica forma parte de Premium.",
        fr = "La capture automatique fait partie de Premium.",
        de = "Die automatische Erfassung ist Teil von Premium.",
        pt = "A captura automatica faz parte do Premium.",
        ar = "الالتقاط التلقائي ضمن Premium.",
        hi = "ऑटो-कैप्चर Premium का हिस्सा है।",
        ja = "自動取り込みは Premium 機能です。",
        sw = "Ukamataji wa kiotomatiki ni sehemu ya Premium.",
        zh = "自动捕捉属于 Premium 功能。"
    )

internal fun localizedAutoCaptureEnableStatementFirstMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Enable statement import in Settings first.",
        es = "Primero activa la importacion de extractos en Ajustes.",
        fr = "Activez d'abord l'import de releves dans les Reglages.",
        de = "Aktivieren Sie zuerst den Auszugsimport in den Einstellungen.",
        pt = "Primeiro ative a importacao de extratos nas Configuracoes.",
        ar = "فعّل استيراد الكشوف من الإعدادات أولا.",
        hi = "पहले सेटिंग्स में स्टेटमेंट इम्पोर्ट चालू करें।",
        ja = "先に設定で明細インポートを有効にしてください。",
        sw = "Washa kwanza uingizaji wa taarifa kwenye Mipangilio.",
        zh = "请先在设置中启用账单导入。"
    )

internal fun localizedAutoCaptureStatementImportSummaryMessage(
    draftsCreated: Int,
    rowsSkipped: Int,
    languageTag: String? = null
): String = pickAutoCaptureText(
    languageTag,
    en = "Added $draftsCreated draft(s). Skipped $rowsSkipped row(s).",
    es = "Se agregaron $draftsCreated borrador(es). Se omitieron $rowsSkipped fila(s).",
    fr = "$draftsCreated brouillon(s) ajoute(s). $rowsSkipped ligne(s) ignoree(s).",
    de = "$draftsCreated Entwurf(e) hinzugefugt. $rowsSkipped Zeile(n) ubersprungen.",
    pt = "$draftsCreated rascunho(s) adicionado(s). $rowsSkipped linha(s) ignorada(s).",
    ar = "تمت إضافة $draftsCreated مسودة/مسودات. وتم تخطي $rowsSkipped صف/صفوف.",
    hi = "$draftsCreated ड्राफ्ट जोड़े गए। $rowsSkipped पंक्तियां छोड़ी गईं।",
    ja = "$draftsCreated 件の下書きを追加し、$rowsSkipped 行をスキップしました。",
    sw = "Rasimu $draftsCreated zimeongezwa. Mistari $rowsSkipped imerukwa.",
    zh = "已添加 $draftsCreated 个草稿，跳过 $rowsSkipped 行。"
)

internal fun localizedAutoCaptureStatementImportFailedMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Statement import failed.",
        es = "La importacion del extracto fallo.",
        fr = "L'import du releve a echoue.",
        de = "Der Auszugsimport ist fehlgeschlagen.",
        pt = "A importacao do extrato falhou.",
        ar = "فشل استيراد الكشف.",
        hi = "स्टेटमेंट इम्पोर्ट विफल रहा।",
        ja = "明細のインポートに失敗しました。",
        sw = "Uingizaji wa taarifa umeshindikana.",
        zh = "账单导入失败。"
    )

internal fun localizedAutoCaptureStatementFormatError(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "We could not recognize this statement format. Use a CSV with date, amount, and description columns.",
        es = "No pudimos reconocer este formato de extracto. Usa un CSV con columnas de fecha, importe y descripcion.",
        fr = "Impossible de reconnaitre ce format de releve. Utilisez un CSV avec les colonnes date, montant et description.",
        de = "Dieses Auszugsformat konnte nicht erkannt werden. Verwenden Sie eine CSV mit Datum, Betrag und Beschreibung.",
        pt = "Nao foi possivel reconhecer este formato de extrato. Use um CSV com colunas de data, valor e descricao.",
        ar = "تعذر التعرف على تنسيق هذا الكشف. استخدم ملف CSV يحتوي على أعمدة التاريخ والمبلغ والوصف.",
        hi = "इस स्टेटमेंट फ़ॉर्मेट को पहचाना नहीं जा सका। तारीख, राशि और विवरण वाले CSV का उपयोग करें।",
        ja = "この明細形式は認識できませんでした。日付、金額、説明の列を含む CSV を使用してください。",
        sw = "Hatukuweza kutambua muundo huu wa taarifa. Tumia CSV yenye safu za tarehe, kiasi, na maelezo.",
        zh = "无法识别此账单格式。请使用包含日期、金额和描述列的 CSV。"
    )

internal fun localizedAutoCaptureEnableOcrFirstMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Enable screenshot / receipt OCR in Settings first.",
        es = "Primero activa el OCR de capturas y recibos en Ajustes.",
        fr = "Activez d'abord l'OCR des captures et recus dans les Reglages.",
        de = "Aktivieren Sie zuerst Screenshot-/Beleg-OCR in den Einstellungen.",
        pt = "Primeiro ative o OCR de capturas e recibos nas Configuracoes.",
        ar = "فعّل OCR للقطات الشاشة والإيصالات من الإعدادات أولا.",
        hi = "पहले सेटिंग्स में स्क्रीनशॉट / रसीद OCR चालू करें।",
        ja = "先に設定でスクリーンショット / レシート OCR を有効にしてください。",
        sw = "Washa kwanza OCR ya picha na risiti kwenye Mipangilio.",
        zh = "请先在设置中启用截图 / 收据 OCR。"
    )

internal fun localizedAutoCaptureOcrAddedMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "OCR draft added to your inbox.",
        es = "El borrador OCR se agrego a tu bandeja.",
        fr = "Le brouillon OCR a ete ajoute a votre boite.",
        de = "Der OCR-Entwurf wurde Ihrem Posteingang hinzugefugt.",
        pt = "O rascunho OCR foi adicionado a sua caixa.",
        ar = "تمت إضافة مسودة OCR إلى صندوقك.",
        hi = "OCR ड्राफ्ट आपके इनबॉक्स में जोड़ दिया गया है।",
        ja = "OCR 下書きが受信箱に追加されました。",
        sw = "Rasimu ya OCR imeongezwa kwenye kikasha chako.",
        zh = "OCR 草稿已添加到你的收件箱。"
    )

internal fun localizedAutoCaptureOcrFailedParseMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "We could not extract a reliable draft from that image.",
        es = "No pudimos extraer un borrador confiable de esa imagen.",
        fr = "Nous n'avons pas pu extraire un brouillon fiable de cette image.",
        de = "Aus diesem Bild konnte kein zuverlassiger Entwurf erstellt werden.",
        pt = "Nao foi possivel extrair um rascunho confiavel dessa imagem.",
        ar = "تعذر استخراج مسودة موثوقة من هذه الصورة.",
        hi = "हम उस छवि से भरोसेमंद ड्राफ्ट नहीं निकाल सके।",
        ja = "その画像から信頼できる下書きを抽出できませんでした。",
        sw = "Hatukuweza kutoa rasimu ya kuaminika kutoka kwenye picha hiyo.",
        zh = "无法从该图片中提取可靠的草稿。"
    )

internal fun localizedAutoCaptureOcrFailedMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "OCR capture failed.",
        es = "La captura OCR fallo.",
        fr = "La capture OCR a echoue.",
        de = "Die OCR-Erfassung ist fehlgeschlagen.",
        pt = "A captura OCR falhou.",
        ar = "فشل التقاط OCR.",
        hi = "OCR कैप्चर विफल रहा।",
        ja = "OCR 取り込みに失敗しました。",
        sw = "Ukamataji wa OCR umeshindikana.",
        zh = "OCR 捕捉失败。"
    )

internal fun localizedAutoCaptureDraftMissingMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Draft no longer exists.",
        es = "El borrador ya no existe.",
        fr = "Le brouillon n'existe plus.",
        de = "Der Entwurf existiert nicht mehr.",
        pt = "O rascunho nao existe mais.",
        ar = "المسودة لم تعد موجودة.",
        hi = "ड्राफ्ट अब मौजूद नहीं है।",
        ja = "この下書きはもう存在しません。",
        sw = "Rasimu hiyo haipo tena.",
        zh = "该草稿已不存在。"
    )

internal fun localizedAutoCaptureConfirmAmountFirstMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Open review to confirm the amount first.",
        es = "Abre la revision para confirmar primero el importe.",
        fr = "Ouvrez la verification pour confirmer d'abord le montant.",
        de = "Offnen Sie zuerst die Prufung, um den Betrag zu bestatigen.",
        pt = "Abra a revisao para confirmar primeiro o valor.",
        ar = "افتح المراجعة لتأكيد المبلغ أولا.",
        hi = "पहले राशि की पुष्टि करने के लिए रिव्यू खोलें।",
        ja = "先にレビューを開いて金額を確認してください。",
        sw = "Fungua ukaguzi kwanza ili kuthibitisha kiasi.",
        zh = "请先打开审核来确认金额。"
    )

internal fun localizedAutoCaptureChooseAccountFirstMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Create or choose an account first.",
        es = "Primero crea o elige una cuenta.",
        fr = "Creez ou choisissez d'abord un compte.",
        de = "Erstellen oder wahlen Sie zuerst ein Konto aus.",
        pt = "Primeiro crie ou escolha uma conta.",
        ar = "أنشئ أو اختر حسابا أولا.",
        hi = "पहले कोई खाता बनाएं या चुनें।",
        ja = "先に口座を作成するか選択してください。",
        sw = "Unda au chagua akaunti kwanza.",
        zh = "请先创建或选择一个账户。"
    )

internal fun localizedAutoCaptureChooseDestinationFirstMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Open review to choose the destination account.",
        es = "Abre la revision para elegir la cuenta de destino.",
        fr = "Ouvrez la verification pour choisir le compte de destination.",
        de = "Offnen Sie die Prufung, um das Zielkonto auszuwahlen.",
        pt = "Abra a revisao para escolher a conta de destino.",
        ar = "افتح المراجعة لاختيار الحساب الوجهة.",
        hi = "गंतव्य खाता चुनने के लिए रिव्यू खोलें।",
        ja = "送金先口座を選ぶにはレビューを開いてください。",
        sw = "Fungua ukaguzi ili kuchagua akaunti lengwa.",
        zh = "请打开审核并选择目标账户。"
    )

internal fun localizedAutoCaptureDefaultTitle(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Captured draft",
        es = "Borrador capturado",
        fr = "Brouillon capture",
        de = "Erfasster Entwurf",
        pt = "Rascunho capturado",
        ar = "مسودة ملتقطة",
        hi = "कैप्चर किया गया ड्राफ्ट",
        ja = "取り込み下書き",
        sw = "Rasimu iliyokamatwa",
        zh = "已捕捉草稿"
    )

internal fun localizedAutoCaptureSavedToTransactionsMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Draft saved to transactions.",
        es = "El borrador se guardo en transacciones.",
        fr = "Le brouillon a ete enregistre dans les transactions.",
        de = "Der Entwurf wurde bei den Transaktionen gespeichert.",
        pt = "O rascunho foi salvo em transacoes.",
        ar = "تم حفظ المسودة ضمن المعاملات.",
        hi = "ड्राफ्ट ट्रांजैक्शन्स में सेव हो गया।",
        ja = "下書きが取引に保存されました。",
        sw = "Rasimu imehifadhiwa kwenye miamala.",
        zh = "草稿已保存到交易中。"
    )

internal fun localizedAutoCaptureSaveFailedMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "We could not save that draft.",
        es = "No pudimos guardar ese borrador.",
        fr = "Nous n'avons pas pu enregistrer ce brouillon.",
        de = "Dieser Entwurf konnte nicht gespeichert werden.",
        pt = "Nao foi possivel salvar esse rascunho.",
        ar = "تعذر حفظ هذه المسودة.",
        hi = "हम उस ड्राफ्ट को सेव नहीं कर सके।",
        ja = "その下書きを保存できませんでした。",
        sw = "Hatukuweza kuhifadhi rasimu hiyo.",
        zh = "无法保存该草稿。"
    )

internal fun localizedAutoCaptureMarkedPassThroughMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Draft marked as pass-through.",
        es = "El borrador se marco como paso temporal.",
        fr = "Le brouillon a ete marque comme pass-through.",
        de = "Der Entwurf wurde als Durchlaufposten markiert.",
        pt = "O rascunho foi marcado como pass-through.",
        ar = "تم وضع المسودة كمرور مؤقت.",
        hi = "ड्राफ्ट को पास-थ्रू के रूप में चिह्नित किया गया।",
        ja = "下書きは通過扱いとしてマークされました。",
        sw = "Rasimu imewekwa kama pass-through.",
        zh = "草稿已标记为代收代付。"
    )

internal fun localizedAutoCaptureExcludedMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Draft excluded from your inbox.",
        es = "El borrador se excluyo de tu bandeja.",
        fr = "Le brouillon a ete exclu de votre boite.",
        de = "Der Entwurf wurde aus Ihrem Posteingang ausgeschlossen.",
        pt = "O rascunho foi excluido da sua caixa.",
        ar = "تم استبعاد المسودة من صندوقك.",
        hi = "ड्राफ्ट को आपके इनबॉक्स से बाहर कर दिया गया।",
        ja = "下書きは受信箱から除外されました。",
        sw = "Rasimu imeondolewa kwenye kikasha chako.",
        zh = "草稿已从收件箱中排除。"
    )

internal fun localizedAutoCaptureResolvedMessage(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Draft resolved.",
        es = "Borrador resuelto.",
        fr = "Brouillon traite.",
        de = "Entwurf erledigt.",
        pt = "Rascunho resolvido.",
        ar = "تمت معالجة المسودة.",
        hi = "ड्राफ्ट का समाधान हो गया।",
        ja = "下書きを処理しました。",
        sw = "Rasimu imetatuliwa.",
        zh = "草稿已处理。"
    )

internal fun localizedAutoCaptureNotificationAmbiguousReason(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "This looks like a real money movement, but its meaning still needs your confirmation.",
        es = "Esto parece un movimiento de dinero real, pero su significado aun necesita tu confirmacion.",
        fr = "Cela ressemble a un vrai mouvement d'argent, mais sa signification doit encore etre confirmee.",
        de = "Dies sieht nach einer echten Geldbewegung aus, aber die Bedeutung muss noch bestatigt werden.",
        pt = "Isto parece um movimento real de dinheiro, mas o significado ainda precisa da sua confirmacao.",
        ar = "يبدو هذا كحركة أموال حقيقية، لكن معناها ما زال يحتاج إلى تأكيدك.",
        hi = "यह असली धन-गतिविधि लगती है, लेकिन इसका अर्थ अभी आपकी पुष्टि चाहता है।",
        ja = "実際のお金の動きに見えますが、意味の確認がまだ必要です。",
        sw = "Hii inaonekana kama uhamisho halisi wa pesa, lakini maana yake bado inahitaji uthibitisho wako.",
        zh = "这看起来像真实的资金变动，但其含义仍需要你确认。"
    )

internal fun localizedAutoCaptureOcrAmbiguousReason(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "OCR found an amount, but the transaction meaning is still ambiguous.",
        es = "El OCR encontro un importe, pero el significado de la transaccion sigue siendo ambiguo.",
        fr = "L'OCR a trouve un montant, mais le sens de la transaction reste ambigu.",
        de = "OCR hat einen Betrag gefunden, aber die Bedeutung der Transaktion bleibt unklar.",
        pt = "O OCR encontrou um valor, mas o significado da transacao ainda e ambiguo.",
        ar = "عثر OCR على مبلغ، لكن معنى المعاملة ما زال غير واضح.",
        hi = "OCR को राशि मिली, लेकिन लेन-देन का अर्थ अभी भी स्पष्ट नहीं है।",
        ja = "OCR で金額は見つかりましたが、取引の意味はまだ曖昧です。",
        sw = "OCR imepata kiasi, lakini maana ya muamala bado haijaeleweka.",
        zh = "OCR 识别到了金额，但该交易的含义仍然不明确。"
    )

internal fun localizedAutoCaptureStatementAmbiguousReason(languageTag: String? = null): String =
    pickAutoCaptureText(
        languageTag,
        en = "Possible transfer or ambiguous statement row.",
        es = "Posible transferencia o fila ambigua del extracto.",
        fr = "Virement possible ou ligne de releve ambigue.",
        de = "Mogliche Uberweisung oder mehrdeutige Auszugszeile.",
        pt = "Possivel transferencia ou linha ambigua do extrato.",
        ar = "تحويل محتمل أو سطر كشف غامض.",
        hi = "संभव ट्रांसफर या स्टेटमेंट की अस्पष्ट पंक्ति।",
        ja = "振替の可能性があるか、明細行が曖昧です。",
        sw = "Huenda ni uhamisho au mstari wa taarifa usio wazi.",
        zh = "可能是转账，或账单行含义不明确。"
    )

private fun pickAutoCaptureText(
    languageTag: String?,
    en: String,
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
    return when (AppLanguages.normalizeTag(languageTag ?: Locale.getDefault().toLanguageTag())) {
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
