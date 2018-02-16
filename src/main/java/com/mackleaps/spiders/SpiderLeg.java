package com.mackleaps.spiders;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;


public class SpiderLeg {

    private HashMap<String, List<String>> hashLists = new HashMap<String, List<String>>();

    // We'll use a fake USER_AGENT so the web server thinks the robot is a normal web browser.
    private static final String USER_AGENT
            = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
    private List<String> substances = new LinkedList<String>();
    private Document htmlDocument;

    /**
     * This performs all the work. It makes an HTTP request, checks the
     * response, and then gathers up all the links on the page. Perform a
     * searchForWord after the successful crawl
     *
     * @param url - The URL to visit
     * @return whether or not the crawl was successful
     */
    public boolean crawl(String url) {

        Connection connection = null;
        Document htmlDocument = null;

        Proxy proxy = new Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved("172.16.0.10", 3128)
        );

        try {
            connection = Jsoup.connect(url).userAgent(USER_AGENT).proxy(proxy);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        try {
            htmlDocument = connection.get();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        this.htmlDocument = htmlDocument;
        if (connection.response().statusCode() == 200) // 200 is the HTTP OK status code
        // indicating that everything is great.
        {
            System.out.println("\n**Visiting** Received web page at " + url);
        }
        if (!connection.response().contentType().contains("text/html")) {
            System.out.println("**Failure** Retrieved something other than HTML");
            return false;
        }


        //Extract the <td> tags in the table of substances
        Elements tdsOnPage = htmlDocument.select("td");


        /* The <td> we want to extract starts with index 6
         * and iterate through the table by 3 for the
         * reason of having 3 columns
         */
        final int firstSubstanceTD = 6;
        for (int i = tdsOnPage.size()-1; i >= firstSubstanceTD; i -= 3){

            Element td = tdsOnPage.get(i);
            System.out.println("TD inteiro: " + td);

            Elements allParagraphs = td.select("p");

            String action = "";

            System.out.println("== == TD "+(i-3)/3+" == =="); //Show <td> index
            for(Element p : allParagraphs){

                if(isAction(p)){
                    action = p.select("strong").first().text().toLowerCase();
                    System.out.println("Ação da Anvisa: " + action);
                }else{
                    try{
                        //Add substances on the list
                        if (action.trim().equals("inclusão")){
                            includeSubstances(extractSubstances(p,action));
                        }
                        //Remove substances of the list
                        if (action.trim().equals("exclusão")){
                            excludeSubstances(extractSubstances(p,action));
                        }
                        //Swap substances of the lists
                        if (action.trim().equals("transferência")){
                            transferSubstances(extractSubstances(p,action));
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }

            System.out.println("");
            System.out.println(" === ");
            System.out.println("");
        }

        //Loop hashmap
        Set<String> chaves = hashLists.keySet();
        for (String chave : chaves){
            System.out.println("========= hashmap =======");
            System.out.println(chave + hashLists.get(chave));
        }


        return true;
    }


    public boolean isAction(Element e){
        return e.select("strong").hasText();
    }

    /**
     * This extract the name of the list and the substances
     * of a paragraph.
     * This only works if it starts with "Lista", "Adendo" is not included.
     *
     * @param p - the paragraph to be splitted
     * @param action - action to be executed
     * @return the name of the list and all substances
     */

    public List<String> extractSubstances(Element p, String action){
        String []splitParagraph = null;
        String []firstSplit = null;
        String []secondSplit = null;

        String nameOfList = "";

        List<String> substances = new ArrayList<String>();

        splitParagraph = p.text().split(" ");
        if(splitParagraph[0].equals("Lista")){
            splitParagraph = p.text().split(":");

            firstSplit = splitParagraph[0].split(" "); //Name of list that substances will be added
            secondSplit = splitParagraph[1].split(";");//The substances extracted

            if (firstSplit[0].equals("Lista")){
                System.out.println("Ação na lista " + firstSplit[1]);

                if (firstSplit[1].length() == 2){
                    nameOfList = firstSplit[1].trim();
                }else{
                    nameOfList = firstSplit[1].substring(1,3);
                }
                substances.add(nameOfList);

                //The list that will receive new substance
                if(action.trim().equals("transferência")){
                    nameOfList = firstSplit[5].substring(1,3);
                    substances.add(nameOfList);
                }

                for (int i = 0; i < secondSplit.length; i++ ){
                    System.out.println(secondSplit[i]);
                    substances.add(secondSplit[i].replace(".", "").trim());
                }
            }
        }
        return substances;
    }

    /**
     * Include all substances in the HashMap
     *
     * @param substances - Substances to be added
     */
    public void includeSubstances(List<String> substances){
        if(!substances.isEmpty()){
            String nameOfList = substances.remove(0);
            System.out.println("\nADICIONANDO NA LISTA "+nameOfList+" ...");

            System.out.println(substances);

            if (hashLists.containsKey(nameOfList)) {
                for (String substance : substances){
                    //Avoid repetition
                    if (!hashLists.get(nameOfList).contains(substance))
                        hashLists.get(nameOfList).add(substance);
                }
            }else{
                hashLists.put(nameOfList,substances);
            }
            System.out.println("");
        }
    }


    /**
     * Remove all substances of the HashMap
     *
     * @param substances - Substances to be removed
     */
    public void excludeSubstances(List<String> substances){
        if(!substances.isEmpty()){
            String nameOfList = substances.remove(0);
            System.out.printf("\nREMOVENDO DA LISTA "+nameOfList+" ...");

            for (String substance : substances){
                System.out.println(substance);
            }

            if (hashLists.containsKey(nameOfList)) {
                hashLists.get(nameOfList).removeAll(substances);
            }

            System.out.println("");
        }
    }

    private void transferSubstances(List<String> substances) {
        if(!substances.isEmpty()){
            System.out.println(" ==== HORT === ");
            System.out.println(substances);
        }
    }

}
