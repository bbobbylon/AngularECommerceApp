package com.bob.ecommerceangularapp.dto;

import java.util.List;

/** Summary of a bulk CSV stock import: how many rows applied, and a message per row that didn't. */
public record CsvImportResult(int updated, List<String> errors) {
}
