/*
 * Copyright
 * Andrei Razhkou
 */

package com.andrey;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;

import static com.andrey.enums.Currency.RUB;
import static com.andrey.enums.Currency.USD;
import static com.andrey.utils.Nbrb.getCurrencyValueByDate;

@Slf4j
public class Parser {


    private final Map<String, String> months = new HashMap<>();

    private final DoubleAdder adderBy = new DoubleAdder();
    private final DoubleAdder adderRu = new DoubleAdder();
    private final DoubleAdder adderUsd = new DoubleAdder();

    {
        months.put("января", "01");
        months.put("февраля", "02");
        months.put("марта", "03");
        months.put("апреля", "04");
        months.put("мая", "05");
        months.put("июня", "06");
        months.put("июля", "07");
        months.put("августа", "08");
        months.put("сентября", "09");
        months.put("октября", "10");
        months.put("ноября", "11");
        months.put("декабря", "12");
    }

    public void parse(String input) {
        long start = System.currentTimeMillis();
        try {
            File inputFile = new File(this.getClass().getClassLoader().getResource(input).toURI());
            Document doc = Jsoup.parse(inputFile);
            Elements elements = doc.select(".archive-item__info");

            elements.forEach(element -> {
                Element price = element.selectFirst(".archive-item__price");
                Element receiveDate = element.selectFirst(".archive-item__receive-date");
                Element brand = element.selectFirst(".archive-item__brand");
                if (receiveDate != null) {
                    String formattedReceiveDate = formatReceiveDate(receiveDate.text());
                    log.info("{}; {}; {}; {}", price.text(), receiveDate.text(), formattedReceiveDate, brand.text());
                    parsePrice(price.text(), formattedReceiveDate);
                }
            });
            log.info("BY sum: {}", adderBy.sum());
            log.info("RU sum: {}", adderRu.sum());
            log.info("US sum: {}", adderUsd.sum());
        } catch (Exception e) {
            log.error("An error occurs during parse wb file.", e);
        } finally {
            log.info("Total time taken: {} ms", System.currentTimeMillis() - start);
        }
    }

    private String formatReceiveDate(String receiveDate) {
        String[] parts = receiveDate.split(" ");
        if (parts.length == 3) {
            return String.join(".", StringUtils.leftPad(parts[1], 2, '0'), months.get(parts[2]), "2024");
        }
        return String.join(".", StringUtils.leftPad(parts[1], 2, '0'), months.get(parts[2]), parts[3]);
    }

    private void parsePrice(String price, String formattedReceiveDate) {
        boolean ru = false;
        if (price.contains("₽")) {
            ru = true;
            price = price.replaceAll("₽", "");
        } else if (price.contains("р.")) {
            price = price.replaceAll("р.", "");
        } else {
            throw new IllegalArgumentException(price);
        }

        try {
            String priceStr = price.trim().replace(",", ".").replaceAll(" ", "");
            double priceDouble = Double.parseDouble(priceStr);
            if (ru && priceDouble > 150) {
                adderRu.add(priceDouble);
                adderUsd.add(((priceDouble / 100) * getCurrencyValueByDate(RUB, formattedReceiveDate)) / getCurrencyValueByDate(USD, formattedReceiveDate));
            } else {
                adderBy.add(priceDouble);
                adderUsd.add(priceDouble / getCurrencyValueByDate(USD, formattedReceiveDate));
            }
        } catch (Exception e) {
            log.error("An error occurs during parsePrice.", e);
            throw new IllegalStateException(e);
        }
    }
}