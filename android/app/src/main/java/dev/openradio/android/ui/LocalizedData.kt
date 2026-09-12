package dev.openradio.android.ui

/** Localized display labels for station-data vocabulary (languages and categories). */
object LocalizedData {
    private val languageTe: Map<String, String> =
        mapOf(
            "Assamese" to "అస్సామీ",
            "Bengali" to "బెంగాలీ",
            "Bhojpuri" to "భోజ్‌పురి",
            "Braj Bhasha" to "బ్రజ్ భాష",
            "Chhattisgarhi" to "ఛత్తీస్‌గఢీ",
            "Dogri" to "డోగ్రీ",
            "English" to "ఇంగ్లీష్",
            "Garhwali" to "గఢ్‌వాలి",
            "Gujarati" to "గుజరాతీ",
            "Haryanvi" to "హర్యాన్వీ",
            "Hindi" to "హిందీ",
            "International" to "అంతర్జాతీయం",
            "Kannada" to "కన్నడ",
            "Khasi" to "ఖాసీ",
            "Kokborok" to "కోక్‌బరక్",
            "Konkani" to "కొంకణి",
            "Ladakhi" to "లడఖీ",
            "Maithili" to "మైథిలి",
            "Malayalam" to "మలయాళం",
            "Manipuri" to "మణిపురి",
            "Marathi" to "మరాఠీ",
            "Mizo" to "మిజో",
            "Monpa" to "మోన్‌పా",
            "Multilingual" to "బహుభాషా",
            "Nagamese" to "నాగామీస్",
            "Nagpuri" to "నాగ్‌పురి",
            "Nepali" to "నేపాలీ",
            "Nicobarese" to "నికోబారీ",
            "Odia" to "ఒడియా",
            "Pahari" to "పహారీ",
            "Punjabi" to "పంజాబీ",
            "Rajasthani" to "రాజస్థానీ",
            "Sanskrit" to "సంస్కృతం",
            "Tamil" to "తమిళం",
            "Telugu" to "తెలుగు",
            "Tulu" to "తుళు",
            "Urdu" to "ఉర్దూ",
        )

    private val languageHi: Map<String, String> =
        mapOf(
            "Assamese" to "असमिया",
            "Bengali" to "बंगाली",
            "Bhojpuri" to "भोजपुरी",
            "Braj Bhasha" to "ब्रज भाषा",
            "Chhattisgarhi" to "छत्तीसगढ़ी",
            "Dogri" to "डोगरी",
            "English" to "अंग्रेज़ी",
            "Garhwali" to "गढ़वाली",
            "Gujarati" to "गुजराती",
            "Haryanvi" to "हरियाणवी",
            "Hindi" to "हिन्दी",
            "International" to "अंतरराष्ट्रीय",
            "Kannada" to "कन्नड़",
            "Khasi" to "खासी",
            "Kokborok" to "कोकबोरोक",
            "Konkani" to "कोंकणी",
            "Ladakhi" to "लद्दाखी",
            "Maithili" to "मैथिली",
            "Malayalam" to "मलयालम",
            "Manipuri" to "मणिपुरी",
            "Marathi" to "मराठी",
            "Mizo" to "मिज़ो",
            "Monpa" to "मोनपा",
            "Multilingual" to "बहुभाषी",
            "Nagamese" to "नागामी",
            "Nagpuri" to "नागपुरी",
            "Nepali" to "नेपाली",
            "Nicobarese" to "निकोबारी",
            "Odia" to "ओड़िया",
            "Pahari" to "पहाड़ी",
            "Punjabi" to "पंजाबी",
            "Rajasthani" to "राजस्थानी",
            "Sanskrit" to "संस्कृत",
            "Tamil" to "तमिल",
            "Telugu" to "तेलुगु",
            "Tulu" to "तुलु",
            "Urdu" to "उर्दू",
        )

    private val categoryTe: Map<String, String> =
        mapOf(
            "Community" to "కమ్యూనిటీ",
        )

    private val categoryHi: Map<String, String> =
        mapOf(
            "Community" to "समुदाय",
        )

    fun language(
        value: String,
        uiLang: String,
    ): String = localized(value, uiLang, languageTe, languageHi)

    fun category(
        value: String,
        uiLang: String,
    ): String = localized(value, uiLang, categoryTe, categoryHi)

    fun languagesData(
        languageField: String,
        uiLang: String,
    ): String = languageField.split(',').joinToString(", ") { tag -> language(tag.trim(), uiLang) }

    private fun localized(
        value: String,
        uiLang: String,
        te: Map<String, String>,
        hi: Map<String, String>,
    ): String =
        when (uiLang) {
            "te" -> te[value]
            "hi" -> hi[value]
            else -> null
        } ?: value
}
