package com.grupo6.rest.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.LocalDate;

@JsonDeserialize(builder = Result.ResultBuilder.class)
public class Result {
    private final String message;

    @JsonIgnore
    private final LocalDate currentDate = LocalDate.now();

    public Result(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ResultBuilder {
        private String message;

        public ResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public Result build() {
            return new Result(message);
        }
    }

    public static ResultBuilder builder() {
        return new ResultBuilder();
    }
}

