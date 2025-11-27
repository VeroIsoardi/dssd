package com.grupo6.rest.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.LocalDate;

@JsonDeserialize(builder = Result.ResultBuilder.class)
public class Result {
    private final Integer total;
    @JsonIgnore
    private final LocalDate currentDate = LocalDate.now();
    
    public Result(Integer total) {
        this.total = total;
    }

    public Integer getTotal() {
        return total;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ResultBuilder {
        private Integer total;

        public ResultBuilder total(Integer total) {
            this.total = total;
            return this;
        }

        public Result build() {
            return new Result(total);
        }
    }

    public static ResultBuilder builder() {
        return new ResultBuilder();
    }
}

