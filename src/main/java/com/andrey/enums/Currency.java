/*
 * Copyright
 * Andrei Razhkou
 */

package com.andrey.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Currency {

    RUB("Российский рубль (RUB)"),
    USD("Доллар США (USD)");

    private final String descr;

}