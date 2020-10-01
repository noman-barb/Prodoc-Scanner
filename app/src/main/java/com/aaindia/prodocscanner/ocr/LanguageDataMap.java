package com.aaindia.prodocscanner.ocr;

import android.util.Log;

import java.util.ArrayList;

public class LanguageDataMap {


    public ArrayList<KeyValuePair> getLangOptions() {


        ArrayList<KeyValuePair> list = new ArrayList<>();


        list.add(new KeyValuePair("English", "eng"));

        list.add(new KeyValuePair("Afrikaans", "afr"));
        list.add(new KeyValuePair("Amharic", "amh"));
        list.add(new KeyValuePair("Arabic", "ara"));
        list.add(new KeyValuePair("Assamese", "asm"));
        list.add(new KeyValuePair("Azerbaijani", "aze"));
        list.add(new KeyValuePair("Azerbaijani - Cyrillic", "aze_cyrl"));
        list.add(new KeyValuePair("Belarusian", "bel"));
        list.add(new KeyValuePair("Bengali", "ben"));
        list.add(new KeyValuePair("Tibetan", "bod"));
        list.add(new KeyValuePair("Bosnian", "bos"));
        list.add(new KeyValuePair("Bulgarian", "bul"));
        list.add(new KeyValuePair("Catalan; Valencian", "cat"));
        list.add(new KeyValuePair("Cebuano", "ceb"));
        list.add(new KeyValuePair("Czech", "ces"));
        list.add(new KeyValuePair("Chinese - Simplified", "chi_sim"));
        list.add(new KeyValuePair("Chinese - Traditional", "chi_tra"));
        list.add(new KeyValuePair("Cherokee", "chr"));
        list.add(new KeyValuePair("Welsh", "cym"));
        list.add(new KeyValuePair("Danish", "dan"));
        list.add(new KeyValuePair("German", "deu"));
        list.add(new KeyValuePair("Dzongkha", "dzo"));
        list.add(new KeyValuePair("Greek, Modern (1453-)", "ell"));

        list.add(new KeyValuePair("English, Middle (1100-1500)", "enm"));
        list.add(new KeyValuePair("Esperanto", "epo"));
        list.add(new KeyValuePair("Estonian", "est"));
        list.add(new KeyValuePair("Basque", "eus"));
        list.add(new KeyValuePair("Persian", "fas"));
        list.add(new KeyValuePair("Finnish", "fin"));
        list.add(new KeyValuePair("French", "fra"));
        list.add(new KeyValuePair("German Fraktur", "frk"));
        list.add(new KeyValuePair("French, Middle (ca. 1400-1600)", "frm"));
        list.add(new KeyValuePair("Irish", "gle"));
        list.add(new KeyValuePair("Galician", "glg"));
        list.add(new KeyValuePair("Greek, Ancient (-1453)", "grc"));
        list.add(new KeyValuePair("Gujarati", "guj"));
        list.add(new KeyValuePair("Haitian; Haitian Creole", "hat"));
        list.add(new KeyValuePair("Hebrew", "heb"));
        list.add(new KeyValuePair("Hindi", "hin"));
        list.add(new KeyValuePair("Croatian", "hrv"));
        list.add(new KeyValuePair("Hungarian", "hun"));
        list.add(new KeyValuePair("Inuktitut", "iku"));
        list.add(new KeyValuePair("Indonesian", "ind"));
        list.add(new KeyValuePair("Icelandic", "isl"));
        list.add(new KeyValuePair("Italian", "ita"));
        list.add(new KeyValuePair("Italian - Old", "ita_old"));
        list.add(new KeyValuePair("Javanese", "jav"));
        list.add(new KeyValuePair("Japanese", "jpn"));
        list.add(new KeyValuePair("Kannada", "kan"));
        list.add(new KeyValuePair("Georgian", "kat"));
        list.add(new KeyValuePair("Georgian - Old", "kat_old"));
        list.add(new KeyValuePair("Kazakh", "kaz"));

        list.add(new KeyValuePair("Central Khmer", "khm"));
        list.add(new KeyValuePair("Kirghiz; Kyrgyz", "kir"));
        list.add(new KeyValuePair("Korean", "kor"));
        list.add(new KeyValuePair("Kurdish", "kur"));
        list.add(new KeyValuePair("Lao", "lao"));
        list.add(new KeyValuePair("Latin", "lat"));
        list.add(new KeyValuePair("Latvian", "lav"));
        list.add(new KeyValuePair("Lithuanian", "lit"));
        list.add(new KeyValuePair("Malayalam", "mal"));
        list.add(new KeyValuePair("Marathi", "mar"));
        list.add(new KeyValuePair("Macedonian", "mkd"));
        list.add(new KeyValuePair("Maltese", "mlt"));
        list.add(new KeyValuePair("Malay", "msa"));
        list.add(new KeyValuePair("Burmese", "mya"));
        list.add(new KeyValuePair("Nepali", "nep"));
        list.add(new KeyValuePair("Dutch; Flemish", "nld"));
        list.add(new KeyValuePair("Norwegian", "nor"));
        list.add(new KeyValuePair("Oriya", "ori"));
        list.add(new KeyValuePair("Panjabi; Punjabi", "pan"));
        list.add(new KeyValuePair("Polish", "pol"));
        list.add(new KeyValuePair("Portuguese", "por"));
        list.add(new KeyValuePair("Pushto; Pashto", "pus"));
        list.add(new KeyValuePair("Romanian; Moldavian; Moldovan", "ron"));
        list.add(new KeyValuePair("Russian", "rus"));
        list.add(new KeyValuePair("Sanskrit", "san"));
        list.add(new KeyValuePair("Sinhala; Sinhalese", "sin"));
        list.add(new KeyValuePair("Slovak", "slk"));
        list.add(new KeyValuePair("Slovenian", "slv"));
        list.add(new KeyValuePair("Spanish; Castilian", "spa"));
        list.add(new KeyValuePair("Spanish; Castilian - Old", "spa_old"));
        list.add(new KeyValuePair("Albanian", "sqi"));
        list.add(new KeyValuePair("Serbian", "srp"));
        list.add(new KeyValuePair("Serbian - Latin", "srp_latn"));
        list.add(new KeyValuePair("Swahili", "swa"));
        list.add(new KeyValuePair("Swedish", "swe"));
        list.add(new KeyValuePair("Syriac", "syr"));
        list.add(new KeyValuePair("Tamil", "tam"));
        list.add(new KeyValuePair("Telugu", "tel"));
        list.add(new KeyValuePair("Tajik", "tgk"));
        list.add(new KeyValuePair("Tagalog", "tgl"));
        list.add(new KeyValuePair("Thai", "tha"));
        list.add(new KeyValuePair("Tigrinya", "tir"));
        list.add(new KeyValuePair("Turkish", "tur"));
        list.add(new KeyValuePair("Uighur; Uyghur", "uig"));
        list.add(new KeyValuePair("Ukrainian", "ukr"));
        list.add(new KeyValuePair("Urdu", "urd"));
        list.add(new KeyValuePair("Uzbek", "uzb"));
        list.add(new KeyValuePair("Uzbek - Cyrillic", "uzb_cyrl"));
        list.add(new KeyValuePair("Vietnamese", "vie"));
        list.add(new KeyValuePair("Yiddish", "yid"));

        Log.d("aaaaaaaa", list.size()+"");
        return list;

    }


    public class KeyValuePair {


        public String key;
        public String value;

        public KeyValuePair(String value, String key) {

            this.key = key;
            this.value = value;
        }

    }
}
