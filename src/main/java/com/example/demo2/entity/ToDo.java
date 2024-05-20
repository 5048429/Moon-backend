package com.example.demo2.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "ToDoList")
public class ToDo {
    @JoinColumn(name = "day")
    private int day;
    @JoinColumn(name = "title")
    private String title;
    @JoinColumn(name = "detail")
    private String detail;
    @JoinColumn(name = "whatToDo")
    private String whatToDo;
    @Id
    private Long id;
}
