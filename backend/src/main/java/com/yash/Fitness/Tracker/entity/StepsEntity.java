package com.yash.Fitness.Tracker.entity;


import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "Steps")
@Getter
@Setter
public class StepsEntity
{
    @Id
    private ObjectId Id;

    @NonNull
    private String steps;

    private String date;

}
