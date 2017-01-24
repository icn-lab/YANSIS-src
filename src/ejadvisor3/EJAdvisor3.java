/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejadvisor3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import net.java.sen.Token;

/**
 *
 * @author Akinori
 */
public class EJAdvisor3 {

    private WordPropertyFactory analyzer;
    private EJConfig conf;
    private Recommendation recommend;
    private WordProperty[][] currentSent; // 現在分析中の文
    private static String base;
    private ExampleFinder examples;
    private ScoreEstimator scoreEstimator;
    private Token[] toks;
    private String morphPath;
    static final String[] REPLACE
            = {"～", "〜",
                "\\.", "．",
                "%", "％",
                "\\+", "＋",
                "\\*", "＊",
                "-", "−",
                "/", "／",
                "=", "＝"
            };

    public EJAdvisor3(String baseDir) {
        base = baseDir;
        if (base.endsWith("/") == false) {
            base += "/";
        }
        initialize();
    }

    public String getBaseDir() {
        return base;
    }

    public Token[] getTokens() {
        return toks;
    }

    public void loadVocab() {
        try {
            examples = new ExampleFinder(morphPath + "Examples.csv");
            conf.set_grade("vocabS.csv", 6); // 記号
            conf.set_grade("vocabB.csv", 5); // 文法項目
            conf.set_grade("vocab4.csv", 4); // 4級
            conf.set_grade("vocab3.csv", 3); // 3級
            conf.set_grade("vocab2.csv", 2); // 2級
            conf.set_grade("vocab1.csv", 1); // 1級
        } catch (IOException e) {
            System.out.println("EJAdvisor3:IO error on initialization:" + e.toString());
            System.exit(1);
        }
    }

    public void loadSen() {
        try {
            analyzer = new WordPropertyFactory(conf);
        } catch (IOException e) {
            System.out.println("EJAdvisor3:IO error on initialization:" + e.toString());
            System.exit(1);
        }
    }

    public void loadRecommend() {
        try {
            recommend = new Recommendation(morphPath + "GrammaticalRecommendation.csv");
        } catch (IOException e) {
            System.out.println("EJAdvisor3:IO error on initialization:" + e.toString());
            System.exit(1);
        }
    }

    public void loadScoreEstimator() {
        scoreEstimator = new ScoreEstimator(morphPath + "score-foreign-all-recommend.w", 26);
    }

    public void initialize() {
        morphPath = base + "morph/";
        conf = new EJConfig(morphPath, 6);
        conf.sen_conf = base + "sen/conf/sen.xml";
        conf.easyword = morphPath + "easyword.txt";
    }

    /**
     * Initialize myself
     */
    /*
     public void initialize() {    
     morphPath = base+"morph/";
     conf = new EJConfig(morphPath,6);
     conf.sen_conf = base+"sen/conf/sen.xml";
     conf.easyword = morphPath+"easyword.txt";
        
     try {            
     examples = new ExampleFinder(morphPath+"Examples.csv");
     conf.set_grade("vocabS.csv", 6); // 記号
     conf.set_grade("vocabB.csv", 5); // 文法項目
     conf.set_grade("vocab4.csv", 4); // 4級
     conf.set_grade("vocab3.csv", 3); // 3級
     conf.set_grade("vocab2.csv", 2); // 2級
     conf.set_grade("vocab1.csv", 1); // 1級
     //splash.setMessage("Senを起動しています...");
     analyzer = new WordPropertyFactory(conf);   
     //splash.setMessage("アドバイスを読みこんでいます...");
     recommend = new Recommendation(morphPath+"GrammaticalRecommendation.csv");
     //wordRecommender = new WordRecommenderByLSA(conf);
     scoreEstimator = new ScoreEstimator(morphPath+"score-foreign-all-recommend.w",26);
     } catch (IOException e) {
     System.out.println("EJAdvisor3:IO error on initialization:"+e.toString());
     System.exit(1);
     }
     }
     */
    // 文終端かどうかを判別する
    private boolean isSentenceEnd(WordProperty w) {
        if (w.getPOS().equals("記号-句点")) {
            return true;
        }
        if (w.toString().equals("？")) {
            return true;
        }
        if (w.toString().equals("！")) {
            return true;
        }
        return false;
    }

