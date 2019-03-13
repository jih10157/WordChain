package com.jih10157.WordChain;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class DBCrawling {
    private static Pattern pattern = Pattern.compile("^([��-�R])[��-�R]+$");
    private final short THREADAMOUNT = 50;
    private final int MAXIDX = 519028;// 519208;

    public static void main(String[] args) {
        Set<String> words = new DBCrawling().load();
        for (String string:words) {
            System.out.println(string);
        }
    }
    public Set<String> load() {
        File file = new File("Data", "db.txt");
        Set<String> words;
        if(!file.exists()) {
            System.out.println("��������� ǥ�ر������� ���� �ܾ� ũ�Ѹ��� �����մϴ�.");
            long mills = System.currentTimeMillis();
            try {
                words = start();
                System.gc();
            } catch (ExecutionException | InterruptedException | IOException e) {
                e.printStackTrace();
                System.out.println("ũ�Ѹ��� ������ �߻��Ͽ� ���α׷��� �����մϴ�.");
                System.exit(1);
                return null;
            }
            System.out.println("ũ�Ѹ��� �Ϸ�Ǿ����ϴ�. �ɸ��ð�: "+(System.currentTimeMillis()-mills)+"ms");
        } else {
            try {
                words = new HashSet<>(Files.readAllLines(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("������ �о������ ������ �߻��Ͽ� ���α׷��� �����մϴ�.");
                System.exit(1);
                return null;
            }
            System.out.println("���������� ������ �о�Խ��ϴ�.");
        }
        System.out.println("��ϵ� �ܾ� ����: "+words.size());
        return words;
    }
    //������ �ܾ� http://stdweb2.korean.go.kr/search/View.jsp?idx=519208
    private Set<String> start() throws ExecutionException, InterruptedException, IOException {
        File file = new File("Data", "db.txt");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) file.createNewFile();
        Path path = file.toPath();
        ExecutorService executor = Executors.newFixedThreadPool(THREADAMOUNT);
        List<Future<List<String>>> futures = new ArrayList<>(THREADAMOUNT);
        int size = (int)Math.ceil((double)MAXIDX/(double)THREADAMOUNT);
        System.out.println("SIze: "+size);
        for (int i=1;i<=THREADAMOUNT;i++) {
            futures.add(executor.submit(new Getter(size, i*size)));
            Thread.sleep(10);
        }
        System.out.println("������ submit");
        Set<String> list = new LinkedHashSet<>(MAXIDX);
        for(Future<List<String>> future:futures) {
            list.addAll(future.get());
        }
        System.out.println(list.size()+"�� ��� �ҷ���");
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        for(String str:list) sb.append(str).append(nl);
        Files.write(path, sb.toString().getBytes());
        System.out.println("���� ���� �Ϸ�");
        executor.shutdown();
        return new HashSet<>(list);
    }
    private class Getter implements Callable<List<String>> {
        private int size;
        private int max;
        private int min;
        private CloseableHttpClient client;
        private Getter(int i, int i2) {
            size = i;
            max = i2;
            min = max-size;
            client = HttpClients.createDefault();
        }
        @Override
        public List<String> call() throws Exception {
            List<String> list = new ArrayList<>();
            Document doc;
            for (int i=1;i<=size;i++) {
                int idx = min+i;
                doc = getDoc(client, "http://stdweb2.korean.go.kr/search/View.jsp?idx="+idx);
                String str;
                if(doc.select(".NumRG").text().replace("��", "").replace("��", "").equalsIgnoreCase("����")&&!(str=doc.select("#print_area > div > font").text()).contains("����")&&!str.contains("���")&&!str.contains("����")&&pattern.matcher((str=doc.select(".word_title").text().replace("-", "").replace(" ", "").replace("��", "").replace("^", "").replaceAll("\\d", ""))).matches()) {
                    list.add(str);
                }
            }
            client.close();
            return list;
        }
    }
    //#print_area > div > font
        //File file = new File("Data", "db.txt");
        //if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        //if (!file.exists()) file.createNewFile();
        //long millis = System.currentTimeMillis();
        //http://stdweb2.korean.go.kr/search/List_dic.jsp?idx=&go=10&gogroup=&PageRow=35120&ImeMode=&setJaso=&JasoCnt=0&SearchPart=SP&ResultRows=351201&SearchStartText=&SearchEndText=&JasoSearch=&arrSearchLen=0&Table=words%7Cword&Gubun=0&OrgLang=&TechTerm=&SearchText=&SpCode=9&SpCode=7&SpCode=2&SpCode=1&SpCode=8&SpCode=3
        //http://stdweb2.korean.go.kr/search/List_dic.jsp?seq=&PageRow=10&Table=words%7Cword&Gubun=0&SearchPart=Simple&SearchText=%EC%9D%B8%EC%82%AC
        //�˻���: �λ�
        //http://stdweb2.korean.go.kr/search/List_dic.jsp?idx=&go=&gogroup=1&PageRow=351201&ImeMode=&setJaso=&JasoCnt=0&SearchPart=SP&ResultRows=351201&SearchStartText=&SearchEndText=&JasoSearch=&arrSearchLen=0&Table=words%7Cword&Gubun=0&OrgLang=&TechTerm=&SearchText=&SpCode=9&SpCode=7&SpCode=2&SpCode=1&SpCode=8&SpCode=3
        //��� ��������
        //351201
        //����
        //String maxsizestr = Jsoup.connect("http://stdweb2.korean.go.kr/search/List_dic.jsp?idx=&go=&gogroup=&PageRow=1&ImeMode=&arrSearchLen=0&setJaso=&JasoCnt=&SearchPart=SP&ResultRows=0&Table=words%7Cword&OrgLang=&TechTerm=&Gubun=0&SearchText=&focus_name=SearchText&SpCode=1&SearchStartText=&SearchEndText=&Jaso1=&Jaso2=&Jaso3=&Jaso1=&Jaso2=&Jaso3=&Jaso1=&Jaso2=&Jaso3=&Jaso1=&Jaso2=&Jaso3=&JasoSearch=")
        //        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36")
        //        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        //        .header("Accept-Encoding", "gzip, deflate")
        //        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
        //        .timeout(0).get().select(".tb12").text().replace("�� ���� �˻� ����Դϴ�.(", "").replace("��)", "");
        //final double maxsize = Double.valueOf(maxsizestr);
        //System.out.println("�ܾ� ����: " + maxsize);
        //final short threadcount = 9;
        //final short threadinitcount = 3;
        //100 = 118302
        //50 = 59323
        //final int size = (int) Math.ceil(maxsize / (double) threadcount);
        //final int for2 = threadcount/threadinitcount;
        //Crawler[] crawlers = new Crawler[threadinitcount];
        //for(short i2 = 1; i2 <= for2; i2++) {
        //    for (short i = 1; i <= threadinitcount; i++) { crawlers[i - 1] = new Crawler(i, size); }
        //    for (Crawler crawler : crawlers) { crawler.start(); }
        //    for (Crawler crawler : crawlers) { crawler.join(); }
        //    }
        //    System.out.println("�ɸ��ð�: " + (System.currentTimeMillis() - millis) + "ms");
        //    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        //    for (String word : wordSet) {
        //    writer.write(word + "\n");
        //    }
        //    writer.flush();
        //    System.out.println("���� �Ϸ� �� �޾ƿ°���: " + wordSet.size());
        //    writer.close();
    //}
    /*private class Crawler extends Thread {
        private short page;
        private int size;

        private Crawler(short page, int size) {
            this.page = page;
            this.size = size;
        }

        @Override
        public void run() {
            try {
                System.out.println(this.page + "�� ������ ���� size: " + this.size);
                //Connection.Response response = Jsoup.connect("http://stdweb2.korean.go.kr/search/List_dic.jsp?idx=&go=" + this.page + "&gogroup=&PageRow=" + this.size + "&ImeMode=&setJaso=&JasoCnt=0&SearchPart=SP&ResultRows=351201&SearchStartText=&SearchEndText=&JasoSearch=&arrSearchLen=0&Table=words%7Cword&Gubun=0&OrgLang=&TechTerm=&SearchText=&SpCode=9&SpCode=7&SpCode=2&SpCode=1&SpCode=8&SpCode=3")
                //        .method(Connection.Method.GET)
                //        .timeout(0).execute();
                Document doc = getDoc("http://stdweb2.korean.go.kr/search/List_dic.jsp?idx=&go=" + this.page + "&gogroup=&PageRow=" + this.size + "&ImeMode=&setJaso=&JasoCnt=0&SearchPart=SP&SearchStartText=&SearchEndText=&JasoSearch=&arrSearchLen=0&Table=words%7Cword&Gubun=0&OrgLang=&TechTerm=&SearchText=&SpCode=9&SpCode=7&SpCode=2&SpCode=1&SpCode=8&SpCode=3");
                System.out.println(this.page + "�� ������ " + doc.select(".page_on").text() + "������");
                Elements elements = doc.select("p.exp a");
                Set<String> wordList = new HashSet<>(this.size);
                String word;
                for (Element element : elements) {
                    if (pattern.matcher(word = element.text().trim().replace("-", "").replace(" ", "").replace("��", "").replace("^", "").replace("</", "")).matches()) {
                        wordList.add(word);
                    }
                }
                System.out.println(this.page + "�� ������ ��� ������ �ܾ��: " + wordList.size());
                wordSet.addAll(wordList);
                System.out.println(this.page + "" + "�� ������ ����");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    private static Document getDoc(CloseableHttpClient client, String url) throws IOException {
        return Jsoup.parse(getHtml(client, url));
    }
    private static String getHtml(CloseableHttpClient client, String url) throws IOException {
        /*AsyncHttpClient asyncHttpClient = asyncHttpClient(config().setReadTimeout(-1).setRequestTimeout(-1).setConnectTimeout(5000));
        Future<Response> f = asyncHttpClient.prepareGet(url).execute();
        Response r = f.get();
        asyncHttpClient.close();
        return r.getResponseBody(StandardCharsets.UTF_8);*/
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(RequestConfig.custom().setConnectTimeout(-1).setSocketTimeout(-1).setConnectionRequestTimeout(-1).build());
        //httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        //httpGet.addHeader("Accept-Encoding", "gzip, deflate");
        //httpGet.addHeader("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        //httpGet.addHeader("Cache-Control","no-store");
        //httpGet.addHeader("Host", "stdweb2.korean.go.kr");
        //httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        String str;
        try (CloseableHttpResponse response = client.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            str = EntityUtils.toString(entity);
        } catch (HttpHostConnectException e) {
            return getHtml(client, url);
        }
        return str;
    }
}