package com.andrey;

import com.andrey.utils.Nbrb;

public class Main {
    public static void main(String[] args) {
        Nbrb.init();
        Parser parser = new Parser();
        parser.parse("Wildberries.html");
    }
}