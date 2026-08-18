package com.assessment.urlshortener.repository;

import com.assessment.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    @Query("select count(distinct c.visitorHash) from ClickEvent c where c.shortCode = :shortCode")
    long countDistinctVisitors(@Param("shortCode") String shortCode);

    @Query("""
            select new com.assessment.urlshortener.repository.CountryCount(
                coalesce(c.country, 'Unknown'), count(c))
            from ClickEvent c
            where c.shortCode = :shortCode
            group by c.country
            """)
    List<CountryCount> countByCountry(@Param("shortCode") String shortCode);
}
