package com.app.programacion.callbacks;

import com.app.programacion.models.Images;
import com.app.programacion.models.Recipe;

import java.util.ArrayList;
import java.util.List;

public class CallbackRecipeDetail {

    public String status = "";
    public Recipe post = null;
    public List<Images> images = new ArrayList<>();
    public List<Recipe> related = new ArrayList<>();

}