/*
 * Copyright
 * Andrei Razhkou
 */

package com.andrey;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;

public class Parser {


    private final Map<String, String> months = new HashMap<>();
    private final List<String> nbrbYears = new ArrayList<>();
    private final Map<String, Double> nbrbYearsRub = new HashMap<>();
    private final Map<String, Double> nbrbYearsUsd = new HashMap<>();

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

        nbrbYears.add("2017_day_ru.xls");
        nbrbYears.add("2018_day_ru.xls");
        nbrbYears.add("2019_day_ru.xls");
        nbrbYears.add("2020_day_ru.xls");
        nbrbYears.add("2021_day_ru.xls");
        nbrbYears.add("2022_day_ru.xls");
        nbrbYears.add("2023_day_ru.xls");
        nbrbYears.add("2024_day_ru.xls");

        readNbrbYears();
    }

    private void readNbrbYears() {
        for (String nbrbYear : nbrbYears) {
            readNbrbYear(nbrbYear);
        }
    }

    private void readNbrbYear(String nbrbYear) {
        try {
            FileInputStream file = new FileInputStream("src/main/resources/" + nbrbYear);
            Workbook workbook = new HSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            int rubCell = -1;
            int usdCell = -1;
            for (Cell cell : sheet.getRow(3)) {
                if (cell.getStringCellValue().equals("Российский рубль (RUB)")) {
                    rubCell = cell.getColumnIndex() + 1;
                }
                if (cell.getStringCellValue().equals("Доллар США (USD)")) {
                    usdCell = cell.getColumnIndex() + 1;
                }
            }

            for (Row row : sheet) {
                if (row.getCell(0) == null) {
                    break;
                }
                if (row.getCell(0).getCellType() != CellType.NUMERIC || !DateUtil.isCellDateFormatted(row.getCell(0))) {
                    continue;
                }
                String date = formatter.format(row.getCell(0).getLocalDateTimeCellValue());
                nbrbYearsRub.put(date, row.getCell(rubCell).getNumericCellValue());
                nbrbYearsUsd.put(date, row.getCell(usdCell).getNumericCellValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(nbrbYear, e);
        }
    }

    public void parse(String input) {
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
                    System.out.println(price.text() + "; " + receiveDate.text() + "; " + formattedReceiveDate + "; " + brand.text());
                    parsePrice(price.text(), formattedReceiveDate);
                }
            });
            System.out.println(adderBy.sum());
            System.out.println(adderRu.sum());
            System.out.println(adderUsd.sum());
        } catch (Exception e) {
            e.printStackTrace();
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
                adderUsd.add(((priceDouble / 100) * nbrbYearsRub.get(formattedReceiveDate)) / nbrbYearsUsd.get(formattedReceiveDate));
            } else {
                adderBy.add(priceDouble);
                adderUsd.add(priceDouble / nbrbYearsUsd.get(formattedReceiveDate));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
}