package com.example.demo2.mapper;

import com.example.demo2.entity.ToDo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TodoMapper {
    @Select("select * from ToDoList where day=#{day}")
    public List<ToDo> GetWhatToDoByDay(@Param("day") int day);

    @Insert("insert into TestList values (#{date}, #{WhatToDo})")
    public void SetTest(@Param("date") int date,@Param("WhatToDo") String WhatToDo);
}
