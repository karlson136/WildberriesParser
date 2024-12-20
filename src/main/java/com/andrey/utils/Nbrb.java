/*
 * Copyright
 * Andrei Razhkou
 */

package com.andrey.utils;

import com.andrey.Main;
import com.andrey.enums.Currency;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
public final class Nbrb {

    private static final String ROOT_PATH = "/";
    private static final String NBRB_SUFFIX = "_day_ru.xls";
    private static final int CURRENCY_ROW_NUMBER = 3;

    private final static Map<Currency, Map<String, Double>> currencyValuesByDate = new HashMap<>();
    private final static Map<Currency, Integer> currencyCells = new HashMap<>();

    private final static DateTimeFormatter currencyDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static void init() {
        try {
            URL nbrbRoot = Main.class.getResource(ROOT_PATH);
            URI uri = nbrbRoot.toURI();
            FilenameFilter filter = new SuffixFileFilter(NBRB_SUFFIX);

            for (File nbrbYear : new File(uri).listFiles(filter)) {
                readNbrbYear(nbrbYear);
            }
        } catch (Exception e) {
            log.error("An error occurs during nbrb init.", e);
            throw new IllegalArgumentException(e);
        }
    }

    private static void readNbrbYear(File nbrbYear) throws Exception {
        FileInputStream file = new FileInputStream(nbrbYear);
        Workbook workbook = new HSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        fillCurrencyCells(sheet.getRow(CURRENCY_ROW_NUMBER));

        StreamSupport.stream(sheet.spliterator(), false)
                .filter(Nbrb::isCurrencyValueRow).forEach(Nbrb::fillCurrencyByDate);
    }

    private static void fillCurrencyCells(Row currencyRow) {
        currencyRow.forEach(cell -> {
            for (Currency currency : Currency.values()) {
                if (currency.getDescr().equals(cell.getStringCellValue())) {
                    currencyCells.put(currency, cell.getColumnIndex() + 1);
                }
            }
        });
    }

    private static boolean isCurrencyValueRow(Row row) {
        return row.getCell(0) != null
                && row.getCell(0).getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(row.getCell(0));
    }

    private static void fillCurrencyByDate(Row row) {
        String date = currencyDateFormatter.format(row.getCell(0).getLocalDateTimeCellValue());
        for (Currency currency : Currency.values()) {
            double currencyValue = row.getCell(currencyCells.get(currency)).getNumericCellValue();
            currencyValuesByDate.computeIfAbsent(currency, (k) -> new HashMap<>()).put(date, currencyValue);
        }
    }

    public static Double getCurrencyValueByDate(Currency currency, String formattedDate) {
        return currencyValuesByDate.get(currency).get(formattedDate);
    }

}