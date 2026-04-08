package com.yash.Fitness.Tracker.entity;


import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "Users")
@Getter
@Setter
public class UserEntity
{
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NonNull
    private String userName;

    @NonNull
    private String password;

    @NonNull
    private String email;

    private String imageUrl = "https://res.cloudinary.com/dgvkjcaqg/image/upload/v1740300887/samples/ecommerce/shoes.png";

    @DBRef
    private List<StepsEntity> stepsEntity = new ArrayList<>();
}
