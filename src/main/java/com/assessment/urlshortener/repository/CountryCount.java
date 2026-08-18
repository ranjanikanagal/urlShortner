package com.assessment.urlshortener.repository;

/** Projection for ClickEventRepository#countByCountry. Must be top-level, not nested,
 *  for Hibernate's HQL "new" constructor expression to resolve it. */
public record CountryCount(String country, Long count) {
}
