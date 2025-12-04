package com.grupo6.rest.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.LocalDate;

@JsonDeserialize(builder = Result.ResultBuilder.class)
public class Result {
    private final Integer unTakenTasks;
    private final Integer pendingTasks;
    private final Integer finishedTasks;
    @JsonIgnore
    private final LocalDate currentDate = LocalDate.now();
    
    public Result(Integer unTakenTasks, Integer pendingTasks, Integer finishedTasks) {
        this.unTakenTasks = unTakenTasks;
        this.pendingTasks = pendingTasks;
        this.finishedTasks = finishedTasks;
    }

    public Integer getUnTakenTasks() {
        return unTakenTasks;
    }

    public Integer getPendingTasks() {
        return pendingTasks;
    }

    public Integer getFinishedTasks() {
        return finishedTasks;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class ResultBuilder {
        private Integer unTakenTasks;
        private Integer pendingTasks;
        private Integer finishedTasks;

        public ResultBuilder unTakenTasks(Integer unTakenTasks) {
            this.unTakenTasks = unTakenTasks;
            return this;
        }

        public ResultBuilder pendingTasks(Integer pendingTasks) {
            this.pendingTasks = pendingTasks;
            return this;
        }

        public ResultBuilder finishedTasks(Integer finishedTasks) {
            this.finishedTasks = finishedTasks;
            return this;
        }

        public Result build() {
            return new Result(unTakenTasks, pendingTasks, finishedTasks);
        }
    }

    public static ResultBuilder builder() {
        return new ResultBuilder();
    }
}