    // WordPropertyの配列を句読点で区切って複数の文に分ける
    private WordProperty[][] splitSentence(WordProperty w[]) {
        ArrayList<Integer> bpos = new ArrayList<Integer>();
        bpos.add(new Integer(0));
        for (int i = 0; i < w.length; i++) {
            if (isSentenceEnd(w[i]) && i < w.length - 1) {
                bpos.add(new Integer(i + 1));
            }
        }
        bpos.add(new Integer(w.length));
        WordProperty[][] res = new WordProperty[bpos.size() - 1][];
        for (int i = 1; i < bpos.size(); i++) {
            int n = bpos.get(i) - bpos.get(i - 1);
            res[i - 1] = new WordProperty[n];
            for (int j = 0; j < n; j++) {
                res[i - 1][j] = w[bpos.get(i - 1) + j];
            }
        }
        return res;
    }

    public WordProperty currentMorph(int s, int i) {
        return currentSent[s][i];
    }

    /**
     * Performs analysis
     *
     * @param t
     */
    public WordProperty[][] doAnalysis(String t) {
        WordProperty[] w;

        try {
            //System.out.println("doAnalysis: text:"+t);
            w = analyzer.analyzeText(replace(hankakuToZenkaku(t)));
            toks = analyzer.getToken();
            currentSent = splitSentence(w);
        } catch (IOException e) {
            System.out.println("EJAdvisor3: IO error on doAnalysis:" + e.toString());
            System.exit(1);
        }

        return currentSent;
    }

    public String[] getRecommendations(WordProperty[] w) {
        String[] adv_len = getRecommendationsAboutLength(w);
        HashMap<String, Integer> map = getRecommendationsAboutPhrase(w);
        ArrayList<String> adv = new ArrayList<String>();

        for (Entry<String, Integer> entry : map.entrySet()) {
            String mesg = String.format("%s(%d回)\n", entry.getKey(), entry.getValue().intValue());
            adv.add(mesg);
        }

        if (adv.size() == 0) {
            return adv_len;
        } else {
            String[] ret = new String[adv_len.length + adv.size()];
            for (int i = 0; i < adv_len.length; i++) {
                ret[i] = adv_len[i];
            }
            for (int i = 0; i < adv.size(); i++) {
                ret[adv_len.length + i] = adv.get(i);
            }
            return ret;
        }
    }

    public String[] getRecommendationsAboutLength(WordProperty[] w) {
        return recommend.getRecommendationsAboutLength(w);
    }

    public HashMap<String, Integer> getRecommendationsAboutPhrase(WordProperty[] w) {
        return recommend.getRecommendationsAboutPhrase(w);
    }

    public double estimateScore(WordProperty[] w) {
        //double score = scoreEstimator.estimateScore(w);
        double score = 0.0;
        if (w.length > 0) {
            score = scoreEstimator.estimateScore(w, recommend.getRecommendationsAboutPhrase(w));
            //System.out.println(""+score);
            score = score * 100.0 / 2.0;
            if (score > 100.0) {
                score = 100.0;
            }
        }

        return score;
    }

    /**
     * 与えられた単語に近い単語を推薦する
     *
     * @param w
     */
    public EJExample[] exampleSentence(WordProperty w) {
        //String res[] = wordRecommender.getSimilarWord(w, n);
        EJExample[] res = examples.grepNJ(w.getBasicString());

        return res;
    }

    public String hankakuToZenkaku(String text) {
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if ('0' <= c && c <= '9') {
                sb.setCharAt(i, (char) (c - '0' + '０'));
            } else if ('A' <= c && c <= 'Z') {
                sb.setCharAt(i, (char) (c - 'A' + 'Ａ'));
            } else if ('a' <= c && c <= 'z') {
                sb.setCharAt(i, (char) (c - 'a' + 'ａ'));
            }
        }

        return sb.toString();
    }

    public String replace(String text) {
        for (int i = 0; i < REPLACE.length; i += 2) {
            text = text.replaceAll(REPLACE[i], REPLACE[i + 1]);
        }

        return text;
    }
    /*
    public void setPronunciation(int s, int i, String pron){
        int pos = 0;
        
        for(int j=0;j < s;j++)
            pos += currentSent[s].length;
        pos += i;
        
        System.err.printf("current:%s -> ", toks[pos].getTermInfo(), pron);
        toks[pos].setPronunciation(pron);
        System.err.printf("change:%s\n", toks[pos].getTermInfo());
    }
    */
}
