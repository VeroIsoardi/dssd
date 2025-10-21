package com.grupo6.rest.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;

@JsonDeserialize(builder = Result.ResultBuilder.class)
public class Result {
    private final JsonNode data;
    private final Integer total;
    private final Integer page;
    private final Integer limit;
    @JsonIgnore
    private final LocalDate currentDate = LocalDate.now();
    
    public Result(JsonNode data, Integer total, Integer page, Integer limit) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.limit = limit;
    }



    public JsonNode getData() {
        return data;
    }

    public Integer getTotal() {
        return total;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ResultBuilder {
        private JsonNode data;
        private Integer total;
        private Integer page;
        private Integer limit;

  

        public ResultBuilder data(JsonNode data) {
            this.data = data;
            return this;
        }

        public ResultBuilder total(Integer total) {
            this.total = total;
            return this;
        }

        public ResultBuilder page(Integer page) {
            this.page = page;
            return this;
        }

        public ResultBuilder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Result build() {
            return new Result(data, total, page, limit);
        }
    }

    public static ResultBuilder builder() {
        return new ResultBuilder();
    }
}

